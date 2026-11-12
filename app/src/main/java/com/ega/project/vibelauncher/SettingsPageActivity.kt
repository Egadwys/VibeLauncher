package com.ega.project.vibelauncher

import android.content.Context
import android.content.Intent
import android.view.View
import android.widget.LinearLayout

/** Satu halaman pengaturan per section: Tampilan, Layar & laci, Dock, Interaksi. */
class SettingsPageActivity : BaseSettingsActivity() {
    companion object {
        const val EXTRA_PAGE = "page"
        const val PAGE_APPEARANCE = "appearance"
        const val PAGE_LAYOUT = "layout"
        const val PAGE_DOCK = "dock"
        const val PAGE_INTERACTION = "interaction"

        fun intent(ctx: Context, page: String) =
            Intent(ctx, SettingsPageActivity::class.java).putExtra(EXTRA_PAGE, page)
    }

    private val page: String get() = intent.getStringExtra(EXTRA_PAGE) ?: PAGE_APPEARANCE
    private var accessBox: LinearLayout? = null

    override val titleRes: Int get() = when (page) {
        PAGE_LAYOUT -> R.string.layout_title
        PAGE_DOCK -> R.string.section_dock
        PAGE_INTERACTION -> R.string.interaction
        else -> R.string.appearance
    }

    override fun build(content: LinearLayout) {
        when (page) {
            PAGE_LAYOUT -> buildLayout(content)
            PAGE_DOCK -> buildDock(content)
            PAGE_INTERACTION -> buildInteraction(content)
            else -> buildAppearance(content)
        }
    }

    // ---------------------------------------------------------------- Tampilan
    private fun buildAppearance(content: LinearLayout) {
        val glass = Themes.isGlass(themeId)

        val themeCard = ui.card(content, R.string.section_theme)
        ui.note(themeCard, R.string.theme_desc)
        ui.themePicker(themeCard, themeId) { id ->
            prefs.theme = id
            recreate() // bangun ulang halaman ini dengan tema baru
        }

        val blurCard = ui.card(content, R.string.section_blur)
        val sw = ui.switchRow(blurCard, R.string.blur_enable, R.string.blur_hint, prefs.blurEnabled) { on ->
            prefs.blurEnabled = on
            ui.setEnabledDeep(sliderRoot, on)
            updateAccessUi()
        }
        ui.divider(blurCard)
        val strength = ui.sliderRow(
            blurCard, R.string.blur_strength_label, Prefs.BLUR_MIN, Prefs.BLUR_MAX, prefs.blurRadius
        ) { prefs.blurRadius = it }
        sliderRoot = strength.root
        accessBox = ui.callout(blurCard, R.string.blur_access_needed, R.string.blur_access_grant) {
            WallpaperAccess.request(this)
        }

        if (!glass) {
            // Blur hanya relevan di tema glass
            ui.note(blurCard, R.string.blur_only_glass)
            ui.setEnabledDeep(sw.parent as View, false)
            ui.setEnabledDeep(strength.root, false)
        } else {
            ui.setEnabledDeep(strength.root, prefs.blurEnabled)
        }
        updateAccessUi()
    }

    private lateinit var sliderRoot: View

    /** Tombol izin wallpaper tampil kalau blur menyala tapi wallpaper belum bisa dibaca. */
    private fun updateAccessUi() {
        val need = Themes.isGlass(themeId) && prefs.blurEnabled && !WallpaperAccess.granted(this)
        accessBox?.visibility = if (need) View.VISIBLE else View.GONE
    }

    override fun onResume() {
        super.onResume()
        updateAccessUi() // setelah balik dari layar izin
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        updateAccessUi()
    }

    // ---------------------------------------------------------------- Layar utama & laci
    private fun buildLayout(content: LinearLayout) {
        val home = ui.card(content, R.string.section_home)
        ui.sliderRow(home, R.string.label_columns, 3, 6, prefs.homeCols) { prefs.homeCols = it }
        ui.divider(home)
        ui.sliderRow(home, R.string.label_rows, 3, 7, prefs.homeRows) { prefs.homeRows = it }

        val drawer = ui.card(content, R.string.section_drawer)
        ui.sliderRow(drawer, R.string.label_columns, 3, 7, prefs.drawerCols) { prefs.drawerCols = it }
    }

    // ---------------------------------------------------------------- Dock
    private fun buildDock(content: LinearLayout) {
        val dock = ui.card(content, R.string.section_dock)
        val searchCard = ui.card(content, R.string.section_search)
        ui.note(searchCard, R.string.search_widget)
        ui.segmented(
            searchCard,
            listOf(R.string.search_builtin_short, R.string.search_google_short),
            if (prefs.dockSearchGoogle) 1 else 0
        ) { prefs.dockSearchGoogle = it == 1 }

        ui.switchRow(dock, R.string.dock_two_rows, R.string.dock_two_rows_hint, prefs.dockTwoRows) { on ->
            prefs.dockTwoRows = on
            ui.setEnabledDeep(searchCard, on)
        }
        ui.setEnabledDeep(searchCard, prefs.dockTwoRows)
    }

    // ---------------------------------------------------------------- Interaksi
    private fun buildInteraction(content: LinearLayout) {
        val c = ui.card(content, R.string.section_feedback)
        ui.switchRow(c, R.string.haptics, R.string.haptics_hint, prefs.haptics) { prefs.haptics = it }
    }
}
