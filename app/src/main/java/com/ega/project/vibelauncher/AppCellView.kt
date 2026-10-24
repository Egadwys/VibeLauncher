package com.ega.project.vibelauncher

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.RippleDrawable
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
class AppCellView(
    ctx: Context, app: AppInfo, cols: Int, ripple: Boolean, theme: String = Themes.HOLO
) : LinearLayout(ctx) {
    private val iconView: ImageView

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val pad = (4 * resources.displayMetrics.density).toInt()
        setPadding(pad, pad, pad, pad)
        val light = Themes.isLight(theme)
        if (!ripple) background = null
        else if (Themes.isGlass(theme)) {
            val mask = GradientDrawable().apply { setColor(Color.WHITE); cornerRadius = 14 * resources.displayMetrics.density }
            background = RippleDrawable(ColorStateList.valueOf(if (light) 0x33000000 else 0x33FFFFFF), null, mask)
        } else setBackgroundResource(R.drawable.bg_item)

        LayoutInflater.from(ctx).inflate(R.layout.item_app, this, true)
        iconView = findViewById(R.id.icon)
        val label = findViewById<TextView>(R.id.label)
        iconView.setImageDrawable(app.icon)
        label.text = app.label
        if (ripple && light) { // drawer terang: label gelap
            label.setTextColor(0xFF1C1C1E.toInt())
            label.setShadowLayer(3f, 0f, 1f, 0x99FFFFFF.toInt())
        }
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
