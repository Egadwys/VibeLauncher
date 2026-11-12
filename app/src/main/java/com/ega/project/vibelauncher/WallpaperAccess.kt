package com.ega.project.vibelauncher

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat

/**
 * Akses baca wallpaper asli (sumber blur BlurView).
 * - Android 12 ke bawah : READ_EXTERNAL_STORAGE (izin runtime biasa).
 * - Android 13+        : WallpaperManager.getDrawable() cuma memberi wallpaper asli ke app
 *   dengan All files access (MANAGE_EXTERNAL_STORAGE); tanpa itu hasilnya wallpaper default
 *   sistem (A13) atau SecurityException (A14+).
 */
object WallpaperAccess {
    const val REQ_CODE = 301

    fun granted(ctx: Context): Boolean = when {
        Build.VERSION.SDK_INT >= 33 -> Environment.isExternalStorageManager()
        Build.VERSION.SDK_INT >= 30 ->
            Environment.isExternalStorageManager() || hasReadStorage(ctx)
        else -> hasReadStorage(ctx)
    }

    private fun hasReadStorage(ctx: Context) =
        ContextCompat.checkSelfPermission(ctx, android.Manifest.permission.READ_EXTERNAL_STORAGE) ==
            PackageManager.PERMISSION_GRANTED

    fun request(activity: Activity) {
        if (Build.VERSION.SDK_INT >= 33) {
            val uri = Uri.parse("package:${activity.packageName}")
            try {
                activity.startActivity(Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION, uri))
            } catch (_: Exception) {
                try {
                    activity.startActivity(Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION))
                } catch (_: Exception) {}
            }
        } else {
            ActivityCompat.requestPermissions(
                activity, arrayOf(android.Manifest.permission.READ_EXTERNAL_STORAGE), REQ_CODE
            )
        }
    }
}