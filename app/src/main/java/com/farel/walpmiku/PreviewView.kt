package com.farel.walpmiku

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.graphics.*
import android.net.Uri
import android.os.BatteryManager
import android.util.AttributeSet
import android.view.View
import java.text.SimpleDateFormat
import java.util.*

class PreviewView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private var textColor = Color.GREEN
    private var bgColor = Color.BLACK
    private var fontSize = 48
    private var customText = "Hello, World!"
    private var backgroundImageUri: Uri? = null
    private var offsetX = 0
    private var offsetY = 0
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)

    // Cache baterai
    private var batteryLevel: Int = 0
    private var isCharging: Boolean = false

    fun setColors(textColor: Int, bgColor: Int, fontSize: Int, customText: String) {
        this.textColor = textColor
        this.bgColor = bgColor
        this.fontSize = fontSize
        this.customText = customText
        invalidate()
    }

    fun updateText(text: String) { customText = text; invalidate() }
    fun updateFontSize(size: Int) { fontSize = size; invalidate() }
    fun updateTextColor(color: Int) { textColor = color; invalidate() }
    fun updateBgColor(color: Int) { bgColor = color; invalidate() }
    fun setBackgroundImage(uri: Uri?) { backgroundImageUri = uri; invalidate() }
    fun setOffset(x: Int, y: Int) { offsetX = x; offsetY = y; invalidate() }

    private fun updateBatteryInfo() {
        val batteryStatus: Intent? = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        batteryLevel = batteryStatus?.let { intent ->
            val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
            val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
            (level * 100 / scale.toFloat()).toInt()
        } ?: 0

        isCharging = batteryStatus?.let { intent ->
            val status = intent.getIntExtra(BatteryManager.EXTRA_STATUS, -1)
            status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        } ?: false
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height
        if (width == 0 || height == 0) return

        updateBatteryInfo()

        // Background gambar (center-crop)
        if (backgroundImageUri != null) {
            try {
                context.contentResolver.openInputStream(backgroundImageUri!!)?.use { inputStream ->
                    val originalBitmap = BitmapFactory.decodeStream(inputStream)
                    originalBitmap?.let { bmp ->
                        // Hitung skala agar gambar menutupi seluruh canvas (center crop)
                        val scale = max(width.toFloat() / bmp.width, height.toFloat() / bmp.height)
                        val scaledWidth = (bmp.width * scale).toInt()
                        val scaledHeight = (bmp.height * scale).toInt()
                        val scaledBitmap = Bitmap.createScaledBitmap(bmp, scaledWidth, scaledHeight, true)
                        // Potong bagian tengah agar sesuai ukuran canvas
                        val left = (scaledWidth - width) / 2
                        val top = (scaledHeight - height) / 2
                        val croppedBitmap = Bitmap.createBitmap(scaledBitmap, left, top, width, height)
                        canvas.drawBitmap(croppedBitmap, 0f, 0f, null)
                        croppedBitmap.recycle()
                        scaledBitmap.recycle()
                        bmp.recycle()
                    }
                }
            } catch (e: Exception) {
                canvas.drawColor(bgColor)
            }
        } else {
            canvas.drawColor(bgColor)
        }

        // Teks
        paint.color = textColor
        paint.textSize = fontSize.toFloat()
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER

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
        val dateNow = SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date())
        val timeNow = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val dayNow = SimpleDateFormat("EEEE", Locale("id")).format(Date())
        val batteryText = "$batteryLevel% ${if (isCharging) "(charging)" else ""}".trim()

        result = result.replace(Regex("#date", RegexOption.IGNORE_CASE), dateNow)
        result = result.replace(Regex("#time", RegexOption.IGNORE_CASE), timeNow)
        result = result.replace(Regex("#day", RegexOption.IGNORE_CASE), dayNow)
        result = result.replace(Regex("#battery", RegexOption.IGNORE_CASE), batteryText)
        result = result.replace(Regex("#charging", RegexOption.IGNORE_CASE), if (isCharging) "Charging" else "Not charging")
        result = result.replace(Regex("@(\\w+)", RegexOption.IGNORE_CASE)) {
            it.groupValues[1]
        }
        return result
    }
}
