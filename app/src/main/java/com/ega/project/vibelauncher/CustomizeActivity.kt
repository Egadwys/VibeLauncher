package com.ega.project.vibelauncher

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
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
        bind(R.id.label_home_cols, R.id.slider_home_cols, R.string.home_columns, p.homeCols) { p.homeCols = it }
        bind(R.id.label_home_rows, R.id.slider_home_rows, R.string.home_rows, p.homeRows) { p.homeRows = it }
        bind(R.id.label_drawer_cols, R.id.slider_drawer_cols, R.string.drawer_columns, p.drawerCols) { p.drawerCols = it }

        val themeIds = listOf(Themes.HOLO, Themes.GLASS_DARK, Themes.GLASS_LIGHT)
        val themeNames = listOf(R.string.theme_holo, R.string.theme_glass_dark, R.string.theme_glass_light)
            .map { getString(it) }
        val spinner = findViewById<Spinner>(R.id.spinner_theme)
        spinner.adapter = ArrayAdapter(this, R.layout.spinner_item, themeNames).apply {
            setDropDownViewResource(R.layout.spinner_dropdown_item)
        }
        spinner.setSelection(themeIds.indexOf(p.theme).coerceAtLeast(0))
        spinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, v: View?, pos: Int, id: Long) { p.theme = themeIds[pos] }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }

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
