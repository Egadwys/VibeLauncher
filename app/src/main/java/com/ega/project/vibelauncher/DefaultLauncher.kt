package com.ega.project.vibelauncher

import android.app.Activity
import android.app.role.RoleManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings

object DefaultLauncher {
    fun isDefault(ctx: Context): Boolean {
        val home = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME)
        val ri = ctx.packageManager.resolveActivity(home, PackageManager.MATCH_DEFAULT_ONLY)
        return ri?.activityInfo?.packageName == ctx.packageName
    }

    /** Android 10+: dialog peran Home langsung; selain itu / sudah default -> layar pengaturan Home. */
    fun request(a: Activity) {
        if (Build.VERSION.SDK_INT >= 29 && !isDefault(a)) {
            try {
                val rm = a.getSystemService(RoleManager::class.java)
                if (rm != null && rm.isRoleAvailable(RoleManager.ROLE_HOME)) {
                    @Suppress("DEPRECATION")
                    a.startActivityForResult(rm.createRequestRoleIntent(RoleManager.ROLE_HOME), 201)
                    return
                }
            } catch (_: Exception) {
            }
        }
        for (action in listOf(Settings.ACTION_HOME_SETTINGS, Settings.ACTION_MANAGE_DEFAULT_APPS_SETTINGS, Settings.ACTION_SETTINGS)) {
            try { a.startActivity(Intent(action)); return } catch (_: Exception) {}
        }
    }
}
