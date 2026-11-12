package com.ega.project.vibelauncher

import android.content.res.ColorStateList
import android.os.Build
import android.os.Bundle
import android.view.View
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.WindowInsetsControllerCompat

/**
 * Dasar semua layar pengaturan: kerangka (back + judul besar), latar & ikon status bar mengikuti tema.
 * Window dibuat transparan lewat tema; untuk Material latarnya solid, untuk Glass berupa scrim
 * di atas blur window (home + wallpaper di belakangnya).
 */
abstract class BaseSettingsActivity : AppCompatActivity() {
    protected lateinit var prefs: Prefs
    protected lateinit var ui: SettingsUi
    protected var themeId: String = Themes.MATERIAL

    protected abstract val titleRes: Int
    protected abstract fun build(content: LinearLayout)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        prefs = Prefs(this)
        themeId = Themes.normalize(prefs.theme)
        setContentView(R.layout.activity_settings)

        val d = resources.displayMetrics.density
        val glass = Themes.isGlass(themeId)
        val windowBlur = glass && Themes.crossWindowBlur(this)

        findViewById<View>(R.id.root).setBackgroundColor(Themes.settingsScrim(themeId, windowBlur))
        if (windowBlur && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            window.addFlags(WindowManager.LayoutParams.FLAG_BLUR_BEHIND)
            val lp = window.attributes
            lp.blurBehindRadius = (48 * d).toInt()
            window.attributes = lp
        }
        WindowInsetsControllerCompat(window, window.decorView).apply {
            isAppearanceLightStatusBars = Themes.isLight(themeId)
            isAppearanceLightNavigationBars = Themes.isLight(themeId)
        }

        ui = SettingsUi(this, themeId)
        findViewById<ImageView>(R.id.btn_back).apply {
            imageTintList = ColorStateList.valueOf(Themes.textPrimary(this@BaseSettingsActivity, themeId))
            background = Themes.rippleBg(themeId, 22f, d)
            setOnClickListener { finish() }
        }
        findViewById<TextView>(R.id.page_title).apply {
            setText(titleRes)
            setTextColor(Themes.textPrimary(this@BaseSettingsActivity, themeId))
        }
        build(findViewById(R.id.content))
    }
}
