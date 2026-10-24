package com.ega.project.vibelauncher

import android.content.ComponentName
import android.content.Context
import android.content.Intent

object AppRepository {
    fun load(ctx: Context): List<AppInfo> {
        val pm = ctx.packageManager
        val query = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val size = (64 * ctx.resources.displayMetrics.density).toInt()
        return pm.queryIntentActivities(query, 0)
            .filter { it.activityInfo.packageName != ctx.packageName }
            .map { ri ->
                val ai = ri.activityInfo
                AppInfo(
                    ri.loadLabel(pm).toString(),
                    ComponentName(ai.packageName, ai.name),
                    IconShaper.shape(ctx.resources, ri.loadIcon(pm), size)
                )
            }
            .sortedBy { it.label.lowercase() }
    }
}
