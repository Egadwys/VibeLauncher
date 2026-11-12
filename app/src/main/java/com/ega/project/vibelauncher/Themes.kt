package com.ega.project.vibelauncher

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Outline
import android.graphics.RenderEffect
import android.graphics.Shader
import android.graphics.drawable.Drawable
import android.graphics.drawable.GradientDrawable
import android.graphics.drawable.LayerDrawable
import android.graphics.drawable.RippleDrawable
import android.os.Build
import android.view.View
import android.view.ViewOutlineProvider
import android.view.WindowManager
import androidx.core.content.ContextCompat
import eightbitlab.com.blurview.BlurView

/**
 * Visual Design Engine: Material Design & Real Liquid Glass.
 */
object Themes {
    const val MATERIAL = "material"
    const val GLASS_DARK = "glass_dark"
    const val GLASS_LIGHT = "glass_light"
    const val HOLO = "holo" // legacy alias mapped to MATERIAL

    /** Radius blur BlurView (<= 25 supaya aman di jalur RenderScript API < 31). */
    const val GLASS_BLUR_RADIUS = 22f
    const val GLASS_BLUR_RADIUS_DRAWER = 25f

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

    /**
     * Styling sebuah [BlurView] sebagai panel tema.
     * - Glass + sumber blur siap  : blur aktif, panel kaca translucent di atasnya.
     * - Selain itu (Material, atau wallpaper tidak bisa dibaca): blur mati, panel biasa.
     * Sudut dibulatkan lewat outline + clip supaya blur tidak bocor keluar capsule.
     */
    fun styleBlurPanel(view: BlurView, theme: String, radiusDp: Float, blurOn: Boolean, blurRadius: Float = GLASS_BLUR_RADIUS) {
        val ctx = view.context
        val r = radiusDp * ctx.resources.displayMetrics.density
        view.background = panel(ctx, theme, radiusDp)
        view.outlineProvider = object : ViewOutlineProvider() {
            override fun getOutline(v: View, outline: Outline) {
                outline.setRoundRect(0, 0, v.width, v.height, r)
            }
        }
        view.clipToOutline = true
        view.setBlurEnabled(blurOn)
        if (blurOn) view.setBlurRadius(blurRadius)
    }

    // ------------------------------------------------------------------
    // Token tema untuk semua layar (Settings, dialog, dsb). Satu sumber warna per tema.
    // ------------------------------------------------------------------

    /** Warna teks utama. */
    fun textPrimary(ctx: Context, t: String): Int = when (normalize(t)) {
        GLASS_DARK -> Color.WHITE
        GLASS_LIGHT -> 0xFF181A20.toInt()
        else -> ContextCompat.getColor(ctx, R.color.md_theme_onSurface)
    }

    /** Teks di atas warna aksen (mis. segmen terpilih, thumb switch aktif). */
    fun onAccent(t: String): Int = when (normalize(t)) {
        GLASS_DARK -> 0xFF111318.toInt()
        else -> Color.WHITE
    }

    /** Warna ripple sentuhan. */
    fun ripple(t: String): Int = when (normalize(t)) {
        GLASS_DARK -> 0x33FFFFFF
        GLASS_LIGHT -> 0x2E000000
        else -> 0x333D75F3
    }

    /** Garis pemisah tipis. */
    fun divider(t: String): Int = if (isLight(t)) 0x1A000000 else 0x1AFFFFFF

    /** Track slider / switch yang tidak aktif. */
    fun trackInactive(t: String): Int = if (isLight(t)) 0x26000000 else 0x33FFFFFF

    /** Latar tile ikon & chip. */
    fun tileBg(ctx: Context, t: String): Int = when (normalize(t)) {
        GLASS_DARK -> 0x26FFFFFF
        GLASS_LIGHT -> 0x14000000
        else -> ContextCompat.getColor(ctx, R.color.md_theme_primaryContainer)
    }

    /** Warna ikon di atas [tileBg]. */
    fun tileFg(ctx: Context, t: String): Int = when (normalize(t)) {
        GLASS_DARK -> Color.WHITE
        GLASS_LIGHT -> 0xFF181A20.toInt()
        else -> ContextCompat.getColor(ctx, R.color.md_theme_onPrimaryContainer)
    }

    /**
     * Latar layar Settings. Material: solid. Glass: scrim tipis di atas wallpaper/home yang di-blur window;
     * kalau blur window tidak tersedia scrim dibuat lebih pekat supaya teks tetap terbaca.
     */
    fun settingsScrim(t: String, windowBlur: Boolean): Int = when (normalize(t)) {
        GLASS_DARK -> if (windowBlur) 0x8C0A0C12.toInt() else 0xEB0A0C12.toInt()
        GLASS_LIGHT -> if (windowBlur) 0x99F2F5FA.toInt() else 0xF2F2F5FA.toInt()
        else -> 0xFF111318.toInt()
    }

    /** Kartu section di Settings. */
    fun settingsCard(ctx: Context, theme: String, radiusDp: Float): Drawable {
        val t = normalize(theme)
        val d = ctx.resources.displayMetrics.density
        val r = radiusDp * d
        if (!isGlass(t)) {
            return GradientDrawable().apply {
                setColor(ContextCompat.getColor(ctx, R.color.md_theme_surface_variant))
                cornerRadius = r
                setStroke((1 * d).toInt().coerceAtLeast(1), 0x1FFFFFFF)
            }
        }
        val light = isLight(t)
        val base = GradientDrawable().apply {
            setColor(if (light) 0xB8FFFFFF.toInt() else 0x8014182A.toInt())
            cornerRadius = r
        }
        val sheen = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM,
            if (light) intArrayOf(0x66FFFFFF, 0x00FFFFFF) else intArrayOf(0x22FFFFFF, 0x00FFFFFF)
        ).apply { cornerRadius = r }
        val rim = GradientDrawable().apply {
            setColor(Color.TRANSPARENT)
            cornerRadius = r
            setStroke((1.2f * d).toInt().coerceAtLeast(1), if (light) 0xCCFFFFFF.toInt() else 0x40FFFFFF)
        }
        return LayerDrawable(arrayOf(base, sheen, rim))
    }

    /** Latar ripple dengan sudut rounded (tombol ikon, baris yang bisa diketuk). */
    fun rippleBg(theme: String, radiusDp: Float, density: Float): Drawable {
        val mask = GradientDrawable().apply {
            setColor(Color.WHITE)
            cornerRadius = radiusDp * density
        }
        return RippleDrawable(ColorStateList.valueOf(ripple(theme)), null, mask)
    }

    /** true kalau blur window (FLAG_BLUR_BEHIND) benar-benar aktif di device ini. */
    fun crossWindowBlur(ctx: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
            try {
                (ctx.getSystemService(Context.WINDOW_SERVICE) as WindowManager).isCrossWindowBlurEnabled
            } catch (_: Throwable) { false }

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
