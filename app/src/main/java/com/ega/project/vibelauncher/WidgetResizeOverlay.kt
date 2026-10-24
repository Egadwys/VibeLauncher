package com.ega.project.vibelauncher

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import kotlin.math.hypot
import kotlin.math.roundToInt

/**
 * Mode ubah ukuran widget: bingkai + 4 pegangan di tepi widget. Geser pegangan untuk mengubah
 * ukuran per sel grid; ketuk di luar pegangan untuk selesai. [item] diubah langsung.
 */
class WidgetResizeOverlay(
    ctx: Context,
    private val grid: HomeGridLayout,
    private val item: HomeItem,
    private val others: List<HomeItem>,
    private val minSx: Int,
    private val minSy: Int,
    private val canH: Boolean,
    private val canV: Boolean,
    private val onChanged: () -> Unit,
    private val onDone: () -> Unit
) : View(ctx) {

    private val d = resources.displayMetrics.density
    private val frame = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFF33B5E5.toInt(); style = Paint.Style.STROKE; strokeWidth = 2 * d
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x2233B5E5 }
    private val knob = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFF33B5E5.toInt() }
    private val knobRing = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xFFFFFFFF.toInt() }
    private val pos = IntArray(2)
    private val myPos = IntArray(2)

    private var active = -1 // 0 kiri, 1 atas, 2 kanan, 3 bawah

    private fun widgetRect(): RectF {
        grid.getLocationOnScreen(pos); getLocationOnScreen(myPos)
        val ox = (pos[0] - myPos[0]).toFloat(); val oy = (pos[1] - myPos[1]).toFloat()
        val cw = grid.cellWidth; val ch = grid.cellHeight
        val l = ox + grid.paddingLeft + item.col * cw
        val t = oy + grid.paddingTop + item.row * ch
        return RectF(l + 3 * d, t + 3 * d, l + item.spanX * cw - 3 * d, t + item.spanY * ch - 3 * d)
    }

    private fun handles(r: RectF): List<Triple<Int, Float, Float>> {
        val out = mutableListOf<Triple<Int, Float, Float>>()
        if (canH) { out.add(Triple(0, r.left, r.centerY())); out.add(Triple(2, r.right, r.centerY())) }
        if (canV) { out.add(Triple(1, r.centerX(), r.top)); out.add(Triple(3, r.centerX(), r.bottom)) }
        return out
    }

    override fun onDraw(canvas: Canvas) {
        val r = widgetRect()
        canvas.drawRoundRect(r, 12 * d, 12 * d, fill)
        canvas.drawRoundRect(r, 12 * d, 12 * d, frame)
        for ((_, x, y) in handles(r)) {
            canvas.drawCircle(x, y, 11 * d, knobRing)
            canvas.drawCircle(x, y, 8 * d, knob)
        }
    }

    override fun onTouchEvent(e: MotionEvent): Boolean {
        when (e.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                val r = widgetRect()
                active = handles(r).firstOrNull { hypot(e.x - it.second, e.y - it.third) < 30 * d }?.first ?: -1
                if (active < 0) { onDone(); return true }
                parent?.requestDisallowInterceptTouchEvent(true)
            }
            MotionEvent.ACTION_MOVE -> if (active >= 0) drag(e.x, e.y)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> active = -1
        }
        return true
    }

    private fun drag(x: Float, y: Float) {
        grid.getLocationOnScreen(pos); getLocationOnScreen(myPos)
        val gx = (x - (pos[0] - myPos[0]) - grid.paddingLeft) / grid.cellWidth
        val gy = (y - (pos[1] - myPos[1]) - grid.paddingTop) / grid.cellHeight
        var c = item.col; var r = item.row; var sx = item.spanX; var sy = item.spanY
        when (active) {
            0 -> { val right = item.col + item.spanX; c = gx.roundToInt().coerceIn(0, right - minSx); sx = right - c }
            2 -> { sx = (gx.roundToInt() - item.col).coerceIn(minSx, grid.cols - item.col) }
            1 -> { val bottom = item.row + item.spanY; r = gy.roundToInt().coerceIn(0, bottom - minSy); sy = bottom - r }
            3 -> { sy = (gy.roundToInt() - item.row).coerceIn(minSy, grid.rows - item.row) }
        }
        if (c == item.col && r == item.row && sx == item.spanX && sy == item.spanY) return
        if (!HomeLayout.isFree(others, grid.cols, grid.rows, c, r, sx, sy)) return
        item.col = c; item.row = r; item.spanX = sx; item.spanY = sy
        onChanged()
        invalidate()
    }
}
