package com.swapy.imagecompressorex

import android.annotation.SuppressLint
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Matrix
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageCapture
import androidx.camera.core.ImageCaptureException
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.lifecycle.lifecycleScope
import com.google.android.material.snackbar.Snackbar
import com.google.common.util.concurrent.ListenableFuture
import com.swapy.imagecompressor.ImageCompressor
import com.swapy.imagecompressorex.databinding.ActivityMainBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {
    private lateinit var binding: ActivityMainBinding
    private lateinit var cameraProviderFuture: ListenableFuture<ProcessCameraProvider>
    private lateinit var cameraSelector: CameraSelector
    private var isFrontCamera = false
    private var imageCapture: ImageCapture? = null
    private lateinit var imgCaptureExecutor: ExecutorService
    private val cameraPermissionResult =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { permissionGranted ->
            if (permissionGranted) {
                startCamera()
            } else {
                Snackbar.make(
                    binding.root,
                    "The camera permission is necessary",
                    Snackbar.LENGTH_INDEFINITE
                ).show()
            }
        }

    @SuppressLint("ObsoleteSdkInt")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        cameraProviderFuture = ProcessCameraProvider.getInstance(this)
        cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA

        imgCaptureExecutor = Executors.newSingleThreadExecutor()

        cameraPermissionResult.launch(android.Manifest.permission.CAMERA)

        binding.imgCaptureBtn.setOnClickListener {
            takePhoto()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                animateFlash()
            }
        }

        binding.switchBtn.setOnClickListener {
            cameraSelector = if (cameraSelector == CameraSelector.DEFAULT_BACK_CAMERA) {
                isFrontCamera = true
                CameraSelector.DEFAULT_FRONT_CAMERA
            } else {
                isFrontCamera = false
                CameraSelector.DEFAULT_BACK_CAMERA
            }
            startCamera()
        }
        binding.galleryBtn.setOnClickListener {
            val intent = Intent(this, GalleryActivity::class.java)
            startActivity(intent)
        }

    }

    private fun startCamera() {
        val preview = Preview.Builder().build().also {
            it.surfaceProvider = binding.preview.surfaceProvider
        }
        cameraProviderFuture.addListener({
            val cameraProvider = cameraProviderFuture.get()

            imageCapture = ImageCapture.Builder().build()

            try {
                cameraProvider.unbindAll()
                cameraProvider.bindToLifecycle(this, cameraSelector, preview, imageCapture)
            } catch (e: Exception) {
                Timber.tag(TAG).d("Use case binding failed")
            }
        }, ContextCompat.getMainExecutor(this))
    }

    private fun takePhoto() {
        imageCapture?.let {
            val fileName = SimpleDateFormat(FILENAME_FORMAT, Locale.US)
                .format(System.currentTimeMillis()) + ".jpeg"
            val file = File(externalMediaDirs[0], fileName)
            val outputFileOptions = ImageCapture.OutputFileOptions.Builder(file).build()
            it.takePicture(
                outputFileOptions,
                imgCaptureExecutor,
                object : ImageCapture.OnImageSavedCallback {
                    override fun onImageSaved(outputFileResults: ImageCapture.OutputFileResults) {
                        Timber.tag(TAG).i("The image has been saved in ${file.absolutePath}")

                        lifecycleScope.launch(Dispatchers.IO) {
                            try {
                                // Decode bitmap from saved file
                                val originalBitmap = BitmapFactory.decodeFile(file.absolutePath)

                                // Flip if using front camera
                                val finalBitmap = if (isFrontCamera) {
                                    flipBitmapHorizontally(originalBitmap)
                                } else {
                                    originalBitmap
                                }

                                // Save flipped bitmap back to file
                                FileOutputStream(file).use { out ->
                                    finalBitmap.compress(Bitmap.CompressFormat.JPEG, 100, out)
                                }

                                // Now compress using your compressor
                                val compressedPath = ImageCompressor.compressImage(
                                    file.absolutePath
                                )

                                withContext(Dispatchers.Main) {
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Compressed image saved at $compressedPath",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                    Timber.tag(TAG).i("Compressed image path: $compressedPath")
                                }
                            } catch (e: Exception) {
                                Timber.e(e, "Failed to process and compress image")
                            }
                        }
                    }


                    override fun onError(exception: ImageCaptureException) {
                        Toast.makeText(
                            binding.root.context,
                            "Error taking photo",
                            Toast.LENGTH_LONG
                        ).show()
                        Timber.tag(TAG).d("Error taking photo:$exception")
                    }

                })
        }
    }

    private fun animateFlash() {
        binding.root.postDelayed({
            binding.root.foreground = Color.WHITE.toDrawable()
            binding.root.postDelayed({
                binding.root.foreground = null
            }, 50)
        }, 100)
    }

    private fun flipBitmapHorizontally(bitmap: Bitmap): Bitmap {
        val matrix = Matrix().apply {
            preScale(-1f, 1f)
        }
        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
    }

    override fun onDestroy() {
        super.onDestroy()
        imgCaptureExecutor.shutdown()
    }

    companion object {
        const val TAG = "MainActivity"
        private const val FILENAME_FORMAT = "yyyy-MM-dd-HH-mm-ss-SSS"
    }
}