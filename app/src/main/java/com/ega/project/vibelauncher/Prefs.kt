package com.ega.project.vibelauncher

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject

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

    var theme: String
        get() = sp.getString("theme", Themes.HOLO) ?: Themes.HOLO
        set(v) = sp.edit().putString("theme", v).apply()

    /** Dock dua baris: baris bawah berisi search bar / widget Google Search. */
    var dockTwoRows: Boolean
        get() = sp.getBoolean("dock_two_rows", false)
        set(v) = sp.edit().putBoolean("dock_two_rows", v).apply()

    /** false = search bar bawaan, true = widget Google Search. */
    var dockSearchGoogle: Boolean
        get() = sp.getBoolean("dock_search_google", false)
        set(v) = sp.edit().putBoolean("dock_search_google", v).apply()

    var dockWidgetId: Int
        get() = sp.getInt("dock_widget_id", -1)
        set(v) = sp.edit().putInt("dock_widget_id", v).apply()

    fun homeItems(): MutableList<HomeItem> {
        val out = mutableListOf<HomeItem>()
        val raw = sp.getString("home_items", null)
        if (raw == null) {
            // Migrasi dari format lama (daftar sel dipisah "|", index = row * cols + col)
            val cols = homeCols
            sp.getString("home_cells", "")!!.split("|").forEachIndexed { i, k ->
                if (k.isNotEmpty()) out.add(HomeItem(HomeItem.KIND_APP, k, i % cols, i / cols))
            }
            saveHomeItems(out)
            return out
        }
        try {
            val arr = JSONArray(raw)
            for (n in 0 until arr.length()) {
                val o = arr.getJSONObject(n)
                out.add(
                    HomeItem(
                        o.getInt("k"), o.getString("key"), o.getInt("c"), o.getInt("r"),
                        o.optInt("sx", 1), o.optInt("sy", 1)
                    )
                )
            }
        } catch (_: Exception) {
        }
        return out
    }

    fun saveHomeItems(items: List<HomeItem>) {
        val arr = JSONArray()
        for (i in items) {
            arr.put(
                JSONObject().put("k", i.kind).put("key", i.key).put("c", i.col).put("r", i.row)
                    .put("sx", i.spanX).put("sy", i.spanY)
            )
        }
        sp.edit().putString("home_items", arr.toString()).apply()
    }
}
