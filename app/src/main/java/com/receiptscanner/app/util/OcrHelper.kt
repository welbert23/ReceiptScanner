package com.receiptscanner.app.util

import android.graphics.Bitmap
import android.graphics.ColorMatrix
import android.graphics.ColorMatrixColorFilter
import android.graphics.Paint
import android.graphics.Canvas
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.tasks.await

class OcrHelper {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    suspend fun recognizeText(bitmap: Bitmap): String {
        val processed = preprocessForOcr(bitmap)
        val image = InputImage.fromBitmap(processed, 0)
        val result = recognizer.process(image).await()
        return result.text
    }

    private fun preprocessForOcr(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height

        val targetMin = 1200f
        val scale = if (w < targetMin || h < targetMin) {
            maxOf(targetMin / w, targetMin / h).coerceAtMost(2.5f)
        } else 1.0f

        val scaledW = (w * scale).toInt()
        val scaledH = (h * scale).toInt()
        val scaled = if (scale > 1.05f) Bitmap.createScaledBitmap(src, scaledW, scaledH, true) else src

        val result = Bitmap.createBitmap(scaledW, scaledH, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        val cm = ColorMatrix().apply {
            setSaturation(0f)
            val contrast = 1.4f
            val brightness = -30f
            set(floatArrayOf(
                contrast, 0f, 0f, 0f, brightness,
                0f, contrast, 0f, 0f, brightness,
                0f, 0f, contrast, 0f, brightness,
                0f, 0f, 0f, 1f, 0f
            ))
        }
        val paint = Paint().apply {
            colorFilter = ColorMatrixColorFilter(cm)
        }
        canvas.drawBitmap(scaled, 0f, 0f, paint)
        return result
    }
}
