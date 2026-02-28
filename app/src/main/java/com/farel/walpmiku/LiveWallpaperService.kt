package com.farel.walpmiku

import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.net.Uri
import android.os.BatteryManager
import android.os.Handler
import android.os.Looper
import android.service.wallpaper.WallpaperService
import android.view.SurfaceHolder
import kotlin.math.max
import java.text.SimpleDateFormat
import java.util.*

class LiveWallpaperService : WallpaperService() {

    override fun onCreateEngine(): Engine = TerminalEngine()

    inner class TerminalEngine : Engine() {

        private val handler = Handler(Looper.getMainLooper())

        private var backgroundBitmap: Bitmap? = null
        private var batteryLevel = 0
        private var isCharging = false
        private var currentUriString: String? = null

        // 🔥 SIMPAN UKURAN SURFACE YANG VALID
        private var surfaceWidth = 0
        private var surfaceHeight = 0

        private val updateTime = object : Runnable {
            override fun run() {
                updateBatteryInfo()
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

        override fun onSurfaceCreated(holder: SurfaceHolder) {
            super.onSurfaceCreated(holder)
        }

        override fun onSurfaceChanged(
            holder: SurfaceHolder,
            format: Int,
            width: Int,
            height: Int
        ) {
            super.onSurfaceChanged(holder, format, width, height)

            surfaceWidth = width
            surfaceHeight = height

            loadBackground()
            drawFrame()
        }

        override fun onSurfaceDestroyed(holder: SurfaceHolder) {
            super.onSurfaceDestroyed(holder)
            handler.removeCallbacks(updateTime)
        }

        private fun updateBatteryInfo() {
            val batteryStatus: Intent? =
                applicationContext.registerReceiver(
                    null,
                    IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                )

            batteryLevel = batteryStatus?.let { intent ->
                val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
                val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
                if (scale > 0) (level * 100 / scale.toFloat()).toInt() else 0
            } ?: 0

            isCharging = batteryStatus?.let { intent ->
                val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
                status == BatteryManager.BATTERY_STATUS_CHARGING ||
                        status == BatteryManager.BATTERY_STATUS_FULL
            } ?: false
        }

        private fun loadBackground() {

            val width = surfaceWidth
            val height = surfaceHeight
            if (width <= 0 || height <= 0) return

            val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
            val uriString = prefs.getString("background_image_uri", null)

            if (uriString.isNullOrEmpty()) return

            // Kalau URI berubah, reset bitmap (tanpa recycle paksa)
            if (currentUriString != uriString) {
                backgroundBitmap = null
                currentUriString = uriString
            }

            // Kalau sudah sesuai ukuran, tidak perlu reload
            if (backgroundBitmap != null &&
                backgroundBitmap!!.width == width &&
                backgroundBitmap!!.height == height
            ) {
                return
            }

            try {
                val uri = Uri.parse(uriString)

                contentResolver.openInputStream(uri)?.use { input ->
                    val original = BitmapFactory.decodeStream(input) ?: return

                    val scale = max(
                        width.toFloat() / original.width.toFloat(),
                        height.toFloat() / original.height.toFloat()
                    )

                    val scaledWidth =
                        (original.width.toFloat() * scale).toInt()

                    val scaledHeight =
                        (original.height.toFloat() * scale).toInt()

                    val scaledBitmap = Bitmap.createScaledBitmap(
                        original,
                        scaledWidth,
                        scaledHeight,
                        true
                    )

                    val left = (scaledWidth - width) / 2
                    val top = (scaledHeight - height) / 2

                    backgroundBitmap = Bitmap.createBitmap(
                        scaledBitmap,
                        left,
                        top,
                        width,
                        height
                    )

                    scaledBitmap.recycle()
                    original.recycle()
                }

            } catch (e: Exception) {
                e.printStackTrace()
                // 🔥 Jangan set ke null biar gak jadi hitam
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
            val customText =
                prefs.getString("custom_text", "Hello, World!")
                    ?: "Hello, World!"
            val offsetX = prefs.getInt("offset_x", 0)
            val offsetY = prefs.getInt("offset_y", 0)

            // Background
            if (backgroundBitmap != null) {
                canvas.drawBitmap(backgroundBitmap!!, 0f, 0f, null)

                val overlay = Paint().apply {
                    color = Color.argb(100, 0, 0, 0)
                }
                canvas.drawRect(
                    0f,
                    0f,
                    width.toFloat(),
                    height.toFloat(),
                    overlay
                )
            } else {
                canvas.drawColor(bgColor)
            }

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

            val dateNow =
                SimpleDateFormat("dd MMM yyyy", Locale("id"))
                    .format(Date())

            val timeNow =
                SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date())

            val dayNow =
                SimpleDateFormat("EEEE", Locale("id"))
                    .format(Date())

            val batteryText =
                "$batteryLevel% ${if (isCharging) "(charging)" else ""}"
                    .trim()

            result = result.replace(
                Regex("#date", RegexOption.IGNORE_CASE),
                dateNow
            )

            result = result.replace(
                Regex("#time", RegexOption.IGNORE_CASE),
                timeNow
            )

            result = result.replace(
                Regex("#day", RegexOption.IGNORE_CASE),
                dayNow
            )

            result = result.replace(
                Regex("#battery", RegexOption.IGNORE_CASE),
                batteryText
            )

            result = result.replace(
                Regex("#charging", RegexOption.IGNORE_CASE),
                if (isCharging) "Charging" else "Not charging"
            )

            result = result.replace(
                Regex("@(\\w+)", RegexOption.IGNORE_CASE)
            ) {
                it.groupValues[1]
            }

            return result
        }
    }
}
