package com.ega.project.vibelauncher

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import androidx.core.content.ContextCompat

/** Tema: Holo (default), Liquid Glass Dark, Liquid Glass Light. */
object Themes {
    const val HOLO = "holo"
    const val GLASS_DARK = "glass_dark"
    const val GLASS_LIGHT = "glass_light"

    fun isGlass(t: String) = t == GLASS_DARK || t == GLASS_LIGHT
    fun isLight(t: String) = t == GLASS_LIGHT

    /** Warna aksen / teks utama pada panel (tab drawer, titik halaman, target drag). */
    fun accent(ctx: Context, t: String): Int = when (t) {
        GLASS_DARK -> Color.WHITE
        GLASS_LIGHT -> 0xFF1C1C1E.toInt()
        else -> ContextCompat.getColor(ctx, R.color.holo_blue)
    }

    fun dim(t: String): Int = if (t == GLASS_LIGHT) 0x44000000 else 0x55FFFFFF

    fun drawerBg(t: String, blur: Boolean): Int = when (t) {
        GLASS_DARK -> if (blur) 0x66000000 else 0xCC000000.toInt()
        GLASS_LIGHT -> if (blur) 0x66FFFFFF else 0xE6F2F2F7.toInt()
        else -> 0xEE000000.toInt()
    }

    fun dragBarBg(t: String): Int = when (t) {
        GLASS_DARK -> 0xE61C1C1E.toInt()
        GLASS_LIGHT -> 0xE6F2F2F7.toInt()
        else -> 0xE6101010.toInt()
    }

    /** Panel kaca: tint translusen + kilau gradien + tepi terang tipis. */
    fun glassPanel(ctx: Context, light: Boolean, radiusDp: Float): Drawable {
        val d = ctx.resources.displayMetrics.density
        val r = radiusDp * d
        val base = GradientDrawable().apply {
            setColor(if (light) 0x55FFFFFF else 0x55000000)
            cornerRadius = r
        }
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (light) intArrayOf(0xAAFFFFFF.toInt(), 0x22FFFFFF) else intArrayOf(0x33FFFFFF, 0x08FFFFFF)
        ).apply { cornerRadius = r }
        val edge = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = r
            setStroke((1.2f * d).toInt().coerceAtLeast(1), if (light) 0xCCFFFFFF.toInt() else 0x55FFFFFF)
        }
        return LayerDrawable(arrayOf(base, sheen, edge))
    }
}
