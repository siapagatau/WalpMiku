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
    override fun onCreateEngine(): Engine = TerminalEngine()

    inner class TerminalEngine : Engine() {
        private val handler = Handler(Looper.getMainLooper())
        private var scanlineY = 0f
        private var scanlineDirection = 1

        private val updateScanline = object : Runnable {
            override fun run() {
                val height = surfaceHolder.surfaceFrame.height()
                if (height > 0) {
                    scanlineY += 5 * scanlineDirection
                    if (scanlineY >= height) {
                        scanlineY = height.toFloat()
                        scanlineDirection = -1
                    } else if (scanlineY <= 0) {
                        scanlineY = 0f
                        scanlineDirection = 1
                    }
                }
                drawFrame()
                handler.postDelayed(this, 50)
            }
        }

        private val updateTime = object : Runnable {
            override fun run() {
                drawFrame()
                handler.postDelayed(this, 1000)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                handler.post(updateScanline)
                handler.post(updateTime)
            } else {
                handler.removeCallbacks(updateScanline)
                handler.removeCallbacks(updateTime)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(updateScanline)
            handler.removeCallbacks(updateTime)
        }

        fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                if (canvas != null) {
                    drawTerminal(canvas)
                }
            } finally {
                if (canvas != null) {
                    holder.unlockCanvasAndPost(canvas)
                }
            }
        }

private fun drawTerminal(canvas: Canvas) {
    val width = surfaceHolder.surfaceFrame.width()
    val height = surfaceHolder.surfaceFrame.height()
    if (width <= 0 || height <= 0) return

    val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
    val bgColor = prefs.getInt("bg_color", Color.BLACK)
    val textColor = prefs.getInt("text_color", Color.WHITE)
    val fontSize = prefs.getInt("font_size", 80)
    val customText = prefs.getString("custom_text", "Hello") ?: "Hello"
    val imageUriString = prefs.getString("background_image_uri", null)

    // ===== BACKGROUND =====
    if (imageUriString != null) {
        try {
            val imageUri = Uri.parse(imageUriString)
            contentResolver.openInputStream(imageUri)?.use { inputStream ->
                val originalBitmap = BitmapFactory.decodeStream(inputStream)
                originalBitmap?.let { bmp ->
                    val scaledBitmap = Bitmap.createScaledBitmap(bmp, width, height, true)
                    canvas.drawBitmap(scaledBitmap, 0f, 0f, null)

                    // overlay gelap biar teks kebaca
                    val overlayPaint = Paint()
                    overlayPaint.color = Color.argb(120, 0, 0, 0)
                    canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlayPaint)

                    scaledBitmap.recycle()
                }
            }
        } catch (e: Exception) {
            canvas.drawColor(bgColor)
        }
    } else {
        canvas.drawColor(bgColor)
    }

    // ===== JAM =====
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val currentTime = timeFormat.format(Date())

    val timePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = fontSize.toFloat()
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.BOLD)
        textAlign = Paint.Align.CENTER
        setShadowLayer(15f, 0f, 0f, Color.BLACK)
    }

    val timeX = width / 2f
    val timeY = height / 2f
    canvas.drawText(currentTime, timeX, timeY, timePaint)

    // ===== TEXT CUSTOM =====
    val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = textColor
        textSize = fontSize * 0.35f
        typeface = Typeface.create(Typeface.SANS_SERIF, Typeface.NORMAL)
        textAlign = Paint.Align.CENTER
        setShadowLayer(10f, 0f, 0f, Color.BLACK)
    }

    canvas.drawText(customText, timeX, timeY + fontSize * 0.8f, textPaint)
}
    }
}
