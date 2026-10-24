package com.ega.project.vibelauncher

import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView
import kotlin.math.abs

/** Satu halaman = satu grid aplikasi; geser ke samping antar halaman (ala Jelly Bean). */
class DrawerPagerAdapter(
    private val pages: List<List<AppInfo>>,
    private val cols: Int,
    private val cellW: Int,
    private val cellH: Int,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (View, AppInfo) -> Unit,
    private val onDragStart: (View, AppInfo) -> Unit,
    private val theme: String
) : RecyclerView.Adapter<DrawerPagerAdapter.VH>() {

    class VH(val page: FrameLayout) : RecyclerView.ViewHolder(page)

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val f = FrameLayout(parent.context)
        f.layoutParams = ViewGroup.LayoutParams(
            ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
        )
        return VH(f)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        h.page.removeAllViews()
        pages[position].forEachIndexed { i, app ->
            val v = AppCellView(h.page.context, app, cols, true, theme)
            val lp = FrameLayout.LayoutParams(cellW, cellH)
            lp.leftMargin = (i % cols) * cellW
            lp.topMargin = (i / cols) * cellH
            v.layoutParams = lp
            v.setOnClickListener { onClick(app) }
            // Tekan lama -> tooltip menu; kalau jari lanjut digeser -> menu hilang & drag ke home dimulai
            val slop = ViewConfiguration.get(v.context).scaledTouchSlop * 2
            var downX = 0f
            var downY = 0f
            var armed = false
            v.setOnLongClickListener {
                armed = true
                it.parent?.requestDisallowInterceptTouchEvent(true)
                onLongClick(it, app)
                true
            }
            v.setOnTouchListener { view, e ->
                when (e.actionMasked) {
                    MotionEvent.ACTION_DOWN -> { downX = e.x; downY = e.y; armed = false; false }
                    MotionEvent.ACTION_MOVE ->
                        if (armed && (abs(e.x - downX) > slop || abs(e.y - downY) > slop)) {
                            armed = false
                            onDragStart(view, app)
                            true
                        } else false
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> { armed = false; false }
                    else -> false
                }
            }
            h.page.addView(v)
        }
    }

    override fun getItemCount() = pages.size
}
