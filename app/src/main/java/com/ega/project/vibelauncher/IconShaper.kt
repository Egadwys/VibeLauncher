package com.ega.project.vibelauncher

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/** Membuat semua ikon menjadi rounded-square yang seragam (sentuhan Material). */
object IconShaper {
    private const val CORNER = 0.26f // rasio radius terhadap ukuran ikon

    fun shape(res: Resources, src: Drawable, size: Int): Drawable {
        val s = size.toFloat()
        val content = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(content)

        if (Build.VERSION.SDK_INT >= 26 && src is AdaptiveIconDrawable) {
            // Layer adaptive berukuran 108dp, area terlihat 72dp -> inset 25% tiap sisi
            val inset = (s * 0.25f).toInt()
            val l = -inset; val r = size + inset
            src.background?.let { it.setBounds(l, l, r, r); it.draw(c) }
            src.foreground?.let { it.setBounds(l, l, r, r); it.draw(c) }
        } else {
            // Ikon legacy: taruh di atas tile terang agar bentuknya seragam
            c.drawColor(0xFFECEFF1.toInt())
            val pad = (s * 0.14f).toInt()
            src.setBounds(pad, pad, size - pad, size - pad)
            src.draw(c)
        }

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(content, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        Canvas(out).drawRoundRect(RectF(0f, 0f, s, s), s * CORNER, s * CORNER, paint)
        return BitmapDrawable(res, out)
    }
}
