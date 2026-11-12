package com.ega.project.vibelauncher

import android.widget.LinearLayout
import android.widget.TextView

/** Halaman utama Pengaturan: daftar section, tiap section membuka halamannya sendiri. */
class SettingsActivity : BaseSettingsActivity() {
    private lateinit var defaultStatus: TextView

    override val titleRes: Int get() = R.string.settings

    override fun build(content: LinearLayout) {
        // Umum
        val general = ui.card(content, R.string.section_general)
        defaultStatus = ui.navRow(
            general, R.drawable.ic_home, R.string.default_launcher, defaultStatusText()
        ) { DefaultLauncher.request(this) }

        // Personalisasi: satu baris per halaman
        val p = ui.card(content, R.string.section_personalize)
        page(p, R.drawable.ic_contrast, R.string.appearance, R.string.appearance_desc, SettingsPageActivity.PAGE_APPEARANCE)
        ui.divider(p)
        page(p, R.drawable.ic_grid, R.string.layout_title, R.string.layout_desc, SettingsPageActivity.PAGE_LAYOUT)
        ui.divider(p)
        page(p, R.drawable.ic_dock, R.string.section_dock, R.string.dock_desc, SettingsPageActivity.PAGE_DOCK)
        ui.divider(p)
        page(p, R.drawable.ic_tune, R.string.interaction, R.string.interaction_desc, SettingsPageActivity.PAGE_INTERACTION)

        // Tentang
        val about = ui.card(content, R.string.section_about)
        val version = try {
            packageManager.getPackageInfo(packageName, 0).versionName ?: ""
        } catch (_: Exception) { "" }
        val summary = listOf(getString(R.string.about_tagline), if (version.isNotEmpty()) getString(R.string.about_version, version) else "")
            .filter { it.isNotEmpty() }.joinToString(" · ")
        ui.navRow(about, R.drawable.ic_info, R.string.app_name, summary, null)
    }

    private fun page(card: LinearLayout, icon: Int, title: Int, desc: Int, page: String) {
        ui.navRow(card, icon, title, getString(desc)) {
            startActivity(SettingsPageActivity.intent(this, page))
        }
    }

    override fun onResume() {
        super.onResume()
        if (::defaultStatus.isInitialized) defaultStatus.text = defaultStatusText()
    }

    private fun defaultStatusText() = getString(
        if (DefaultLauncher.isDefault(this)) R.string.default_launcher_active else R.string.default_launcher_inactive
    )
}
