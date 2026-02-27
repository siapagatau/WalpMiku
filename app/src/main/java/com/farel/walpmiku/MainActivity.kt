package com.farel.walpmiku

import android.Manifest
import android.app.WallpaperManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Color
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.text.Editable
import android.text.TextWatcher
import android.widget.*
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity

class MainActivity : AppCompatActivity() {

    private lateinit var previewView: PreviewView
    private lateinit var editText: EditText
    private lateinit var seekBarFont: SeekBar
    private lateinit var textFontSize: TextView
    private lateinit var btnSetWallpaper: Button
    private lateinit var btnPickTextColor: Button
    private lateinit var btnPickBgColor: Button
    private lateinit var btnPickFromGallery: Button

    private var textColor = Color.GREEN
    private var bgColor = Color.BLACK
    private var selectedImageUri: Uri? = null

    private val handler = Handler(Looper.getMainLooper())
    private val updateTimeRunnable = object : Runnable {
        override fun run() {
            previewView.invalidate()
            handler.postDelayed(this, 1000)
        }
    }

    // Launcher untuk memilih gambar dari galeri
    private val pickImageLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedImageUri = uri
            previewView.setBackgroundImage(selectedImageUri)
            savePrefs()
        }
    }

    // Launcher untuk meminta izin penyimpanan
    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            pickImageLauncher.launch("image/*")
        } else {
            Toast.makeText(this, "Izin diperlukan untuk memilih gambar", Toast.LENGTH_SHORT).show()
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
        btnPickFromGallery = findViewById(R.id.btn_pick_from_gallery)

        val prefs = getSharedPreferences("wallpaper_prefs", MODE_PRIVATE)
        textColor = prefs.getInt("text_color", Color.GREEN)
        bgColor = prefs.getInt("bg_color", Color.BLACK)
        editText.setText(prefs.getString("custom_text", "Hello, World!"))
        val fontSize = prefs.getInt("font_size", 48)
        seekBarFont.progress = fontSize
        textFontSize.text = "Font size: $fontSize"
        val uriString = prefs.getString("background_image_uri", null)
        selectedImageUri = if (uriString != null) Uri.parse(uriString) else null

        previewView.setColors(textColor, bgColor, fontSize, editText.text.toString())
        previewView.setBackgroundImage(selectedImageUri)

        editText.addTextChangedListener(object : TextWatcher {
            override fun afterTextChanged(s: Editable?) {
                savePrefs()
                previewView.updateText(s.toString())
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

        btnPickFromGallery.setOnClickListener {
            val permission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                Manifest.permission.READ_MEDIA_IMAGES
            } else {
                Manifest.permission.READ_EXTERNAL_STORAGE
            }

            if (checkSelfPermission(permission) == PackageManager.PERMISSION_GRANTED) {
                pickImageLauncher.launch("image/*")
            } else {
                requestPermissionLauncher.launch(permission)
            }
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
            putString("background_image_uri", selectedImageUri?.toString())
            apply()
        }
    }
}
