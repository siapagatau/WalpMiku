package com.farel.walpmiku

import android.app.AlertDialog
import android.content.Context
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.*

class ColorPickerDialog(
    context: Context,
    private val currentColor: Int,
    private val onColorSelected: (Int) -> Unit
) : AlertDialog.Builder(context) {

    private val colors = listOf(
        Color.BLACK, Color.WHITE, Color.RED, Color.GREEN, Color.BLUE,
        Color.CYAN, Color.MAGENTA, Color.YELLOW, Color.GRAY, Color.DKGRAY,
        Color.parseColor("#FF5722"), Color.parseColor("#9C27B0"), Color.parseColor("#3F51B5"),
        Color.parseColor("#009688"), Color.parseColor("#FFC107"), Color.parseColor("#795548")
    )

    init {
        setTitle("Choose Color")
        val gridView = GridView(context).apply {
            numColumns = 4
            adapter = ColorAdapter(context, colors, currentColor)
            onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
                onColorSelected(colors[position])
                dismiss()
            }
        }
        setView(gridView)
        setNegativeButton("Cancel", null)
    }

    fun show() = create().show()

    private class ColorAdapter(
        context: Context,
        private val colors: List<Int>,
        private val currentColor: Int
    ) : BaseAdapter() {

        private val inflater = LayoutInflater.from(context)

        override fun getCount(): Int = colors.size
        override fun getItem(position: Int): Any = colors[position]
        override fun getItemId(position: Int): Long = position.toLong()

        override fun getView(position: Int, convertView: View?, parent: ViewGroup?): View {
            val view = convertView ?: inflater.inflate(R.layout.item_color, parent, false)
            val colorView = view.findViewById<View>(R.id.color_view)
            val color = colors[position]
            colorView.setBackgroundColor(color)
            if (color == currentColor) {
                colorView.background = context.getDrawable(R.drawable.color_selected_border)
            } else {
                colorView.background = null
            }
            return view
        }
    }
}