package com.my.customcamera.fragments

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.my.customcamera.FilesActivity
import com.my.customcamera.databinding.CaptureLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream

class CaptureFragment : Fragment() {

    private var cameraID = Camera.CameraInfo.CAMERA_FACING_BACK
    private lateinit var surfaceHolder: SurfaceHolder
    private var camera: android.hardware.Camera? = null
    companion object {
        const val CAMERA_PERMISSION_CODE = 1001
        const val STORAGE_PERMISSION_CODE = 1002
        const val STORAGE_UP_PERMISSION_CODE = 1003
    }
    private lateinit var binding : CaptureLayoutBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = CaptureLayoutBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        if (isCameraPermissionGranted()){
            launchCamera()
        } else {
            requestCameraPermission()
        }


        binding.captureImage.setOnClickListener {
            if(camera != null){
                captureImage()
            }
        }

        binding.switchCamera.setOnClickListener {
            switchCamera()
        }
    }

    private fun switchCamera(){
        if (camera != null){
            camera!!.stopPreview()
            camera!!.release()


            cameraID = if(cameraID == Camera.CameraInfo.CAMERA_FACING_BACK){
                Camera.CameraInfo.CAMERA_FACING_FRONT
            } else {
                Camera.CameraInfo.CAMERA_FACING_BACK
            }

            camera = Camera.open(cameraID)
            try {
                camera!!.setDisplayOrientation(90)
                camera!!.setPreviewDisplay(surfaceHolder)
                camera!!.startPreview()
            } catch (ex : Exception){
                Log.d("TAG","Failed to open camera " + ex.message)
            }
        }
    }

    private fun captureImage() {
        camera!!.takePicture(null, null, Camera.PictureCallback { data, _ ->
            val bitmap = BitmapFactory.decodeByteArray(data, 0, data.size)

            // Flip horizontally for front camera
            val matrix = Matrix().apply {
                preScale(-1.0f, -1.0f)
            }
            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)

            requireActivity().runOnUiThread {
                binding.imagePreview.setImageBitmap(bitmap)

                val imageID = System.currentTimeMillis().toString()
                // Create the directory path for saving the image
                val directory = File(
                    Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_DCIM), "custom")

                // Ensure the directory exists
                if (!directory.exists()) {
                    directory.mkdirs() // Create all missing parent directories
                }

                val file = File(directory, "$imageID.jpg")

                try {
                    FileOutputStream(file).use { outputStream ->
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream)
                    }

                    releaseCamera()
                    startCamera()

                    Log.d("TAG", "Image saved at: ${file.absolutePath}")
                } catch (ex: Exception) {
                    ex.printStackTrace()
                    Log.d("TAG", "Failed to save image: ${ex.message}")
                }
            }
        })
    }

    private fun launchCamera(){

        surfaceHolder = binding.surfaceView.holder
        surfaceHolder.addCallback(object : SurfaceHolder.Callback {
            override fun surfaceCreated(holder: SurfaceHolder) {
                CoroutineScope(Dispatchers.IO).launch {
                    startCamera()
                }
            }

            override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
                resetCamera()
            }

            override fun surfaceDestroyed(holder: SurfaceHolder) {
                releaseCamera()
            }

        })

    }

    private fun releaseCamera() {
        if (camera != null) {
            camera!!.stopPreview()
            camera!!.release()
            camera = null
        }
    }

    private fun startCamera() {
        camera = Camera.open(cameraID)
        try {
            camera!!.setDisplayOrientation(90) // Adjust based on the device's orientation
            camera!!.setPreviewDisplay(surfaceHolder)
            camera!!.startPreview()
        } catch (ex: Exception) {
            Log.d("TAG", "Failed to open camera: ${ex.message}")
        }
    }

    private fun resetCamera(){

        if(surfaceHolder.surface == null){
            return
        }

        if(camera != null){
            try {
                camera!!.stopPreview()
                camera!!.setPreviewDisplay(surfaceHolder)
            } catch (ex : Exception){
                Log.d("TAG","Failed to open camera " + ex.message)
            }
        }

    }

    private fun isCameraPermissionGranted() : Boolean {
        return (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA)
                == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                    requireContext(),Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED)
    }

    private fun requestCameraPermission(){
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.CAMERA,
            Manifest.permission.RECORD_AUDIO),
            CAMERA_PERMISSION_CODE)
    }

    private fun requestLegacyStorage(){
        ActivityCompat.requestPermissions(requireActivity(), arrayOf(
            Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE
        ), STORAGE_PERMISSION_CODE)
    }

    private fun requestStorage(){
        val intent = Intent()
        intent.setAction(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION)
        val uri = Uri.fromParts("package", requireContext().packageName, null)
        intent.setData(uri)
        startActivityForResult(intent, STORAGE_UP_PERMISSION_CODE)
    }

    private fun isStoragePermissionGranted() : Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
            Environment.isExternalStorageManager()
        } else {
            (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED && ContextCompat.checkSelfPermission(
                requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE
            ) == PackageManager.PERMISSION_GRANTED)
        }
    }

    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        if(requestCode == CAMERA_PERMISSION_CODE){
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S){
                requestStorage()
            } else {
                requestLegacyStorage()
            }
        } else {
            Toast.makeText(requireContext(),"Please allow camera", Toast.LENGTH_LONG).show()
        }

        if(requestCode == STORAGE_PERMISSION_CODE){
            launchCamera()
        } else {
            Toast.makeText(requireContext(),"Please allow storage", Toast.LENGTH_LONG).show()
        }

    }

    @Deprecated("Deprecated in Java")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if(requestCode == STORAGE_UP_PERMISSION_CODE){
            launchCamera()
        } else {
            Toast.makeText(requireContext(),"Please allow storage", Toast.LENGTH_LONG).show()
        }
    }


}