package com.swapy.imagecompressor

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import androidx.core.graphics.createBitmap
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object ImageCompressor {

    fun compressImage(
        imageUri: String,
        maxWidth: Float = 960f,
        maxHeight: Float = 1280f,
        compressQuality: Int = 90
    ): String {
        val file = File(imageUri)
        if (!file.exists()) return imageUri

        val options = BitmapFactory.Options().apply {
            inJustDecodeBounds = true
        }
        BitmapFactory.decodeFile(imageUri, options)

        var actualWidth = options.outWidth
        var actualHeight = options.outHeight
        val imgRatio = actualWidth.toFloat() / actualHeight
        val maxRatio = maxWidth / maxHeight

        if (actualHeight > maxHeight || actualWidth > maxWidth) {
            if (imgRatio < maxRatio) {
                val ratio = maxHeight / actualHeight
                actualWidth = (ratio * actualWidth).toInt()
                actualHeight = maxHeight.toInt()
            } else if (imgRatio > maxRatio) {
                val ratio = maxWidth / actualWidth
                actualHeight = (ratio * actualHeight).toInt()
                actualWidth = maxWidth.toInt()
            } else {
                actualWidth = maxWidth.toInt()
                actualHeight = maxHeight.toInt()
            }
        }

        val sampleSize = calculateInSampleSize(options, actualWidth, actualHeight)
        options.inJustDecodeBounds = false
        options.inSampleSize = sampleSize
        options.inPreferredConfig = Bitmap.Config.ARGB_8888

        val bmp = BitmapFactory.decodeFile(imageUri, options) ?: return imageUri

        val scaledBitmap = createBitmap(actualWidth, actualHeight, Bitmap.Config.ARGB_8888)
        val scaleMatrix = Matrix().apply {
            setScale(
                actualWidth.toFloat() / bmp.width,
                actualHeight.toFloat() / bmp.height
            )
        }
        val canvas = Canvas(scaledBitmap)
        canvas.setMatrix(scaleMatrix)
        canvas.drawBitmap(bmp, 0f, 0f, Paint(Paint.FILTER_BITMAP_FLAG))

        // Handle image rotation via EXIF
        try {
            val exif = ExifInterface(imageUri)
            val orientation = exif.getAttributeInt(ExifInterface.TAG_ORIENTATION, ExifInterface.ORIENTATION_NORMAL)
            val rotationMatrix = Matrix()
            when (orientation) {
                ExifInterface.ORIENTATION_ROTATE_90 -> rotationMatrix.postRotate(90f)
                ExifInterface.ORIENTATION_ROTATE_180 -> rotationMatrix.postRotate(180f)
                ExifInterface.ORIENTATION_ROTATE_270 -> rotationMatrix.postRotate(270f)
            }
            val rotatedBitmap = Bitmap.createBitmap(
                scaledBitmap,
                0,
                0,
                scaledBitmap.width,
                scaledBitmap.height,
                rotationMatrix,
                true
            )
            scaledBitmap.recycle()

            // Overwrite original file
            FileOutputStream(file).use { out ->
                rotatedBitmap.compress(Bitmap.CompressFormat.JPEG, compressQuality, out)
            }
            rotatedBitmap.recycle()
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return imageUri
    }

    private fun calculateInSampleSize(
        options: BitmapFactory.Options,
        reqWidth: Int,
        reqHeight: Int
    ): Int {
        val height = options.outHeight
        val width = options.outWidth
        var inSampleSize = 1

        if (height > reqHeight || width > reqWidth) {
            val halfHeight = height / 2
            val halfWidth = width / 2

            while ((halfHeight / inSampleSize) >= reqHeight && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2
            }
        }
        return inSampleSize
    }

}