package com.ega.project.vibelauncher

import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView

/**
 * Dipakai untuk home screen (rows != null -> tinggi sel = tinggi RecyclerView / rows,
 * item null = sel kosong) dan app drawer (rows == null -> tinggi mengikuti lebar sel).
 */
class AppGridAdapter(
    private val items: List<AppInfo?>,
    private val cols: Int,
    private val rows: Int?,
    private val onClick: (AppInfo) -> Unit,
    private val onLongClick: (View, AppInfo, Int) -> Unit,
    private val onEmptyLongClick: () -> Unit = {}
) : RecyclerView.Adapter<AppGridAdapter.VH>() {

    class VH(v: View) : RecyclerView.ViewHolder(v) {
        val icon: ImageView = v.findViewById(R.id.icon)
        val label: TextView = v.findViewById(R.id.label)
    }

    private var cellW = 0
    private var cellH = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
        val v = LayoutInflater.from(parent.context).inflate(R.layout.item_app, parent, false)
        val w = parent.width - parent.paddingLeft - parent.paddingRight
        cellW = (if (w > 0) w else parent.resources.displayMetrics.widthPixels) / cols
        val h = parent.height - parent.paddingTop - parent.paddingBottom
        cellH = if (rows != null && h > 0) h / rows else (cellW * 1.15f).toInt()
        v.layoutParams = RecyclerView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, cellH)
        return VH(v)
    }

    override fun onBindViewHolder(h: VH, position: Int) {
        val size = (minOf(cellW, cellH) * 0.58f).toInt()
        h.icon.layoutParams = h.icon.layoutParams.apply { width = size; height = size }
        val sp = when {
            cols >= 7 -> 9f
            cols >= 6 -> 10f
            cols >= 5 -> 11f
            else -> 12f
        }
        h.label.setTextSize(TypedValue.COMPLEX_UNIT_SP, sp)

        val app = items[position]
        if (app == null) {
            h.icon.setImageDrawable(null)
            h.label.text = ""
            h.itemView.setOnClickListener(null)
            h.itemView.isClickable = false
            h.itemView.setOnLongClickListener { onEmptyLongClick(); true }
        } else {
            h.icon.setImageDrawable(app.icon)
            h.label.text = app.label
            h.itemView.setOnClickListener { onClick(app) }
            h.itemView.setOnLongClickListener { v ->
                onLongClick(v, app, h.bindingAdapterPosition); true
            }
        }
    }

    override fun getItemCount() = items.size
}
