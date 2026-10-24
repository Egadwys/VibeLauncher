package com.ega.project.vibelauncher

import android.content.Intent
import android.os.Bundle
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class SettingsActivity : AppCompatActivity() {
    private lateinit var defaultStatus: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        defaultStatus = findViewById(R.id.default_launcher_status)
        findViewById<android.view.View>(R.id.item_default_launcher).setOnClickListener { chooseDefaultLauncher() }
        findViewById<android.view.View>(R.id.item_customize).setOnClickListener {
            startActivity(Intent(this, CustomizeActivity::class.java))
        }
    }

    override fun onResume() {
        super.onResume()
        defaultStatus.setText(
            if (isDefaultLauncher()) R.string.default_launcher_active else R.string.default_launcher_inactive
        )
    }

    private fun isDefaultLauncher() = DefaultLauncher.isDefault(this)

    private fun chooseDefaultLauncher() = DefaultLauncher.request(this)
}
