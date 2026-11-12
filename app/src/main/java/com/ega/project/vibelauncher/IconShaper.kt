package com.ega.project.vibelauncher

import android.content.res.Resources
import android.graphics.*
import android.graphics.drawable.AdaptiveIconDrawable
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build

/**
 * Membentuk ikon menjadi Material squircle seragam dengan efek Real Liquid Glass opsional.
 */
object IconShaper {
    private const val CORNER = 0.27f // Rasio squircle modern

    fun shape(res: Resources, src: Drawable, size: Int, glass: Boolean = false): Drawable {
        val s = size.toFloat()
        val content = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val c = Canvas(content)

        if (Build.VERSION.SDK_INT >= 26 && src is AdaptiveIconDrawable) {
            // Adaptive icon: inset 25% agar area utama tepat di dalam squircle
            val inset = (s * 0.25f).toInt()
            val l = -inset
            val r = size + inset
            src.background?.let { it.setBounds(l, l, r, r); it.draw(c) }
            src.foreground?.let { it.setBounds(l, l, r, r); it.draw(c) }
        } else {
            // Legacy icon: diletakkan di atas tile surface modern
            c.drawColor(0xFF222630.toInt())
            val pad = (s * 0.15f).toInt()
            src.setBounds(pad, pad, size - pad, size - pad)
            src.draw(c)
        }

        val out = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888)
        val paint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            shader = BitmapShader(content, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
        }
        val r = s * CORNER
        val canvasOut = Canvas(out)
        canvasOut.drawRoundRect(RectF(0f, 0f, s, s), r, r, paint)

        if (glass) {
            // Real Liquid Glass: kilau specular miring dari sudut atas-kiri + border bevel kaca
            val gloss = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                shader = LinearGradient(
                    0f, 0f, s * 0.7f, s * 0.7f,
                    intArrayOf(0x55FFFFFF, 0x1AFFFFFF, 0x00FFFFFF),
                    floatArrayOf(0f, 0.45f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            canvasOut.drawRoundRect(RectF(0f, 0f, s, s), r, r, gloss)

            // Bevel rim kaca
            val edge = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                style = Paint.Style.STROKE
                strokeWidth = s * 0.035f
                shader = LinearGradient(
                    0f, 0f, s, s,
                    intArrayOf(0x80FFFFFF.toInt(), 0x30FFFFFF, 0x10FFFFFF),
                    floatArrayOf(0f, 0.5f, 1f),
                    Shader.TileMode.CLAMP
                )
            }
            val inset = edge.strokeWidth / 2f
            canvasOut.drawRoundRect(RectF(inset, inset, s - inset, s - inset), r, r, edge)
        }

        return BitmapDrawable(res, out)
    }
}
