package com.paici.app.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.BlurMaskFilter
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.PorterDuffXfermode
import com.google.mediapipe.framework.image.BitmapExtractor
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.imagesegmenter.ImageSegmenter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * MediaPipe selfie segmenter wrapper.
 * Produces a [Bitmap] with transparent background + white glow/outline composite,
 * or null if segmentation fails / model is unavailable.
 *
 * The model file `selfie_segmenter.tflite` must be placed in app/src/main/assets/.
 * If missing, initialisation silently sets [segmenter] to null and every call
 * to [process] returns null (caller should fall back to the original bitmap).
 */
class SubjectSegmentationProcessor(context: Context) {

    private val segmenter: ImageSegmenter? = try {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("selfie_segmenter.tflite")
            .build()
        val options = ImageSegmenter.ImageSegmenterOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setOutputCategoryMask(true)
            .setOutputConfidenceMasks(false)
            .build()
        ImageSegmenter.createFromOptions(context, options)
    } catch (_: Exception) {
        null
    }

    /**
     * Runs segmentation on [bitmap] and returns a composited display bitmap
     * (transparent BG + white glow + white outline + foreground), or null on failure.
     */
    suspend fun process(bitmap: Bitmap): Bitmap? = withContext(Dispatchers.Default) {
        if (segmenter == null) return@withContext null
        try {
            // 1. Scale to model input size
            val scaled = Bitmap.createScaledBitmap(bitmap, 256, 256, true)

            // 2. Run segmentation
            val result = segmenter.segment(BitmapImageBuilder(scaled).build())
            val maskImage = result.categoryMask().orElse(null) ?: return@withContext null

            // 3. Extract mask as ByteBuffer (uint8: 0=background, 1=subject)
            val maskBitmap = BitmapExtractor.extract(maskImage)

            // 4. Build foreground bitmap at original size
            val w = bitmap.width
            val h = bitmap.height
            val scaledMask = Bitmap.createScaledBitmap(maskBitmap, w, h, true)

            // Create alpha mask: white where subject (pixel value > 0)
            val alphaMask = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val maskPixels = IntArray(w * h)
            scaledMask.getPixels(maskPixels, 0, w, 0, 0, w, h)
            for (i in maskPixels.indices) {
                // category mask stores category index in red channel of ARGB
                val category = Color.red(maskPixels[i])
                maskPixels[i] = if (category > 0) Color.WHITE else Color.TRANSPARENT
            }
            alphaMask.setPixels(maskPixels, 0, w, 0, 0, w, h)

            // Extract foreground using DST_IN
            val fgBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val fgCanvas = Canvas(fgBitmap)
            fgCanvas.drawBitmap(bitmap, 0f, 0f, null)
            val dstInPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
            }
            fgCanvas.drawBitmap(alphaMask, 0f, 0f, dstInPaint)

            // 5. Check foreground coverage — skip if < 5%
            val fgPixels = IntArray(w * h)
            fgBitmap.getPixels(fgPixels, 0, w, 0, 0, w, h)
            val nonTransparent = fgPixels.count { Color.alpha(it) > 32 }
            val coverage = nonTransparent.toFloat() / (w * h)
            if (coverage < 0.05f) return@withContext null

            // 6. Build white silhouette for glow/outline
            val silhouette = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val silCanvas = Canvas(silhouette)
            val silPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = PorterDuffColorFilter(Color.WHITE, PorterDuff.Mode.SRC_IN)
            }
            silCanvas.drawBitmap(fgBitmap, 0f, 0f, silPaint)

            // 7. Composite: glow (wide blur) + outline (tight blur) + foreground
            val displayBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
            val displayCanvas = Canvas(displayBitmap)
            displayCanvas.drawColor(Color.TRANSPARENT, PorterDuff.Mode.CLEAR)

            // Glow layer — wide blur
            val glowPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = BlurMaskFilter(24f, BlurMaskFilter.Blur.NORMAL)
                alpha = 180
            }
            displayCanvas.drawBitmap(silhouette, 0f, 0f, glowPaint)

            // Outline layer — tight blur
            val outlinePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                maskFilter = BlurMaskFilter(6f, BlurMaskFilter.Blur.NORMAL)
                alpha = 230
            }
            displayCanvas.drawBitmap(silhouette, 0f, 0f, outlinePaint)

            // Foreground on top
            displayCanvas.drawBitmap(fgBitmap, 0f, 0f, null)

            displayBitmap
        } catch (_: Exception) {
            null
        }
    }

    fun close() {
        segmenter?.close()
    }
}
