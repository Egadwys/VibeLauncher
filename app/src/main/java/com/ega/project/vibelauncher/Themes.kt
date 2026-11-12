package com.ega.project.vibelauncher

import android.content.Context
import android.graphics.Color
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.os.Build
import android.view.View
import androidx.core.content.ContextCompat

/**
 * Visual Design Engine: Material Design & Real Liquid Glass.
 */
object Themes {
    const val MATERIAL = "material"
    const val GLASS_DARK = "glass_dark"
    const val GLASS_LIGHT = "glass_light"
    const val HOLO = "holo" // legacy alias mapped to MATERIAL

    fun normalize(t: String): String = if (t == HOLO) MATERIAL else t
    fun isGlass(t: String): Boolean = t == GLASS_DARK || t == GLASS_LIGHT
    fun isLight(t: String): Boolean = t == GLASS_LIGHT

    /** Warna aksen utama (Material Primary / Glass Frost White / Glass Obsidian). */
    fun accent(ctx: Context, t: String): Int = when (normalize(t)) {
        GLASS_DARK -> Color.WHITE
        GLASS_LIGHT -> 0xFF181A20.toInt()
        else -> ContextCompat.getColor(ctx, R.color.md_theme_primary)
    }

    /** Warna teks sekunder / muted. */
    fun secondaryText(ctx: Context, t: String): Int = when (normalize(t)) {
        GLASS_DARK -> 0xB3FFFFFF.toInt()
        GLASS_LIGHT -> 0x99181A20.toInt()
        else -> ContextCompat.getColor(ctx, R.color.md_theme_onSurfaceVariant)
    }

    /** Warna titik indikator / elemen redup. */
    fun dim(t: String): Int = if (isLight(t)) 0x33000000 else 0x38FFFFFF

    /** Background app drawer: transparan frosted untuk liquid glass, surface solid-translucent untuk Material. */
    fun drawerBg(t: String, blur: Boolean): Int = when (normalize(t)) {
        GLASS_DARK -> if (blur) 0x380F1219 else 0xCC11141C.toInt()
        GLASS_LIGHT -> if (blur) 0x4DFFFFFF else 0xE6F2F5F9.toInt()
        else -> 0xF412141A.toInt() // Material 3 dark surface
    }

    /** Background bar target drag (floating capsule). */
    fun dragBarBg(t: String): Int = when (normalize(t)) {
        GLASS_DARK -> 0xCC141720.toInt()
        GLASS_LIGHT -> 0xCCF4F6FB.toInt()
        else -> 0xF21C2028.toInt()
    }

    /** Panel Material 3: rounded elevation + subtle tonal outline. */
    fun materialPanel(ctx: Context, radiusDp: Float): Drawable {
        val d = ctx.resources.displayMetrics.density
        return GradientDrawable().apply {
            setColor(0xF0181B23.toInt())
            cornerRadius = radiusDp * d
            setStroke((1 * d).toInt().coerceAtLeast(1), 0x2EFFFFFF)
        }
    }

    /**
     * Real Liquid Glass Panel:
     * - Layer 0: Frosted translucent glass body
     * - Layer 1: Specular top curve gloss sheen
     * - Layer 2: Light refraction bevel border
     */
    fun liquidGlassPanel(ctx: Context, light: Boolean, radiusDp: Float, strokeWidthDp: Float = 1.2f): Drawable {
        val d = ctx.resources.displayMetrics.density
        val r = radiusDp * d

        // Base frosted body
        val base = GradientDrawable().apply {
            setColor(if (light) 0x59FFFFFF else 0x3D13161F)
            cornerRadius = r
        }

        // Specular highlight gloss (light reflecting on curved glass surface)
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (light) intArrayOf(0x99FFFFFF.toInt(), 0x1AFFFFFF)
            else intArrayOf(0x40FFFFFF, 0x05FFFFFF)
        ).apply {
            cornerRadius = r
        }

        // Refraction edge rim
        val rim = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = r
            setStroke(
                (strokeWidthDp * d).toInt().coerceAtLeast(1),
                if (light) 0x99FFFFFF.toInt() else 0x4DFFFFFF
            )
        }

        return LayerDrawable(arrayOf(base, sheen, rim))
    }

    /** Panel serbaguna (dock, search pill, cards) mengikuti tema. */
    fun panel(ctx: Context, theme: String, radiusDp: Float): Drawable {
        val t = normalize(theme)
        return if (isGlass(t)) liquidGlassPanel(ctx, isLight(t), radiusDp)
        else materialPanel(ctx, radiusDp)
    }

    /** Terapkan blur hardware pada Android 12+ untuk Real Liquid Glass. */
    fun applyBlurIfSupported(view: View, blurRadius: Float = 32f) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(RenderEffect.createBlurEffect(blurRadius, blurRadius, Shader.TileMode.CLAMP))
            } catch (_: Throwable) {}
        }
    }

    /** Bersihkan efek blur. */
    fun clearBlur(view: View) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                view.setRenderEffect(null)
            } catch (_: Throwable) {}
        }
    }
}
