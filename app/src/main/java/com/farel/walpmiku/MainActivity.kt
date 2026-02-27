package com.farel.walpmiku

import android.content.ComponentName
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.app.WallpaperManager
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {
    private lateinit var previewView: PreviewView
    private lateinit var editText: EditText
    private lateinit var seekBarFont: SeekBar
    private lateinit var textFontSize: TextView
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickTextColor: Button
    private lateinit var btnPickBgColor: Button

    private var textColor = Color.GREEN
    private var bgColor = Color.BLACK
    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            previewView.invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        previewView = findViewById(R.id.preview_view)
        editText = findViewById(R.id.edit_custom_text)
        seekBarFont = findViewById(R.id.seekbar_font_size)
        textFontSize = findViewById(R.id.text_font_size)
        btnSetWallpaper = findViewById(R.id.btn_set_wallpaper)
        btnPickTextColor = findViewById(R.id.btn_text_color)
        btnPickBgColor = findViewById(R.id.btn_bg_color)

        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        textColor = prefs.getInt("text_color", Color.GREEN)
        bgColor = prefs.getInt("bg_color", Color.BLACK)
        editText.setText(prefs.getString("custom_text", "Hello, World!"))
        val fontSize = prefs.getInt("font_size", 48)
        seekBarFont.progress = fontSize
        textFontSize.text = "Font size: $fontSize"

        previewView.setColors(textColor, bgColor, fontSize, editText.text.toString())

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) { savePrefs(); previewView.updateText(s.toString()) }
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

        btnPickTextColor.setOnClickListener {
            ColorPickerDialog(this, textColor) { color ->
                textColor = color
                previewView.updateTextColor(color)
                savePrefs()
            }.show()
        }

        btnPickBgColor.setOnClickListener {
            ColorPickerDialog(this, bgColor) { color ->
                bgColor = color
                previewView.updateBgColor(color)
                savePrefs()
            }.show()
        }

        btnSetWallpaper.setOnClickListener {
            val intent = Intent(WallpaperManager.ACTION_CHANGE_LIVE_WALLPAPER)
            intent.putExtra(WallpaperManager.EXTRA_LIVE_WALLPAPER_COMPONENT,
                ComponentName(this, LiveWallpaperService::class.java))
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

    private fun savePrefs() {
        getSharedPreferences("wallpaper_prefs", MODE_PRIVATE).edit().apply {
            putInt("text_color", textColor)
            putInt("bg_color", bgColor)
            putString("custom_text", editText.text.toString())
            putInt("font_size", seekBarFont.progress)
            apply()
        }
    }
}