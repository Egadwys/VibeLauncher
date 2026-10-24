package com.ega.project.vibelauncher

/** Satu objek di home screen: aplikasi (1x1) atau widget (span bebas). Posisi dalam satuan sel. */
data class HomeItem(
    val kind: Int,
    val key: String, // app: ComponentName.flattenToString(), widget: appWidgetId
    var col: Int,
    var row: Int,
    var spanX: Int = 1,
    var spanY: Int = 1
) {
    companion object {
        const val KIND_APP = 0
        const val KIND_WIDGET = 1
    }
}

object HomeLayout {
    fun overlaps(i: HomeItem, c: Int, r: Int, sx: Int, sy: Int) =
        !(c + sx <= i.col || i.col + i.spanX <= c || r + sy <= i.row || i.row + i.spanY <= r)

    fun isFree(items: List<HomeItem>, cols: Int, rows: Int, c: Int, r: Int, sx: Int, sy: Int): Boolean {
        if (c < 0 || r < 0 || c + sx > cols || r + sy > rows) return false
        return items.none { overlaps(it, c, r, sx, sy) }
    }

    /** Area kosong terdekat dari (nearC, nearR), atau null kalau penuh. */
    fun findNearest(
        items: List<HomeItem>, cols: Int, rows: Int, sx: Int, sy: Int, nearC: Int, nearR: Int
    ): IntArray? {
        var best: IntArray? = null
        var bestD = Int.MAX_VALUE
        for (r in 0..(rows - sy)) for (c in 0..(cols - sx)) {
            if (!isFree(items, cols, rows, c, r, sx, sy)) continue
            val d = (c - nearC) * (c - nearC) + (r - nearR) * (r - nearR)
            if (d < bestD) { bestD = d; best = intArrayOf(c, r) }
        }
        return best
    }

    class FitResult(val changed: Boolean, val dropped: List<HomeItem>)

    /** Pastikan semua item muat di grid cols x rows (dipakai saat grid dikecilkan). */
    fun fit(items: MutableList<HomeItem>, cols: Int, rows: Int): FitResult {
        var changed = false
        val result = mutableListOf<HomeItem>()
        val dropped = mutableListOf<HomeItem>()
        for (item in items.sortedWith(compareBy({ it.row }, { it.col }))) {
            val sx = item.spanX.coerceIn(1, cols)
            val sy = item.spanY.coerceIn(1, rows)
            var c = item.col.coerceIn(0, cols - sx)
            var r = item.row.coerceIn(0, rows - sy)
            if (!isFree(result, cols, rows, c, r, sx, sy)) {
                val p = findNearest(result, cols, rows, sx, sy, c, r)
                if (p == null) { dropped.add(item); changed = true; continue }
                c = p[0]; r = p[1]
            }
            if (c != item.col || r != item.row || sx != item.spanX || sy != item.spanY) changed = true
            item.col = c; item.row = r; item.spanX = sx; item.spanY = sy
            result.add(item)
        }
        items.clear()
        items.addAll(result)
        return FitResult(changed, dropped)
    }
}
