package com.ega.project.vibelauncher

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Ikon polos: konten ikon tidak diubah sama sekali (tanpa tile, padding, gloss, border atau efek tema).
 * Satu-satunya perlakuan: adaptive icon dipotong rounded-rect supaya bentuknya seragam.
 * Ikon legacy ditampilkan apa adanya.
 */
object IconShaper {
    private const val CORNER = 0.27f // rasio sudut rounded

    fun shape(res: Resources, src: Drawable, size: Int): Drawable {
        val s = size.toFloat()
        val content = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(content)

        if (Build.VERSION.SDK_INT >= 26 && src is AdaptiveIconDrawable) {
            // Layer adaptive 108dp digambar pada skala yang sama dengan sistem (area terlihat 72dp)
            val inset = (s * 0.25f).toInt()
            val l = -inset
            val r = size + inset
            src.background?.let { it.setBounds(l, l, r, r); it.draw(c) }
            src.foreground?.let { it.setBounds(l, l, r, r); it.draw(c) }

            val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
            val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = BitmapShader(content, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            }
            val radius = s * CORNER
            Canvas(out).drawRoundRect(RectF(0f, 0f, s, s), radius, radius, paint)
            return BitmapDrawable(res, out)
        }

        // Ikon legacy: apa adanya, tanpa latar tambahan
        src.setBounds(0, 0, size, size)
        src.draw(c)
        return BitmapDrawable(res, content)
    }
}
