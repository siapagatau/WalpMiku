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
        private var backgroundBitmap: Bitmap? = null

        private val updateTime = object : Runnable {
            override fun run() {
                drawFrame()
                handler.postDelayed(this, 1000)
            }
        }

        override fun onVisibilityChanged(visible: Boolean) {
            if (visible) {
                loadBackground()
                handler.post(updateTime)
            } else {
                handler.removeCallbacks(updateTime)
            }
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(updateTime)
            backgroundBitmap?.recycle()
            backgroundBitmap = null
        }

        private fun loadBackground() {
            backgroundBitmap?.recycle()
            backgroundBitmap = null

            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val uriString = prefs.getString("background_image_uri", null)
            val width = surfaceHolder.surfaceFrame.width()
            val height = surfaceHolder.surfaceFrame.height()
            if (width <= 0 || height <= 0) return

            if (uriString != null) {
                try {
                    val uri = Uri.parse(uriString)
                    contentResolver.openInputStream(uri)?.use { input ->
                        val original = BitmapFactory.decodeStream(input)
                        original?.let {
                            backgroundBitmap = Bitmap.createScaledBitmap(it, width, height, true)
                            it.recycle()
                        }
                    }
                } catch (_: Exception) { }
            }
        }

        private fun drawFrame() {
            val holder = surfaceHolder
            var canvas: Canvas? = null
            try {
                canvas = holder.lockCanvas()
                canvas?.let { drawContent(it) }
            } finally {
                canvas?.let { holder.unlockCanvasAndPost(it) }
            }
        }

        private fun drawContent(canvas: Canvas) {
            val width = canvas.width
            val height = canvas.height

            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val bgColor = prefs.getInt("bg_color", Color.BLACK)
            val textColor = prefs.getInt("text_color", Color.GREEN)
            val fontSize = prefs.getInt("font_size", 48)
            val customText = prefs.getString("custom_text", "Hello, World!") ?: "Hello, World!"
            val offsetX = prefs.getInt("offset_x", 0)
            val offsetY = prefs.getInt("offset_y", 0)

            // Background
            if (backgroundBitmap != null) {
                canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)
                // overlay gelap agar teks terbaca
                val overlay = Paint().apply { color = Color.argb(100, 0, 0, 0) }
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), overlay)
            } else {
                canvas.drawColor(bgColor)
            }

            // Teks
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = fontSize.toFloat()
                typeface = Typeface.MONOSPACE
                textAlign = Paint.Align.CENTER
            }

            val centerX = width / 2f + offsetX
            val centerY = height / 2f + offsetY

            val lines = customText.split("\n")
            var yPos = centerY
            for (line in lines) {
                val displayText = parseLine(line)
                canvas.drawText(displayText, centerX, yPos, paint)
                yPos += fontSize * 1.2f
            }
        }

        private fun parseLine(line: String): String {
            var result = line
            result = result.replace(Regex("#date", RegexOption.IGNORE_CASE)) {
                SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date())
            }
            result = result.replace(Regex("#time", RegexOption.IGNORE_CASE)) {
                SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            }
            result = result.replace(Regex("#day", RegexOption.IGNORE_CASE)) {
                SimpleDateFormat("EEEE", Locale("id")).format(Date())
            }
            result = result.replace(Regex("@(\\w+)")) {
                it.groupValues[1]
            }
            return result
        }
    }
}
