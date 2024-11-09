package com.my.customcamera

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.Bundle
import android.os.Environment
import android.view.View
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.GridLayoutManager
import com.my.customcamera.adapters.FileAdapter
import com.my.customcamera.databinding.ActivityFilesBinding
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.io.File


class FilesActivity : AppCompatActivity() {

    private lateinit var binding: ActivityFilesBinding
    private lateinit var bitMapsList: MutableList<Bitmap>
    private lateinit var fileAdapter: FileAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityFilesBinding.inflate(layoutInflater)
        enableEdgeToEdge()
        setContentView(binding.root)

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

        bitMapsList = mutableListOf()

        CoroutineScope(Dispatchers.Main).launch {
            loadImages()
            delay(5000)
            binding.recyclerView.layoutManager = GridLayoutManager(this@FilesActivity, 2)
            fileAdapter = FileAdapter(this@FilesActivity, bitMapsList)
            binding.recyclerView.adapter = fileAdapter
        }

    }

    private fun loadImages() {
        binding.progressBar.visibility = View.VISIBLE

        val file = File(
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DCIM),
            "/custom/"
        )
        if (file.exists() && file.isDirectory) {
            val images = file.listFiles { _, name ->
                name.endsWith(".jpg") || name.endsWith(".png") || name.endsWith(".jpeg")
            }

            if (images != null && images.isNotEmpty()) {
                for (image in images) {
                    val bitmap = BitmapFactory.decodeFile(image.absolutePath)
                    bitMapsList.add(bitmap)
                }
                binding.recyclerView.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            } else {
                showError()
            }
        } else {
            showError()
        }
    }

    private fun showError() {
        binding.progressBar.visibility = View.GONE
        binding.errorText.visibility = View.VISIBLE
    }
}
