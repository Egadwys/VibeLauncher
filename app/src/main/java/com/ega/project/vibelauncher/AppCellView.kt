package com.ega.project.vibelauncher

import android.content.Context
import android.util.TypedValue
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView

/**
 * Satu sel aplikasi (ikon + label). ripple = false untuk home screen (tanpa efek sentuhan kotak),
 * true untuk app drawer.
 */
class AppCellView(ctx: Context, app: AppInfo, cols: Int, ripple: Boolean) : LinearLayout(ctx) {
    private val iconView: ImageView

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val pad = (4 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        if (ripple) setBackgroundResource(R.drawable.bg_item) else background = null

        LayoutInflater.from(ctx).inflate(R.layout.item_app, this, true)
        iconView = findViewById(R.id.icon)
        val label = findViewById<TextView>(R.id.label)
        iconView.setImageDrawable(app.icon)
        label.text = app.label
        val sp = when {
            cols >= 7 -> 9f
            cols >= 6 -> 10f
            cols >= 5 -> 11f
            else -> 12f
        }
        label.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)
    }

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        val size = (minOf(w, h) * 0.58f).toInt()
        if (size > 0 && iconView.layoutParams.width != size) {
            iconView.layoutParams.width = size
            iconView.layoutParams.height = size
        }
        super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }
}
