package com.swapy.imagecompressorex

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PointF
import android.graphics.Rect
import android.graphics.Typeface
import androidx.annotation.ColorInt
import androidx.core.content.res.ResourcesCompat
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object Util {
    fun addWatermarkToFile(
        sourceFilePath: String,
        latitude: Double?,
        longitude: Double?
    ) {
        val imageFile = File(sourceFilePath)
        if (!imageFile.exists()) return

        val sourceBitmap = BitmapFactory.decodeFile(imageFile.absolutePath)
        val dateFormat = SimpleDateFormat("dd-MM-yyyy hh:mm a", Locale.getDefault())
        val currentDate = dateFormat.format(Date())
        val options = WatermarkOptions()

        val result = sourceBitmap.copy(sourceBitmap.config ?: Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(result)

        val paint = Paint(ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            textAlign = when (options.corner) {
                Corner.TOP_LEFT, Corner.BOTTOM_LEFT -> Paint.Align.LEFT
                Corner.TOP_RIGHT, Corner.BOTTOM_RIGHT -> Paint.Align.RIGHT
            }
            textSize = result.width * options.textSizeToWidthRatio + 10
            color = options.textColor
            options.typeface?.let { typeface = it }
        }

        val borderPaint = Paint(ANTI_ALIAS_FLAG or Paint.DITHER_FLAG).apply {
            style = Paint.Style.STROKE
            strokeWidth = 3f
            color = options.shadowColor
            textSize = paint.textSize
            textAlign = paint.textAlign
            options.typeface?.let { typeface = it }
        }

        val padding = result.width * options.paddingToWidthRatio
        val coordinates = calculateCoordinates(
            currentDate,
            paint,
            options,
            canvas.width,
            canvas.height,
            padding
        )

        // Draw watermark text
        canvas.drawText(currentDate, coordinates.x, coordinates.y, borderPaint)
        canvas.drawText(currentDate, coordinates.x - 1, coordinates.y, paint)

        canvas.drawText("LONG: $longitude", coordinates.x, coordinates.y - 40, borderPaint)
        canvas.drawText("LONG: $longitude", coordinates.x - 1, coordinates.y - 40, paint)

        canvas.drawText("LAT: $latitude", coordinates.x, coordinates.y - 80, borderPaint)
        canvas.drawText("LAT: $latitude", coordinates.x - 1, coordinates.y - 80, paint)

        // Overwrite original image
        FileOutputStream(imageFile).use { outputStream ->
            result.compress(Bitmap.CompressFormat.JPEG, 90, outputStream)
            outputStream.flush()
        }

        result.recycle()
        sourceBitmap.recycle()
    }

    private fun calculateCoordinates(
        watermarkText: String,
        paint: Paint,
        options: WatermarkOptions,
        width: Int,
        height: Int,
        padding: Float
    ): PointF {
        val x = when (options.corner) {
            Corner.TOP_LEFT,
            Corner.BOTTOM_LEFT -> {
                padding
            }
            Corner.TOP_RIGHT,
            Corner.BOTTOM_RIGHT -> {
                width - padding
            }
        }
        val y = when (options.corner) {
            Corner.BOTTOM_LEFT,
            Corner.BOTTOM_RIGHT -> {
                height - padding
            }
            Corner.TOP_LEFT,
            Corner.TOP_RIGHT -> {
                val bounds = Rect()
                paint.getTextBounds(watermarkText, 0, watermarkText.length, bounds)
                val textHeight = bounds.height()
                textHeight + padding

            }
        }
        return PointF(x, y)
    }

    enum class Corner {
        TOP_LEFT,
        TOP_RIGHT,
        BOTTOM_LEFT,
        BOTTOM_RIGHT,
    }

    data class WatermarkOptions(
        val corner: Corner = Corner.BOTTOM_LEFT,
        val textSizeToWidthRatio: Float = 0.04f,
        val paddingToWidthRatio: Float = 0.04f,
        @ColorInt val textColor: Int = Color.WHITE,
        @ColorInt val shadowColor: Int = Color.BLACK,
        val typeface: Typeface? = ResourcesCompat.getFont(
            ImageCompressorApp.instance,
            R.font.bebasneuebold
        )
    )
}