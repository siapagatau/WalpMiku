package com.farel.walpmiku

import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.view.ViewGroup
import android.widget.*
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var editText: EditText
    private lateinit var seekBarFont: SeekBar
    private lateinit var textFontSize: TextView
    private lateinit var seekBarOffsetX: SeekBar
    private lateinit var seekBarOffsetY: SeekBar
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickTextColor: Button
    private lateinit var btnPickBgColor: Button
    private lateinit var btnPickFromGallery: Button
    private lateinit var btnClearImage: Button

    private var textColor = Color.GREEN
    private var bgColor = Color.BLACK
    private var selectedImageUri: Uri? = null
    private var offsetX = 0
    private var offsetY = 0

    private val handler = Handler(Looper.getMainLooper())

    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            previewView.invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    // ✅ SAF picker dengan persist permission
    private val pickImageLauncher =
        registerForActivityResult(ActivityResultContracts.OpenDocument()) { uri ->
            if (uri != null) {

                contentResolver.takePersistableUriPermission(
                    uri,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                selectedImageUri = uri
                previewView.setBackgroundImage(uri)
                savePrefs()
            }
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.preview_view)
        editText = findViewById(R.id.edit_custom_text)
        seekBarFont = findViewById(R.id.seekbar_font_size)
        textFontSize = findViewById(R.id.text_font_size)
        seekBarOffsetX = findViewById(R.id.seekbar_offset_x)
        seekBarOffsetY = findViewById(R.id.seekbar_offset_y)
        btnSetWallpaper = findViewById(R.id.btn_set_wallpaper)
        btnPickTextColor = findViewById(R.id.btn_text_color)
        btnPickBgColor = findViewById(R.id.btn_bg_color)
        btnPickFromGallery = findViewById(R.id.btn_pick_from_gallery)
        btnClearImage = findViewById(R.id.btn_clear_image)

        // 🔥 FIX: Preview tidak boleh intercept touch
        previewView.isClickable = false
        previewView.isFocusable = false
        previewView.isFocusableInTouchMode = false
        previewView.setOnTouchListener { _, _ -> false }

        // Atur rasio preview sesuai layar
        previewView.post {
            val previewWidth = previewView.width
            if (previewWidth > 0) {
                val displayMetrics = resources.displayMetrics
                val ratio = displayMetrics.heightPixels.toFloat() /
                        displayMetrics.widthPixels.toFloat()
                val desiredHeight = (previewWidth * ratio).toInt()

                val params = previewView.layoutParams
                params.height = desiredHeight
                previewView.layoutParams = params
            }
        }

        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)

        textColor = prefs.getInt("text_color", Color.GREEN)
        bgColor = prefs.getInt("bg_color", Color.BLACK)

        val savedText = prefs.getString("custom_text", "Hello, World!")!!
        val fontSize = prefs.getInt("font_size", 48)

        offsetX = prefs.getInt("offset_x", 0)
        offsetY = prefs.getInt("offset_y", 0)

        val uriString = prefs.getString("background_image_uri", null)
        selectedImageUri = uriString?.let { Uri.parse(it) }

        editText.setText(savedText)
        seekBarFont.progress = fontSize
        textFontSize.text = "Font size: $fontSize"
        seekBarOffsetX.progress = offsetX + 500
        seekBarOffsetY.progress = offsetY + 500

        previewView.setColors(textColor, bgColor, fontSize, savedText)
        previewView.setBackgroundImage(selectedImageUri)
        previewView.setOffset(offsetX, offsetY)

        btnPickTextColor.setBackgroundColor(textColor)
        btnPickTextColor.setTextColor(getContrastColor(textColor))

        btnPickBgColor.setBackgroundColor(bgColor)
        btnPickBgColor.setTextColor(getContrastColor(bgColor))

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                previewView.updateText(s.toString())
                savePrefs()
            }
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) {}
            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {}
        })

        seekBarFont.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                textFontSize.text = "Font size: $progress"
                previewView.updateFontSize(progress)
                savePrefs()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarOffsetX.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                offsetX = progress - 500
                previewView.setOffset(offsetX, offsetY)
                savePrefs()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        seekBarOffsetY.setOnSeekBarChangeListener(object : SeekBar.OnSeekBarChangeListener {
            override fun onProgressChanged(seekBar: SeekBar?, progress: Int, fromUser: Boolean) {
                offsetY = progress - 500
                previewView.setOffset(offsetX, offsetY)
                savePrefs()
            }
            override fun onStartTrackingTouch(seekBar: SeekBar?) {}
            override fun onStopTrackingTouch(seekBar: SeekBar?) {}
        })

        btnPickTextColor.setOnClickListener {
            ColorPickerDialog(this, textColor) { color ->
                textColor = color
                previewView.updateTextColor(color)
                btnPickTextColor.setBackgroundColor(color)
                btnPickTextColor.setTextColor(getContrastColor(color))
                savePrefs()
            }.show()
        }

        btnPickBgColor.setOnClickListener {
            ColorPickerDialog(this, bgColor) { color ->
                bgColor = color
                previewView.updateBgColor(color)
                btnPickBgColor.setBackgroundColor(color)
                btnPickBgColor.setTextColor(getContrastColor(color))
                savePrefs()
            }.show()
        }

        btnPickFromGallery.setOnClickListener {
            pickImageLauncher.launch(arrayOf("image/*"))
        }

        btnClearImage.setOnClickListener {
            selectedImageUri?.let {
                try {
                    contentResolver.releasePersistableUriPermission(
                        it,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                    )
                } catch (_: Exception) {}
            }

            selectedImageUri = null
            previewView.setBackgroundImage(null)
            savePrefs()
        }

        btnSetWallpaper.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(
                WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, LiveWallpaperService::class.java)
            )
            startActivity(intent)
        }
    }

    override fun onResume() {
        super.onResume()
        handler.post(updateTimeRunnable)
    }

    override fun onPause() {
        super.onPause()
        handler.removeCallbacks(updateTimeRunnable)
    }

    private fun getContrastColor(color: Int): Int {
        val darkness = 1 - (0.299 * Color.red(color) +
                0.587 * Color.green(color) +
                0.114 * Color.blue(color)) / 255
        return if (darkness < 0.5) Color.BLACK else Color.WHITE
    }

    private fun savePrefs() {
        getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit().apply {
            putInt("text_color", textColor)
            putInt("bg_color", bgColor)
            putString("custom_text", editText.text.toString())
            putInt("font_size", seekBarFont.progress)
            putInt("offset_x", offsetX)
            putInt("offset_y", offsetY)
            putString("background_image_uri", selectedImageUri?.toString())
            apply()
        }
    }
}
