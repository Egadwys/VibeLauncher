package com.ega.project.vibelauncher

import android.app.role.RoleManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.provider.Settings
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

    private fun isDefaultLauncher(): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return ri?.activityInfo?.packageName == packageName
    }

    private fun chooseDefaultLauncher() {
        // Android 10+: dialog peran Home langsung; selain itu / sudah default -> layar pengaturan Home
        if (Build.VERSION.SDK_INT >= 29 && !isDefaultLauncher()) {
            try {
                val rm = getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    @Suppress("DEPRECATION")
                    startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 201)
                    return
                }
            } catch (_: Exception) {
            }
        }
        for (action in listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Settings.ACTION_SETTINGS)) {
            try { startActivity(Intent(action)); return } catch (_: Exception) {}
        }
    }
}
