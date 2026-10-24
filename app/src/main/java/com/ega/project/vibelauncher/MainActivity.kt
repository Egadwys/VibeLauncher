package com.ega.project.vibelauncher

import android.content.*
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.GridLayoutManager
import androidx.recyclerview.widget.RecyclerView
import java.util.concurrent.Executors

class MainActivity : AppCompatActivity() {

    private lateinit var prefs: Prefs
    private lateinit var home: RecyclerView
    private lateinit var drawer: View
    private lateinit var drawerList: RecyclerView

    private var apps: List<AppInfo> = emptyList()
    private val executor = Executors.newSingleThreadExecutor()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = loadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)

        home = findViewById(R.id.home_grid)
        drawer = findViewById(R.id.drawer)
        drawerList = findViewById(R.id.drawer_list)

        findViewById<View>(R.id.btn_apps).setOnClickListener { openDrawer() }
        findViewById<View>(R.id.search_bar).setOnClickListener { openSearch() }
        findViewById<View>(R.id.root).setOnLongClickListener { openSettings(); true }
        findViewById<View>(R.id.drawer_overflow).setOnClickListener { v ->
            PopupMenu(this, v).apply {
                menu.add(0, 1, 0, R.string.settings)
                setOnMenuItemClickListener { openSettings(); true }
            }.show()
        }

        bindDock(R.id.btn_phone, Intent(Intent.ACTION_DIAL))
        bindDock(
            R.id.btn_browser,
            Intent(Intent.ACTION_VIEW, Uri.parse("http://")).addCategory(Intent.CATEGORY_BROWSABLE)
        )

        onBackPressedDispatcher.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() = closeDrawer()
        })
        addOnNewIntentListener { closeDrawer() } // tombol Home ditekan saat drawer terbuka

        val f = IntentFilter().apply {
            addAction(Intent.ACTION_PACKAGE_ADDED)
            addAction(Intent.ACTION_PACKAGE_REMOVED)
            addAction(Intent.ACTION_PACKAGE_REPLACED)
            addDataScheme("package")
        }
        ContextCompat.registerReceiver(this, pkgReceiver, f, ContextCompat.RECEIVER_EXPORTED)

        loadApps()
    }

    override fun onResume() {
        super.onResume()
        render() // terapkan ulang grid setelah kembali dari Settings
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pkgReceiver)
        executor.shutdown()
    }

    // ---------- data & render ----------
    private fun loadApps() {
        executor.execute {
            val list = AppRepository.load(this)
            runOnUiThread { apps = list; render() }
        }
    }

    private fun render() {
        val cols = prefs.homeCols
        val rows = prefs.homeRows
        val byKey = apps.associateBy { it.key }
        val items = prefs.homeCells(cols * rows).map { byKey[it] }

        home.layoutManager = object : GridLayoutManager(this@MainActivity, cols) {
            override fun canScrollVertically() = false
        }
        home.adapter = AppGridAdapter(
            items, cols, rows, ::launch,
            { v, app, pos -> showHomeMenu(v, app, pos) },
            ::openSettings
        )

        val dCols = prefs.drawerCols
        drawerList.layoutManager = GridLayoutManager(this, dCols)
        drawerList.adapter = AppGridAdapter(apps, dCols, null, ::launch, { v, app, _ -> showDrawerMenu(v, app) })
    }

    // ---------- aksi ----------
    private fun launch(app: AppInfo) {
        val i = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(app.component)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED)
        try { startActivity(i) } catch (_: Exception) {
            Toast.makeText(this, app.label, Toast.LENGTH_SHORT).show()
        }
        closeDrawer()
    }

    private fun openSettings() = startActivity(Intent(this, SettingsActivity::class.java))

    private fun openSearch() {
        try { startActivity(Intent(Intent.ACTION_WEB_SEARCH)) } catch (_: Exception) {
            try { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("https://www.google.com"))) } catch (_: Exception) {}
        }
    }

    private fun bindDock(id: Int, intent: Intent) {
        val view = findViewById<ImageView>(id)
        val ri = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY)
        if (ri != null && ri.activityInfo.packageName != "android") {
            val size = (56 * resources.displayMetrics.density).toInt()
            view.setImageDrawable(IconShaper.shape(resources, ri.loadIcon(packageManager), size))
            view.setOnClickListener {
                try { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            }
        } else view.visibility = View.INVISIBLE
    }

    private fun showHomeMenu(anchor: View, app: AppInfo, pos: Int) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.remove)
            menu.add(0, 2, 0, R.string.app_info)
            menu.add(0, 3, 0, R.string.uninstall)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> {
                        val cells = prefs.homeCells(prefs.homeCols * prefs.homeRows)
                        if (pos in cells.indices) { cells[pos] = ""; prefs.saveHomeCells(cells); render() }
                    }
                    2 -> openAppInfo(app)
                    3 -> uninstall(app)
                }
                true
            }
        }.show()
    }

    private fun showDrawerMenu(anchor: View, app: AppInfo) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.add_to_home)
            menu.add(0, 2, 0, R.string.app_info)
            menu.add(0, 3, 0, R.string.uninstall)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> addToHome(app)
                    2 -> openAppInfo(app)
                    3 -> uninstall(app)
                }
                true
            }
        }.show()
    }

    private fun addToHome(app: AppInfo) {
        val cells = prefs.homeCells(prefs.homeCols * prefs.homeRows)
        if (cells.contains(app.key)) { closeDrawer(); return }
        val i = cells.indexOf("")
        if (i < 0) {
            Toast.makeText(this, R.string.home_full, Toast.LENGTH_SHORT).show()
            return
        }
        cells[i] = app.key
        prefs.saveHomeCells(cells)
        render()
        closeDrawer()
    }

    private fun openAppInfo(app: AppInfo) {
        startActivity(
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${app.packageName}"))
        )
    }

    private fun uninstall(app: AppInfo) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:${app.packageName}")))
    }

    // ---------- drawer ----------
    private fun openDrawer() {
        if (drawer.visibility == View.VISIBLE) return
        drawerList.scrollToPosition(0)
        drawer.alpha = 0f
        drawer.translationY = 60 * resources.displayMetrics.density
        drawer.visibility = View.VISIBLE
        drawer.animate().alpha(1f).translationY(0f).setDuration(220).start()
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        drawer.animate().alpha(0f).translationY(60 * resources.displayMetrics.density)
            .setDuration(180).withEndAction { drawer.visibility = View.GONE }.start()
    }
}
