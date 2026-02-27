package com.farel.walpmiku

import android.graphics.*
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import java.text.SimpleDateFormat
import java.util.*

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = CleanEngine()

    inner class CleanEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())
        private var backgroundBitmap: Bitmap? = null

        private val updateTimeRunnable = object : Runnable {
            override fun run() {
                drawFrame()
                handler.postDelayed(this, 1000)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                loadBackground()
                handler.post(updateTimeRunnable)
            } else {
                handler.removeCallbacks(updateTimeRunnable)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(updateTimeRunnable)
            backgroundBitmap?.recycle()
            backgroundBitmap = null
        }

        private fun loadBackground() {
            backgroundBitmap?.recycle()
            backgroundBitmap = null

            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val imageUriString = prefs.getString("background_image_uri", null)
            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()

            if (width <= 0 || height <= 0) return

            if (imageUriString != null) {
                try {
                    val uri = Uri.parse(imageUriString)
                    contentResolver.openInputStream(uri)?.use { input ->
                        val original = BitmapFactory.decodeStream(input)
                        original?.let {
                            backgroundBitmap =
                                Bitmap.createScaledBitmap(it, width, height, true)
                            it.recycle()
                        }
                    }
                } catch (_: Exception) {
                }
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { drawWallpaper(it) }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun drawWallpaper(canvas: Canvas) {

            val width = canvas.width
            val height = canvas.height

            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val bgColor = prefs.getInt("bg_color", Color.BLACK)
            val textColor = prefs.getInt("text_color", Color.WHITE)
            val fontSize = prefs.getInt("font_size", 100)
            val customText = prefs.getString("custom_text", "Hello") ?: "Hello"

            // ===== BACKGROUND =====
            if (backgroundBitmap != null) {
                canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)

                // overlay biar teks kebaca
                val overlayPaint = Paint().apply {
                    color = Color.argb(130, 0, 0, 0)
                }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

            } else {
                canvas.drawColor(bgColor)
            }

            // ===== TIME =====
            val timeText = SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date())

            val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = fontSize.toFloat()
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
                textAlign = Paint.Align.CENTER
                setShadowLayer(20f, 0f, 0f, Color.BLACK)
            }

            val centerX = width / 2f
            val centerY = height / 2f

            canvas.drawText(timeText, centerX, centerY, timePaint)

            // ===== CUSTOM TEXT =====
            val customPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = fontSize * 0.35f
                typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
                textAlign = Paint.Align.CENTER
                setShadowLayer(12f, 0f, 0f, Color.BLACK)
            }

            canvas.drawText(
                customText,
                centerX,
                centerY + fontSize * 0.8f,
                customPaint
            )
        }
    }
}
