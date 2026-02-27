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
                loadBackground()
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

            // Prompt
            val prompt = "$ "
            val promptY = yPos + fontSize * 0.3f
            paint.textSize = fontSize * 0.8f
            canvas.drawText(prompt, centerX, promptY, paint)

            // Kursor
            val showCursor = (System.currentTimeMillis() / 500) % 2 == 0L
            if (showCursor) {
                val promptWidth = paint.measureText(prompt)
                val cursorX = centerX + promptWidth / 2 + 10
                val cursorY = promptY - paint.textSize * 0.2f
                paint.style = Paint.Style.FILL
                paint.color = textColor
                canvas.drawRect(cursorX, cursorY, cursorX + 20, cursorY + paint.textSize * 0.8f, paint)
            }

            // Scanline bergerak
            paint.style = Paint.Style.FILL
            paint.color = Color.argb(50, 255, 255, 255)
            canvas.drawRect(0f, scanlineY, width.toFloat(), scanlineY + 2, paint)
        }

        private fun parseLine(line: String): String {
            return when {
                line.startsWith("#date") -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date())
                line.startsWith("#time") -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
                line.startsWith("#day") -> SimpleDateFormat("EEEE", Locale("id")).format(Date())
                line.startsWith("@") -> line.substring(1)
                else -> line
            }
        }
    }
}
