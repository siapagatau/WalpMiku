package com.farel.walpmiku

import android.content.Context
import android.graphics.*
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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height
        if (width == 0 || height == 0) return

        canvas.drawColor(bgColor)
        paint.color = textColor
        paint.textSize = fontSize.toFloat()
        paint.typeface = Typeface.MONOSPACE
        paint.textAlign = Paint.Align.CENTER

        val currentTime = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(Date())
        val timeX = width / 2f
        val timeY = height / 3f
        canvas.drawText(currentTime, timeX, timeY, paint)

        val customY = height / 2f
        canvas.drawText(customText, timeX, customY, paint)

        val prompt = "$ "
        val promptY = customY + fontSize * 1.5f
        paint.textSize = fontSize * 0.8f
        canvas.drawText(prompt, timeX, promptY, paint)

        val showCursor = (System.currentTimeMillis() / 500) % 2 == 0L
        if (showCursor) {
            val promptWidth = paint.measureText(prompt)
            val cursorX = timeX + promptWidth / 2 + 10
            val cursorY = promptY - paint.textSize * 0.2f
            paint.style = Paint.Style.FILL
            paint.color = textColor
            canvas.drawRect(cursorX, cursorY, cursorX + 20, cursorY + paint.textSize * 0.8f, paint)
        }

        // Garis scanline statis di pratinjau (hanya contoh)
        paint.color = Color.argb(50, 255, 255, 255)
        paint.style = Paint.Style.FILL
        canvas.drawRect(0f, height * 0.7f, width.toFloat(), height * 0.7f + 2, paint)
    }
}