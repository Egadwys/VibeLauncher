package com.ega.project.vibelauncher

import android.content.ComponentName
import android.graphics.drawable.Drawable

data class AppInfo(val label: String, val component: ComponentName, val icon: Drawable) {
    val key: String get() = component.flattenToString()
    val packageName: String get() = component.packageName
}
