package com.ega.project.vibelauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout

/**
 * Grid home screen: tiap child punya HomeItem di `tag` (kolom, baris, span).
 * Menggambar highlight area tujuan saat drag.
 */
class HomeGridLayout @JvmOverloads constructor(ctx: Context, attrs: AttributeSet? = null) :
    FrameLayout(ctx, attrs) {

    var cols = 4
    var rows = 5

    private var hl: RectF? = null
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x3333B5E5 }
    private val stroke = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0x9933B5E5.toInt(); style = Paint.Style.STROKE
        strokeWidth = 2 * resources.displayMetrics.density
    }

    val cellWidth: Float get() = (width - paddingLeft - paddingRight) / cols.toFloat()
    val cellHeight: Float get() = (height - paddingTop - paddingBottom) / rows.toFloat()

    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val w = MeasureSpec.getSize(widthMeasureSpec)
        val h = MeasureSpec.getSize(heightMeasureSpec)
        setMeasuredDimension(w, h)
        val cw = (w - paddingLeft - paddingRight) / cols
        val ch = (h - paddingTop - paddingBottom) / rows
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val item = c.tag as? HomeItem ?: continue
            c.measure(
                MeasureSpec.makeMeasureSpec(cw * item.spanX, MeasureSpec.EXACTLY),
                MeasureSpec.makeMeasureSpec(ch * item.spanY, MeasureSpec.EXACTLY)
            )
        }
    }

    override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
        val cw = (width - paddingLeft - paddingRight) / cols
        val ch = (height - paddingTop - paddingBottom) / rows
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val item = c.tag as? HomeItem ?: continue
            val left = paddingLeft + item.col * cw
            val top = paddingTop + item.row * ch
            c.layout(left, top, left + cw * item.spanX, top + ch * item.spanY)
        }
    }

    /** Sel kiri-atas tujuan drop untuk objek berukuran sx x sy, dengan jari di tengahnya. */
    fun targetCell(x: Float, y: Float, sx: Int, sy: Int): IntArray {
        val c = ((x - paddingLeft) / cellWidth).toInt() - (sx - 1) / 2
        val r = ((y - paddingTop) / cellHeight).toInt() - (sy - 1) / 2
        return intArrayOf(c.coerceIn(0, maxOf(0, cols - sx)), r.coerceIn(0, maxOf(0, rows - sy)))
    }

    fun showHighlight(col: Int, row: Int, sx: Int, sy: Int) {
        val cw = cellWidth; val ch = cellHeight
        hl = RectF(
            paddingLeft + col * cw + 4, paddingTop + row * ch + 4,
            paddingLeft + (col + sx) * cw - 4, paddingTop + (row + sy) * ch - 4
        )
        invalidate()
    }

    fun clearHighlight() {
        if (hl != null) { hl = null; invalidate() }
    }

    override fun dispatchDraw(canvas: Canvas) {
        super.dispatchDraw(canvas)
        hl?.let {
            val rad = 14 * resources.displayMetrics.density
            canvas.drawRoundRect(it, rad, rad, fill)
            canvas.drawRoundRect(it, rad, rad, stroke)
        }
    }

    fun viewFor(item: HomeItem): View? {
        for (i in 0 until childCount) {
            val c = getChildAt(i)
            val t = c.tag as? HomeItem ?: continue
            if (t.kind == item.kind && t.key == item.key) return c
        }
        return null
    }
}
