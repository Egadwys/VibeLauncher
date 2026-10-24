package com.ega.project.vibelauncher

import android.content.Context

class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("holo_launcher", Context.MODE_PRIVATE)

    var homeCols: Int
        get() = sp.getInt("home_cols", 4)
        set(v) = sp.edit().putInt("home_cols", v).apply()

    var homeRows: Int
        get() = sp.getInt("home_rows", 5)
        set(v) = sp.edit().putInt("home_rows", v).apply()

    var drawerCols: Int
        get() = sp.getInt("drawer_cols", 4)
        set(v) = sp.edit().putInt("drawer_cols", v).apply()

    /** Isi sel home screen (index = posisi sel, "" = kosong). Ukuran selalu == count. */
    fun homeCells(count: Int): MutableList<String> {
        val raw = sp.getString("home_cells", "")!!.split("|").toMutableList()
        if (raw.size > count) {
            // Grid mengecil: pindahkan item yang "tumpah" ke sel kosong yang tersisa
            val overflow = raw.subList(count, raw.size).filter { it.isNotEmpty() }
            val head = raw.subList(0, count).toMutableList()
            for (k in overflow) {
                val i = head.indexOf("")
                if (i >= 0) head[i] = k
            }
            return head
        }
        while (raw.size < count) raw.add("")
        return raw
    }

    fun saveHomeCells(cells: List<String>) {
        sp.edit().putString("home_cells", cells.joinToString("|")).apply()
    }
}
