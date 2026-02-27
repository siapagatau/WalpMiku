package com.farel.walpmiku

import android.graphics.*
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.text.format.DateFormat
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
                val height = desiredHeight
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

        override fun onSurfaceDestroyed(holder: SurfaceHolder?) {
            handler.removeCallbacks(updateScanline)
            handler.removeCallbacks(updateTime)
        }

        override fun onDraw(canvas: Canvas) {
            drawTerminal(canvas)
        }

        private fun drawTerminal(canvas: Canvas) {
            val width = desiredWidth
            val height = desiredHeight
            if (width <= 0 || height <= 0) return

            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val bgColor = prefs.getInt("bg_color", Color.BLACK)
            val textColor = prefs.getInt("text_color", Color.GREEN)
            val fontSize = prefs.getInt("font_size", 48)
            val customText = prefs.getString("custom_text", "Hello, World!") ?: "Hello, World!"

            // Latar belakang
            canvas.drawColor(bgColor)

            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                color = textColor
                textSize = fontSize.toFloat()
                typeface = Typeface.MONOSPACE
            }

            // Waktu
            val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            val timeX = width / 2f
            val timeY = height / 3f
            paint.textAlign = Paint.Align.CENTER
            canvas.drawText(currentTime, timeX, timeY, paint)

            // Teks kustom
            val customY = height / 2f
            canvas.drawText(customText, timeX, customY, paint)

            // Prompt "$ "
            val prompt = "$ "
            val promptY = customY + fontSize * 1.5f
            paint.textSize = fontSize * 0.8f
            canvas.drawText(prompt, timeX, promptY, paint)

            // Kursor berkedip
            val showCursor = (System.currentTimeMillis() / 500) % 2 == 0L
            if (showCursor) {
                val promptWidth = paint.measureText(prompt)
                val cursorX = timeX + promptWidth / 2 + 10
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
    }
}