package com.ega.project.vibelauncher

import android.os.Bundle
import android.widget.RadioGroup
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        val p = Prefs(this)
        bind(R.id.label_home_cols, R.id.slider_home_cols, R.string.home_columns, p.homeCols) { p.homeCols = it }
        bind(R.id.label_home_rows, R.id.slider_home_rows, R.string.home_rows, p.homeRows) { p.homeRows = it }
        bind(R.id.label_drawer_cols, R.id.slider_drawer_cols, R.string.drawer_columns, p.drawerCols) { p.drawerCols = it }

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
