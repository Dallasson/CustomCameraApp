package com.my.customcamera

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BitmapFactory
import android.hardware.Camera
import android.media.MediaRecorder
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Environment
import android.provider.Settings
import android.util.Log
import android.view.SurfaceHolder
import android.widget.TableLayout
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.graphics.translationMatrix
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import com.google.android.material.tabs.TabLayout
import com.my.customcamera.databinding.ActivityMainBinding
import com.my.customcamera.fragments.CaptureFragment
import com.my.customcamera.fragments.RecordFragment
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream


class MainActivity : AppCompatActivity() {

    private lateinit var binding : ActivityMainBinding
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }

         setTabs()
         loadFragment(CaptureFragment())

        binding.checkImages.setOnClickListener {
            Intent(this, FilesActivity::class.java).apply {
                startActivity(this)
            }
        }


    }

    private fun setTabs(){
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Capture"))
        binding.tabLayout.addTab(binding.tabLayout.newTab().setText("Record"))

        binding.tabLayout.addOnTabSelectedListener(object : TabLayout.OnTabSelectedListener {
            override fun onTabSelected(tab: TabLayout.Tab?) {
                if(tab!!.position == 0){
                    loadFragment(CaptureFragment())
                } else {
                    loadFragment(RecordFragment())
                }
            }

            override fun onTabUnselected(tab: TabLayout.Tab?) {

            }

            override fun onTabReselected(tab: TabLayout.Tab?) {

            }

        })
    }

    private fun loadFragment(fragment : Fragment){
        supportFragmentManager.beginTransaction().replace(binding.containerView.id,fragment).commit()
    }

}