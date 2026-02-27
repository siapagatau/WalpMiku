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

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = width
        val height = height
        if (width == 0 || height == 0) return

        // Gambar background (foto jika ada)
        if (backgroundImageUri != null) {
            try {
                context.contentResolver.openInputStream(backgroundImageUri!!)?.use { inputStream ->
                    val options = BitmapFactory.Options().apply { inSampleSize = 2 }
                    val originalBitmap = BitmapFactory.decodeStream(inputStream, null, options)
                    originalBitmap?.let { bmp ->
                        val scaledBitmap = Bitmap.createScaledBitmap(bmp, width, height, true)
                        canvas.drawBitmap(scaledBitmap, 0f, 0f, null)
                        scaledBitmap.recycle()
                    }
                }
            } catch (e: Exception) {
                canvas.drawColor(bgColor)
            }
        } else {
            canvas.drawColor(bgColor)
        }

        // Teks terminal
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

        // Scanline statis di pratinjau
        paint.style = Paint.Style.FILL
        paint.color = Color.argb(50, 255, 255, 255)
        canvas.drawRect(0f, height * 0.7f, width.toFloat(), height * 0.7f + 2, paint)
    }
}
