package com.ega.project.vibelauncher

import android.content.pm.PackageManager
import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.Button
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.RadioGroup
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class CustomizeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_customize)
        val p = Prefs(this)

        findViewById<ImageView>(R.id.btn_back)?.setOnClickListener { finish() }

        bind(R.id.label_home_cols, R.id.slider_home_cols, R.string.home_columns, p.homeCols) { p.homeCols = it }
        bind(R.id.label_home_rows, R.id.slider_home_rows, R.string.home_rows, p.homeRows) { p.homeRows = it }
        bind(R.id.label_drawer_cols, R.id.slider_drawer_cols, R.string.drawer_columns, p.drawerCols) { p.drawerCols = it }

        val themeIds = listOf(Themes.MATERIAL, Themes.GLASS_DARK, Themes.GLASS_LIGHT)
        val themeNames = listOf(R.string.theme_material, R.string.theme_glass_dark, R.string.theme_glass_light)
            .map { getString(it) }
        val spinner = findViewById<Spinner>(R.id.spinner_theme)
        spinner.adapter = ArrayAdapter(this, R.layout.spinner_item, themeNames).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        val currentTheme = Themes.normalize(p.theme)
        spinner.setSelection(themeIds.indexOf(currentTheme).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) {
                p.theme = themeIds[pos]
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

        // Blur Liquid Glass (BlurView): switch + kekuatan
        val swBlur = findViewById<SwitchMaterial>(R.id.switch_blur)
        val sliderBlur = findViewById<Slider>(R.id.slider_blur_radius)
        val labelBlur = findViewById<TextView>(R.id.label_blur_radius)
        bind(R.id.label_blur_radius, R.id.slider_blur_radius, R.string.blur_strength, p.blurRadius) { p.blurRadius = it }
        fun enableBlurUi(on: Boolean) { sliderBlur.isEnabled = on; labelBlur.alpha = if (on) 1f else 0.4f }
        swBlur.isChecked = p.blurEnabled
        enableBlurUi(p.blurEnabled)
        swBlur.setOnCheckedChangeListener { _, on -> p.blurEnabled = on; enableBlurUi(on); updateAccessUi() }
        findViewById<Button>(R.id.btn_blur_access).setOnClickListener { WallpaperAccess.request(this) }
        updateAccessUi()

        val swHaptics = findViewById<SwitchMaterial>(R.id.switch_haptics)
        swHaptics.isChecked = p.haptics
        swHaptics.setOnCheckedChangeListener { _, on -> p.haptics = on }

        val sw = findViewById<SwitchMaterial>(R.id.switch_dock_two_rows)
        val group = findViewById<RadioGroup>(R.id.group_search)
        fun enableGroup(on: Boolean) {
            for (i in 0 until group.childCount) group.getChildAt(i).isEnabled = on
        }
        sw.isChecked = p.dockTwoRows
        group.check(if (p.dockSearchGoogle) R.id.radio_search_google else R.id.radio_search_builtin)
        enableGroup(p.dockTwoRows)
        sw.setOnCheckedChangeListener { _, on -> p.dockTwoRows = on; enableGroup(on) }
        group.setOnCheckedChangeListener { _, id -> p.dockSearchGoogle = id == R.id.radio_search_google }
    }

    override fun onResume() {
        super.onResume()
        updateAccessUi() // setelah balik dari layar izin
    }

    override fun onRequestPermissionsResult(code: Int, perms: Array<out String>, results: IntArray) {
        super.onRequestPermissionsResult(code, perms, results)
        updateAccessUi()
    }

    /** Info + tombol izin wallpaper: tampil kalau blur nyala tapi wallpaper belum bisa dibaca. */
    private fun updateAccessUi() {
        val need = Prefs(this).blurEnabled && !WallpaperAccess.granted(this)
        findViewById<View>(R.id.blur_access_box).visibility = if (need) View.VISIBLE else View.GONE
    }

    private fun bind(labelId: Int, sliderId: Int, fmt: Int, initial: Int, save: (Int) -> Unit) {
        val label = findViewById<TextView>(labelId)
        val slider = findViewById<Slider>(sliderId)
        label.text = getString(fmt, initial)
        slider.value = initial.toFloat()
        slider.addOnChangeListener { _, v, _ ->
            val n = v.toInt()
            label.text = getString(fmt, n)
            save(n)
        }
    }
}