package com.ega.project.vibelauncher

import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import androidx.recyclerview.widget.RecyclerView

/** Satu halaman = satu grid aplikasi; geser ke samping antar halaman (ala Jelly Bean). */
class DrawerPagerAdapter(
    private val pages: List<List<AppInfo>>,
    private val cols: Int,
    private val cellW: Int,
    private val cellH: Int,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (View, AppInfo) -> Unit
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
            val v = AppCellView(h.page.context, app, cols, true)
            val lp = FrameLayout.LayoutParams(cellW, cellH)
            lp.leftMargin = (i % cols) * cellW
            lp.topMargin = (i / cols) * cellH
            v.layoutParams = lp
            v.setOnClickListener { onClick(app) }
            v.setOnLongClickListener { onLongClick(it, app); true }
            h.page.addView(v)
        }
    }

    override fun getItemCount() = pages.size
}
