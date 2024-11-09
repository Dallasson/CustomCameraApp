package com.my.customcamera.fragments

import android.annotation.SuppressLint
import android.hardware.Camera
import android.media.MediaRecorder
import android.os.Build
import android.os.Bundle
import android.os.CountDownTimer
import android.os.Environment
import android.util.Log
import android.view.LayoutInflater
import android.view.SurfaceHolder
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.my.customcamera.databinding.RecordLayoutBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File

class RecordFragment : Fragment() {

    private var elapsedTime: Long = 0L
    private var countDownTimer: CountDownTimer? = null
    private var isRecording = false
    private var cameraID = Camera.CameraInfo.CAMERA_FACING_BACK
    private lateinit var surfaceHolder: SurfaceHolder
    private var camera: Camera? = null
    private var mediaRecorder: MediaRecorder? = null
    private lateinit var binding : RecordLayoutBinding
    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = RecordLayoutBinding.inflate(inflater,container,false)
        return binding.root
    }


    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)


        launchCamera()

        binding.recordVid.setOnClickListener {
            if(camera != null){
               if(isRecording){
                   stopRecording()
                   stopCounter()
               } else {
                   recordVideo()
                   startCounter()
               }
            }
        }

        binding.switchCamera.setOnClickListener {
            switchCamera()
        }

    }

    private fun startCounter() {
        isRecording = true
        countDownTimer = object : CountDownTimer(Long.MAX_VALUE, 1000L) {
            override fun onTick(millisUntilFinished: Long) {
                if (isRecording){
                    elapsedTime++
                    val formattedTime = formatTime(elapsedTime)
                    updateTimeDisplay(formattedTime)
                }
            }

            override fun onFinish() {
                // Handle timer finish if needed
            }
        }.start()
    }

    private fun stopCounter(){
        isRecording = false
        countDownTimer!!.cancel()
        countDownTimer = null
        binding.countTxt.text = "00:00"
    }

    private fun updateTimeDisplay(formattedTime: String) {
        binding.countTxt.text = formattedTime
    }

    @SuppressLint("DefaultLocale")
    private fun formatTime(seconds: Long): String {
        val minutes = (seconds / 60).toInt()
        val remainingSeconds = (seconds % 60).toInt()
        return String.format("%02d:%02d", minutes, remainingSeconds)
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

    private fun startCamera(){
        camera = Camera.open(cameraID)
        try {
            camera!!.setDisplayOrientation(90)
            camera!!.setPreviewDisplay(surfaceHolder)
            camera!!.startPreview()
        }  catch (ex : Exception){
            Log.d("TAG","Failed to open camera " + ex.message)
        }
    }

    private fun resetCamera(){

        if(surfaceHolder.surface == null){
            return
        }

        if(camera != null){
            try {
                camera!!.setDisplayOrientation(90)
                camera!!.stopPreview()
                camera!!.setPreviewDisplay(surfaceHolder)
            } catch (ex : Exception){
                Log.d("TAG","Failed to open camera " + ex.message)
            }
        }

    }

    private fun recordVideo(){

        val videoID = System.currentTimeMillis().toString()


        val directory = File(
            Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DCIM), "custom/videos/")


        if (!directory.exists()) {
            directory.mkdirs()
        }

        val file = File(directory, "$videoID.mp4")

        mediaRecorder  = MediaRecorder().apply {
            setCamera(camera)
            setAudioSource(MediaRecorder.AudioSource.CAMCORDER)
            setVideoSource(MediaRecorder.VideoSource.CAMERA)
            setOutputFormat(MediaRecorder.OutputFormat.MPEG_4)
            setAudioEncoder(MediaRecorder.AudioEncoder.AAC)
            setVideoEncoder(MediaRecorder.VideoEncoder.H264)
            setOutputFile(file.absolutePath)
            setVideoFrameRate(30)
            setVideoSize(1280, 720)
            setVideoEncodingBitRate(10000000)
            setOrientationHint(90)
            setPreviewDisplay(surfaceHolder.surface)
        }

        try {
            mediaRecorder!!.prepare()
            mediaRecorder!!.start()
        } catch (e: Exception) {
            Log.e("VideoCapture", "MediaRecorder preparation failed: ${e.message}")
            releaseMediaRecorder()

        }
    }

    private fun stopRecording() {
        releaseCamera()
        startCamera()
        releaseMediaRecorder()
    }

    private fun releaseMediaRecorder() {
        mediaRecorder?.release()
        mediaRecorder = null
    }

    override fun onDestroy() {
        stopRecording()
        stopCounter()
        releaseCamera()
        super.onDestroy()
    }
}