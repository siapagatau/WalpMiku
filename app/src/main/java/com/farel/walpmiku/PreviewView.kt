package com.farel.walpmiku

import android.content.Context
import android.graphics.*
import android.net.Uri
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height
        if (width == 0 || height == 0) return

        // Background
        if (backgroundImageUri != null) {
            try {
                context.contentResolver.openInputStream(backgroundImageUri!!)?.use { input ->
                    val original = BitmapFactory.decodeStream(input)
                    original?.let {
                        val scaled = Bitmap.createScaledBitmap(it, width, height, true)
                        canvas.drawBitmap(scaled, 0f, 0f, null)
                        scaled.recycle()
                        it.recycle()
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
        return when {
            line.startsWith("#date") -> SimpleDateFormat("dd MMM yyyy", Locale("id")).format(Date())
            line.startsWith("#time") -> SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
            line.startsWith("#day") -> SimpleDateFormat("EEEE", Locale("id")).format(Date())
            line.startsWith("@") -> line.substring(1)
            else -> line
        }
    }
}
