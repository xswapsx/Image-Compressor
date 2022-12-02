package com.swapy.imagecompressorex

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.swapy.imagecompressor.ImageCompressor

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        var imagePath =
            "/storage/emulated/0/Android/media/com.asdf.mycamerapp/20221202121650702.jpeg"
        val compressedImagePath = ImageCompressor.compressImage(imagePath)

        Log.d("MainActivity", "onCreate: saved at: $compressedImagePath")
    }
}