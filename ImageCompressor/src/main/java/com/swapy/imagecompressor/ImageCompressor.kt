package com.swapy.imagecompressor

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import androidx.core.graphics.scale

object ImageCompressor {
    private const val MAX_WIDTH = 812
    private const val MAX_HEIGHT = 1016

    fun compressImage(context: Context, imagePath: String): String {
        val originalFile = File(imagePath)
        if (!originalFile.exists()) return imagePath

        // Decode bounds only
        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imagePath, options)

        val originalWidth = options.outWidth
        val originalHeight = options.outHeight



        // Calculate the new dimensions
        val (newWidth, newHeight) = calculateScaledDimensions(originalWidth, originalHeight)

        // Load scaled bitmap
        options.inJustDecodeBounds = false
        options.inSampleSize = calculateInSampleSize(options, newWidth, newHeight)
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val bitmap = BitmapFactory.decodeFile(imagePath, options)

        // Rotate based on EXIF orientation
        val rotatedBitmap = applyExifRotation(imagePath, bitmap)

        // Resize to scaled dimensions
        val scaledBitmap = rotatedBitmap.scale(newWidth, newHeight)

        // Save to compressed file
        val compressedFile = File(context.cacheDir, "compressed_${System.currentTimeMillis()}.jpg")
        FileOutputStream(compressedFile).use { out ->
            scaledBitmap.compress(Bitmap.CompressFormat.JPEG, 70, out) // 70 quality targets < 100KB
        }

        return compressedFile.absolutePath
    }

    private fun calculateScaledDimensions(
        actualWidth: Int,
        actualHeight: Int
    ): Pair<Int, Int> {
        var width = actualWidth
        var height = actualHeight

        if (width > MAX_WIDTH || height > MAX_HEIGHT) {
            val widthRatio = MAX_WIDTH.toFloat() / width
            val heightRatio = MAX_HEIGHT.toFloat() / height
            val bestRatio = minOf(widthRatio, heightRatio)

            width = (width * bestRatio).toInt()
            height = (height * bestRatio).toInt()
        }

        return Pair(width, height)
    }

    private fun applyExifRotation(imagePath: String, bitmap: Bitmap): Bitmap {
        return try {
            val exif = ExifInterface(imagePath)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)

            val matrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> matrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> matrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> matrix.postRotate(270f)
                else -> return bitmap // No rotation needed
            }

            Bitmap.createBitmap(bitmap, 0, 0, bitmap.width, bitmap.height, matrix, true)
        } catch (e: IOException) {
            e.printStackTrace()
            bitmap
        }
    }

    private fun calculateInSampleSize(options: BitmapFactory.Options, reqWidth: Int, reqHeight: Int): Int {
        val (height: Int, width: Int) = options.run { outHeight to outWidth }
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight: Int = height / 2
            val halfWidth: Int = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }
}
