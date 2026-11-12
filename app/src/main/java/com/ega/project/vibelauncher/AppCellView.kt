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
 * Sel aplikasi (ikon + label) bergaya Material Design & Real Liquid Glass.
 */
class AppCellView(
    ctx: Context,
    app: AppInfo,
    cols: Int,
    ripple: Boolean,
    theme: String = Themes.MATERIAL
) : LinearLayout(ctx) {
    private val iconView: ImageView

    init {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER
        val d = resources.displayMetrics.density
        val pad = (4 * d).toInt()
        setPadding(pad, pad, pad, pad)
        val t = Themes.normalize(theme)
        val light = Themes.isLight(t)
        val glass = Themes.isGlass(t)

        if (!ripple) {
            background = null
        } else {
            val mask = GradientDrawable().apply {
                setColor(Color.WHITE)
                cornerRadius = 16 * d
            }
            val rippleColor = when {
                glass && light -> 0x2E000000
                glass -> 0x33FFFFFF
                else -> 0x333D75F3.toInt() // Material primary ripple
            }
            background = RippleDrawable(ColorStateList.valueOf(rippleColor), null, mask)
        }

        LayoutInflater.from(ctx).inflate(R.layout.item_app, this, true)
        iconView = findViewById(R.id.icon)
        val label = findViewById<TextView>(R.id.label)
        iconView.setImageDrawable(app.icon)
        label.text = app.label

        if (ripple && glass && light) {
            label.setTextColor(0xFF181A20.toInt())
            label.setShadowLayer(3f, 0f, 1f, 0x80FFFFFF.toInt())
        } else {
            label.setTextColor(Color.WHITE)
            label.setShadowLayer(3f, 0f, 1f, 0x99000000.toInt())
        }

        val sp = when {
            cols >= 7 -> 9.5f
            cols >= 6 -> 10.5f
            cols >= 5 -> 11.5f
            else -> 12.5f
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
