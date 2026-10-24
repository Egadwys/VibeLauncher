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
        get() = sp.getBoolean("dock_two_rows", true) // default: dock + search bar
        set(v) = sp.edit().putBoolean("dock_two_rows", v).apply()

    /** false = search bar bawaan, true = widget Google Search. */
    var dockSearchGoogle: Boolean
        get() = sp.getBoolean("dock_search_google", false)
        set(v) = sp.edit().putBoolean("dock_search_google", v).apply()

    var dockWidgetId: Int
        get() = sp.getInt("dock_widget_id", -1)
        set(v) = sp.edit().putInt("dock_widget_id", v).apply()

    /** Prompt "jadikan launcher default" sudah pernah ditampilkan. */
    var defaultPrompted: Boolean
        get() = sp.getBoolean("default_prompted", false)
        set(v) = sp.edit().putBoolean("default_prompted", v).apply()

    var haptics: Boolean
        get() = sp.getBoolean("haptics", true)
        set(v) = sp.edit().putBoolean("haptics", v).apply()

    /** true kalau dock belum pernah diisi / disimpan (dipakai untuk mengisi default sekali). */
    val dockUnset: Boolean get() = sp.getString("dock_apps", null) == null

    /** 4 slot dock (kiri 2, kanan 2; tombol drawer di tengah). "" = slot kosong. */
    fun dockApps(): MutableList<String> {
        val out = MutableList(DOCK_SLOTS) { "" }
        val raw = sp.getString("dock_apps", null) ?: return out
        try {
            val arr = JSONArray(raw)
            for (i in 0 until minOf(arr.length(), DOCK_SLOTS)) out[i] = arr.optString(i, "")
        } catch (_: Exception) {
        }
        return out
    }

    fun saveDockApps(list: List<String>) {
        val arr = JSONArray()
        for (i in 0 until DOCK_SLOTS) arr.put(list.getOrElse(i) { "" })
        sp.edit().putString("dock_apps", arr.toString()).apply()
    }

    companion object {
        const val DOCK_SLOTS = 4
    }

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
