package com.ega.project.vibelauncher

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.*
import android.content.pm.PackageManager
import android.content.res.ColorStateList
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.graphics.Rect
import android.view.DragEvent
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.widget.TextViewCompat
import androidx.core.view.doOnLayout
import androidx.viewpager2.widget.ViewPager2
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.max

class MainActivity : AppCompatActivity() {

    companion object {
        private const val HOST_ID = 1024
        private const val REQ_BIND = 101
        private const val REQ_CONFIGURE = 102
        private const val PURPOSE_HOME = 0
        private const val PURPOSE_DOCK = 1
        private const val GOOGLE_PKG = "com.google.android.googlequicksearchbox"
    }

    /** Data yang sedang di-drag (app dari drawer, atau app/widget dari home). */
    private class DragPayload(
        val kind: Int, val key: String, val spanX: Int, val spanY: Int,
        val fromHome: Boolean, val col: Int = -1, val row: Int = -1
    )

    private class Pending(val id: Int, val info: AppWidgetProviderInfo, val purpose: Int)

    private lateinit var prefs: Prefs
    private lateinit var homeGrid: HomeGridLayout
    private lateinit var drawer: View
    private lateinit var drawerPager: ViewPager2
    private lateinit var drawerDots: LinearLayout
    private lateinit var searchTop: View
    private lateinit var searchDock: View
    private lateinit var dockRow2: View
    private lateinit var dockWidgetContainer: FrameLayout
    private lateinit var dragBarWrap: View
    private lateinit var dragRemove: View
    private lateinit var dragInfo: View
    private lateinit var dragUninstall: View

    private lateinit var widgetMgr: AppWidgetManager
    private lateinit var widgetHost: LauncherAppWidgetHost
    private val widgetViews = HashMap<Int, LauncherWidgetHostView>()
    private var pending: Pending? = null
    private var drag: DragPayload? = null
    private var currentPopup: PopupMenu? = null
    private lateinit var gestures: GestureDetector
    private var gestureBlocked = false

    private var apps: List<AppInfo> = emptyList()
    private var appsLoaded = false
    private var iconsGlass = false
    private val executor = Executors.newSingleThreadExecutor()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = loadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        widgetMgr = AppWidgetManager.getInstance(this)
        widgetHost = LauncherAppWidgetHost(this, HOST_ID)

        homeGrid = findViewById(R.id.home_grid)
        drawer = findViewById(R.id.drawer)
        drawerPager = findViewById(R.id.drawer_pager)
        drawerDots = findViewById(R.id.drawer_dots)
        searchTop = findViewById(R.id.search_bar)
        searchDock = findViewById(R.id.search_bar_dock)
        dockRow2 = findViewById(R.id.dock_search_row)
        dockWidgetContainer = findViewById(R.id.dock_widget_container)
        dragBarWrap = findViewById(R.id.drag_bar_wrap)
        dragRemove = findViewById(R.id.drag_remove)
        dragInfo = findViewById(R.id.drag_info)
        dragUninstall = findViewById(R.id.drag_uninstall)

        findViewById<View>(R.id.btn_apps).setOnClickListener { openDrawer() }
        searchTop.setOnClickListener { openSearch() }
        searchDock.setOnClickListener { openSearch() }
        homeGrid.setOnLongClickListener { showHomeMenu(); true } // area kosong
        findViewById<View>(R.id.drawer_overflow).setOnClickListener { v ->
            PopupMenu(this, v).apply {
                menu.add(0, 1, 0, R.string.settings)
                setOnMenuItemClickListener { openSettings(); true }
            }.show()
        }

        drawerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) = updateDots(position)
        })

        setupHomeDrag()
        gestures = GestureDetector(this, object : GestureDetector.SimpleOnGestureListener() {
            override fun onFling(e1: MotionEvent?, e2: MotionEvent, vx: Float, vy: Float): Boolean {
                if (e1 == null) return false
                val dx = e2.x - e1.x
                val dy = e2.y - e1.y
                val min = 64 * resources.displayMetrics.density
                if (abs(dy) < min || abs(dy) < abs(dx) * 2 || abs(vy) < 500) return false
                onVerticalSwipe(up = dy < 0)
                return false
            }
        })
        gestures.setIsLongpressEnabled(false)
        setupDropTarget(dragRemove) { p -> removeDragged(p) }
        setupDropTarget(dragInfo) { p ->
            if (p.kind == HomeItem.KIND_APP) { openAppInfo(pkgOf(p.key)); true } else false
        }
        setupDropTarget(dragUninstall) { p ->
            if (p.kind == HomeItem.KIND_APP) { uninstall(pkgOf(p.key)); true } else false
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

    // ---------- gesture swipe ----------
    override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
        if (ev.actionMasked == MotionEvent.ACTION_DOWN) gestureBlocked = isOnHomeWidget(ev.rawX, ev.rawY)
        if (!gestureBlocked && drag == null) gestures.onTouchEvent(ev)
        return super.dispatchTouchEvent(ev)
    }

    /** Swipe di atas widget dibiarkan untuk widget itu sendiri (mis. list yang bisa di-scroll). */
    private fun isOnHomeWidget(x: Float, y: Float): Boolean {
        if (drawer.visibility == View.VISIBLE) return false
        val r = Rect()
        for (i in 0 until homeGrid.childCount) {
            val c = homeGrid.getChildAt(i)
            if ((c.tag as? HomeItem)?.kind != HomeItem.KIND_WIDGET) continue
            if (c.getGlobalVisibleRect(r) && r.contains(x.toInt(), y.toInt())) return true
        }
        return false
    }

    private fun onVerticalSwipe(up: Boolean) {
        if (drawer.visibility == View.VISIBLE) {
            if (!up) closeDrawer() // swipe turun menutup drawer
        } else if (up) openDrawer() else expandNotifications()
    }

    private fun expandNotifications() {
        try {
            val sbs = getSystemService("statusbar")
            Class.forName("android.app.StatusBarManager").getMethod("expandNotificationsPanel").invoke(sbs)
        } catch (_: Exception) {
        }
    }

    override fun onStart() {
        super.onStart()
        try { widgetHost.startListening() } catch (_: Exception) {}
    }

    override fun onStop() {
        try { widgetHost.stopListening() } catch (_: Exception) {}
        super.onStop()
    }

    override fun onResume() {
        super.onResume()
        render() // terapkan ulang setelah kembali dari Settings
    }

    override fun onDestroy() {
        super.onDestroy()
        unregisterReceiver(pkgReceiver)
        executor.shutdown()
    }

    // ---------- data & render ----------
    private fun loadApps() {
        val glass = Themes.isGlass(prefs.theme)
        executor.execute {
            val list = AppRepository.load(this, glass)
            runOnUiThread { apps = list; appsLoaded = true; iconsGlass = glass; renderHome() }
        }
    }

    private fun render() {
        // Ikon dibentuk ulang kalau tema (kaca / bukan) berubah
        if (appsLoaded && Themes.isGlass(prefs.theme) != iconsGlass) loadApps()
        applyTheme()
        renderHome()
        applyDock()
    }

    // ---------- tema ----------
    private fun blurSupported() =
        Build.VERSION.SDK_INT >= 31 && try { windowManager.isCrossWindowBlurEnabled } catch (_: Exception) { false }

    /** Blur wallpaper di belakang drawer (Android 12+, kalau perangkat mendukung). */
    private fun setBlur(on: Boolean) {
        if (Build.VERSION.SDK_INT < 31) return
        try {
            window.setBackgroundBlurRadius(if (on && Themes.isGlass(prefs.theme) && blurSupported()) 80 else 0)
        } catch (_: Exception) {
        }
    }

    private fun applyTheme() {
        val t = prefs.theme
        val glass = Themes.isGlass(t)
        val light = Themes.isLight(t)
        val d = resources.displayMetrics.density

        val searchText = if (light) 0x99000000.toInt() else 0xCCFFFFFF.toInt()
        for (v in listOf(searchTop, searchDock)) {
            val tv = v as TextView
            if (glass) {
                tv.background = Themes.glassPanel(this, light, 24f)
                tv.setTextColor(searchText)
                TextViewCompat.setCompoundDrawableTintList(tv, ColorStateList.valueOf(searchText))
            } else {
                tv.setBackgroundResource(R.drawable.bg_search)
                tv.setTextColor(0xB3FFFFFF.toInt())
                TextViewCompat.setCompoundDrawableTintList(tv, null)
            }
        }

        // Dock: kaca melayang dengan sudut membulat
        val dock = findViewById<View>(R.id.dock)
        val lp = dock.layoutParams as ViewGroup.MarginLayoutParams
        if (glass) {
            val m = (12 * d).toInt()
            lp.setMargins(m, 0, m, (10 * d).toInt())
            dock.background = Themes.glassPanel(this, light, 30f)
        } else {
            lp.setMargins(0, 0, 0, 0)
            dock.setBackgroundResource(R.drawable.bg_dock)
        }
        dock.layoutParams = lp
        findViewById<View>(R.id.dock_divider).visibility = if (glass) View.GONE else View.VISIBLE

        // Drawer
        val accent = Themes.accent(this, t)
        drawer.setBackgroundColor(Themes.drawerBg(t, blurSupported()))
        findViewById<TextView>(R.id.drawer_title).setTextColor(accent)
        findViewById<View>(R.id.drawer_underline).setBackgroundColor(accent)
        findViewById<View>(R.id.drawer_divider).setBackgroundColor(
            if (!glass) ContextCompat.getColor(this, R.color.holo_line) else if (light) 0x33000000 else 0x33FFFFFF
        )
        findViewById<TextView>(R.id.drawer_overflow)
            .setTextColor(if (light) 0xFF1C1C1E.toInt() else 0xFFFFFFFF.toInt())

        // Bar target drag
        findViewById<View>(R.id.drag_bar).setBackgroundColor(Themes.dragBarBg(t))
        for (id in listOf(R.id.drag_remove, R.id.drag_info, R.id.drag_uninstall))
            findViewById<TextView>(id).setTextColor(accent)

        bindDock(R.id.btn_phone, Intent(Intent.ACTION_DIAL))
        bindDock(
            R.id.btn_browser,
            Intent(Intent.ACTION_VIEW, Uri.parse("http://")).addCategory(Intent.CATEGORY_BROWSABLE)
        )
    }

    private fun renderHome() {
        val cols = prefs.homeCols
        val rows = prefs.homeRows
        val items = prefs.homeItems()
        val byKey = apps.associateBy { it.key }
        var dirty = false

        // Grid dikecilkan -> pindahkan / buang item yang tidak muat
        val fit = HomeLayout.fit(items, cols, rows)
        fit.dropped.forEach { deleteWidgetIfAny(it) }
        if (fit.changed) dirty = true

        // Buang app yang sudah di-uninstall & widget yang provider-nya hilang
        val iter = items.iterator()
        while (iter.hasNext()) {
            val i = iter.next()
            val gone = if (i.kind == HomeItem.KIND_APP) appsLoaded && byKey[i.key] == null
            else widgetMgr.getAppWidgetInfo(i.key.toIntOrNull() ?: -1) == null
            if (gone) { iter.remove(); deleteWidgetIfAny(i); dirty = true }
        }
        if (dirty) prefs.saveHomeItems(items)

        homeGrid.cols = cols
        homeGrid.rows = rows
        homeGrid.removeAllViews()
        for (item in items) {
            val v: View? = if (item.kind == HomeItem.KIND_APP) {
                byKey[item.key]?.let { appCell(it, item, cols) }
            } else homeWidgetView(item)
            if (v != null) { v.tag = item; homeGrid.addView(v) }
        }
    }

    private fun appCell(app: AppInfo, item: HomeItem, cols: Int): View {
        val v = AppCellView(this, app, cols, false) // tanpa ripple di home screen
        v.setOnClickListener { launch(app) }
        v.setOnLongClickListener {
            startDrag(it, DragPayload(HomeItem.KIND_APP, app.key, 1, 1, true, item.col, item.row)); true
        }
        return v
    }

    private fun homeWidgetView(item: HomeItem): View? {
        val id = item.key.toIntOrNull() ?: return null
        val v = widgetViewFor(id) ?: return null
        v.onLongPress = {
            val t = v.tag as? HomeItem
            if (t != null) startDrag(v, DragPayload(t.kind, t.key, t.spanX, t.spanY, true, t.col, t.row))
        }
        return v
    }

    private fun widgetViewFor(id: Int): LauncherWidgetHostView? {
        widgetViews[id]?.let { (it.parent as? ViewGroup)?.removeView(it); return it }
        val info = widgetMgr.getAppWidgetInfo(id) ?: return null
        val v = widgetHost.createView(this, id, info) as LauncherWidgetHostView
        widgetViews[id] = v
        return v
    }

    private fun deleteWidgetIfAny(item: HomeItem) {
        if (item.kind != HomeItem.KIND_WIDGET) return
        val id = item.key.toIntOrNull() ?: return
        widgetViews.remove(id)
        widgetHost.deleteAppWidgetId(id)
    }

    // ---------- dock dua baris ----------
    private fun applyDock() {
        val two = prefs.dockTwoRows
        searchTop.visibility = if (two) View.GONE else View.VISIBLE
        dockRow2.visibility = if (two) View.VISIBLE else View.GONE
        dockWidgetContainer.removeAllViews()
        dockWidgetContainer.visibility = View.GONE
        searchDock.visibility = View.VISIBLE
        if (!two) return

        if (prefs.dockSearchGoogle) {
            val id = prefs.dockWidgetId
            val v = if (id >= 0) widgetViewFor(id) else null
            if (v != null) {
                v.onLongPress = null
                dockWidgetContainer.addView(
                    v, FrameLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT
                    )
                )
                dockWidgetContainer.visibility = View.VISIBLE
                searchDock.visibility = View.GONE
            } else if (pending == null) {
                val gi = findGoogleSearchProvider()
                if (gi == null) {
                    toast(R.string.google_search_missing)
                    prefs.dockSearchGoogle = false
                } else startBind(gi, PURPOSE_DOCK)
            }
        } else if (prefs.dockWidgetId >= 0) {
            // Kembali ke search bar bawaan -> lepas widget Google
            val id = prefs.dockWidgetId
            widgetViews.remove(id)
            widgetHost.deleteAppWidgetId(id)
            prefs.dockWidgetId = -1
        }
    }

    private fun findGoogleSearchProvider(): AppWidgetProviderInfo? {
        val pm = packageManager
        val google = widgetMgr.installedProviders.filter { it.provider.packageName == GOOGLE_PKG }
        return google.firstOrNull { it.provider.className.contains("SearchWidget", ignoreCase = true) }
            ?: google.firstOrNull { (it.loadLabel(pm) ?: "").contains("search", ignoreCase = true) }
    }

    // ---------- widget: pilih, bind, konfigurasi ----------
    private fun showHomeMenu() {
        val items = arrayOf(getString(R.string.widgets), getString(R.string.settings))
        AlertDialog.Builder(this).setItems(items) { _, which ->
            if (which == 0) showWidgetPicker() else openSettings()
        }.show()
    }

    private fun widgetSpan(info: AppWidgetProviderInfo): Pair<Int, Int> {
        val cw = max(1f, homeGrid.cellWidth)
        val ch = max(1f, homeGrid.cellHeight)
        val sx = ceil(info.minWidth / cw).toInt().coerceIn(1, prefs.homeCols)
        val sy = ceil(info.minHeight / ch).toInt().coerceIn(1, prefs.homeRows)
        return Pair(sx, sy)
    }

    private fun showWidgetPicker() {
        val pm = packageManager
        val list = widgetMgr.installedProviders
            .filter { (it.widgetCategory and AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN) != 0 }
            .sortedBy { it.loadLabel(pm)?.lowercase() ?: "" }
        if (list.isEmpty()) { toast(R.string.no_widgets); return }
        val adapter = object : ArrayAdapter<AppWidgetProviderInfo>(this, R.layout.item_widget, list) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_widget, parent, false)
                val info = getItem(position)!!
                v.findViewById<ImageView>(R.id.widget_icon).setImageDrawable(info.loadIcon(context, 0))
                v.findViewById<TextView>(R.id.widget_title).text = info.loadLabel(pm)
                val (sx, sy) = widgetSpan(info)
                v.findViewById<TextView>(R.id.widget_size).text = "$sx × $sy"
                return v
            }
        }
        AlertDialog.Builder(this).setTitle(R.string.widgets)
            .setAdapter(adapter) { _, i -> startBind(list[i], PURPOSE_HOME) }
            .show()
    }

    private fun startBind(info: AppWidgetProviderInfo, purpose: Int) {
        try {
            val id = widgetHost.allocateAppWidgetId()
            val p = Pending(id, info, purpose)
            pending = p
            if (widgetMgr.bindAppWidgetIdIfAllowed(id, info.provider)) onBound(p)
            else {
                val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                @Suppress("DEPRECATION")
                startActivityForResult(i, REQ_BIND)
            }
        } catch (_: Exception) {
            cancelPending()
        }
    }

    private fun onBound(p: Pending) {
        if (p.info.configure != null) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(this, p.id, 0, REQ_CONFIGURE, null)
            } catch (_: Exception) {
                cancelPending()
            }
        } else finishPending(p)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        val p = pending ?: return
        when (requestCode) {
            REQ_BIND -> if (resultCode == RESULT_OK) onBound(p) else cancelPending()
            REQ_CONFIGURE -> if (resultCode == RESULT_OK) finishPending(p) else cancelPending()
        }
    }

    private fun cancelPending() {
        val p = pending ?: return
        pending = null
        widgetHost.deleteAppWidgetId(p.id)
        if (p.purpose == PURPOSE_DOCK) { prefs.dockSearchGoogle = false; applyDock() }
        else toast(R.string.widget_failed)
    }

    private fun finishPending(p: Pending) {
        pending = null
        if (p.purpose == PURPOSE_DOCK) {
            val old = prefs.dockWidgetId
            if (old >= 0 && old != p.id) { widgetViews.remove(old); widgetHost.deleteAppWidgetId(old) }
            prefs.dockWidgetId = p.id
            applyDock()
            return
        }
        val cols = prefs.homeCols; val rows = prefs.homeRows
        val items = prefs.homeItems()
        val (sx, sy) = widgetSpan(p.info)
        val pos = HomeLayout.findNearest(items, cols, rows, sx, sy, 0, 0)
        if (pos == null) {
            widgetHost.deleteAppWidgetId(p.id)
            toast(R.string.home_full)
            return
        }
        items.add(HomeItem(HomeItem.KIND_WIDGET, p.id.toString(), pos[0], pos[1], sx, sy))
        prefs.saveHomeItems(items)
        renderHome()
    }

    // ---------- drag & drop ----------
    private fun setupHomeDrag() {
        homeGrid.setOnDragListener { _, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> drag != null
                DragEvent.ACTION_DRAG_ENTERED, DragEvent.ACTION_DRAG_LOCATION -> {
                    drag?.let {
                        val t = homeGrid.targetCell(e.x, e.y, it.spanX, it.spanY)
                        homeGrid.showHighlight(t[0], t[1], it.spanX, it.spanY)
                    }
                    true
                }
                DragEvent.ACTION_DRAG_EXITED -> { homeGrid.clearHighlight(); true }
                DragEvent.ACTION_DROP -> { homeGrid.clearHighlight(); dropOnHome(e.x, e.y) }
                DragEvent.ACTION_DRAG_ENDED -> { homeGrid.clearHighlight(); onDragEnded(e.result); true }
                else -> true
            }
        }
    }

    private fun setupDropTarget(v: View, action: (DragPayload) -> Boolean) {
        v.setOnDragListener { view, e ->
            when (e.action) {
                DragEvent.ACTION_DRAG_STARTED -> drag != null
                DragEvent.ACTION_DRAG_ENTERED -> { view.setBackgroundColor(0x5533B5E5); true }
                DragEvent.ACTION_DRAG_EXITED -> { view.background = null; true }
                DragEvent.ACTION_DROP -> { view.background = null; drag?.let(action) ?: false }
                DragEvent.ACTION_DRAG_ENDED -> { view.background = null; onDragEnded(e.result); true }
                else -> true
            }
        }
    }

    private fun startDrag(view: View, p: DragPayload) {
        currentPopup?.dismiss()
        currentPopup = null
        drag = p
        val isApp = p.kind == HomeItem.KIND_APP
        dragRemove.visibility = if (p.fromHome) View.VISIBLE else View.GONE
        dragInfo.visibility = if (isApp) View.VISIBLE else View.GONE
        dragUninstall.visibility = if (isApp) View.VISIBLE else View.GONE
        dragBarWrap.visibility = View.VISIBLE
        val ok = ViewCompat.startDragAndDrop(
            view, ClipData.newPlainText("vibe", p.key), View.DragShadowBuilder(view), p, 0
        )
        if (!ok) { drag = null; dragBarWrap.visibility = View.GONE; return }
        if (!p.fromHome) { drawer.animate().cancel(); drawer.visibility = View.GONE; setBlur(false) } // home terlihat untuk drop
    }

    private fun showDrawerMenu(anchor: View, app: AppInfo) {
        currentPopup?.dismiss()
        currentPopup = PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.add_to_home)
            menu.add(0, 2, 0, R.string.app_info)
            menu.add(0, 3, 0, R.string.uninstall)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> addToHome(app)
                    2 -> openAppInfo(app.packageName)
                    3 -> uninstall(app.packageName)
                }
                true
            }
            show()
        }
    }

    private fun addToHome(app: AppInfo) {
        val items = prefs.homeItems()
        if (items.any { it.kind == HomeItem.KIND_APP && it.key == app.key }) { closeDrawer(); return }
        val pos = HomeLayout.findNearest(items, prefs.homeCols, prefs.homeRows, 1, 1, 0, 0)
        if (pos == null) { toast(R.string.home_full); return }
        items.add(HomeItem(HomeItem.KIND_APP, app.key, pos[0], pos[1]))
        prefs.saveHomeItems(items)
        renderHome()
        closeDrawer()
    }

    private fun startDragFromDrawer(view: View, app: AppInfo) =
        startDrag(view, DragPayload(HomeItem.KIND_APP, app.key, 1, 1, false))

    private fun onDragEnded(dropped: Boolean) {
        val p = drag ?: return // dipanggil dari beberapa listener; hanya yang pertama bekerja
        drag = null
        dragBarWrap.visibility = View.GONE
        if (!dropped && !p.fromHome) openDrawer() // drag dibatalkan -> kembali ke drawer
    }

    private fun dropOnHome(x: Float, y: Float): Boolean {
        val p = drag ?: return false
        val cols = prefs.homeCols; val rows = prefs.homeRows
        val items = prefs.homeItems()
        val existing = items.firstOrNull { it.kind == p.kind && it.key == p.key }
        val sx = existing?.spanX ?: p.spanX
        val sy = existing?.spanY ?: p.spanY
        val t = homeGrid.targetCell(x, y, sx, sy)
        var col = t[0]; var row = t[1]

        if (existing != null && existing.col == col && existing.row == row) {
            // Dilepas di tempat semula -> tampilkan menu (Hapus / Info / Copot)
            if (p.fromHome) {
                val anchor = homeGrid.viewFor(existing)
                if (anchor != null) anchor.post { showItemMenu(anchor, existing) }
            }
            return true
        }

        val others = items.filter { it !== existing }
        if (!HomeLayout.isFree(others, cols, rows, col, row, sx, sy)) {
            val occ = others.firstOrNull { HomeLayout.overlaps(it, col, row, sx, sy) }
            if (existing != null && sx == 1 && sy == 1 && occ != null &&
                occ.kind == HomeItem.KIND_APP && occ.spanX == 1 && occ.spanY == 1
            ) {
                occ.col = existing.col; occ.row = existing.row // tukar posisi dua app
            } else {
                val pos = HomeLayout.findNearest(others, cols, rows, sx, sy, col, row)
                if (pos == null) { toast(R.string.home_full); return false }
                col = pos[0]; row = pos[1]
            }
        }
        if (existing != null) { existing.col = col; existing.row = row }
        else items.add(HomeItem(p.kind, p.key, col, row, sx, sy))
        prefs.saveHomeItems(items)
        renderHome()
        return true
    }

    private fun removeDragged(p: DragPayload): Boolean {
        val items = prefs.homeItems()
        val item = items.firstOrNull { it.kind == p.kind && it.key == p.key } ?: return false
        removeItem(item, items)
        return true
    }

    private fun removeItem(item: HomeItem, items: MutableList<HomeItem> = prefs.homeItems()) {
        val target = items.firstOrNull { it.kind == item.kind && it.key == item.key } ?: return
        items.remove(target)
        deleteWidgetIfAny(target)
        prefs.saveHomeItems(items)
        renderHome()
    }

    private fun showItemMenu(anchor: View, item: HomeItem) {
        PopupMenu(this, anchor).apply {
            menu.add(0, 1, 0, R.string.remove)
            if (item.kind == HomeItem.KIND_APP) {
                menu.add(0, 2, 0, R.string.app_info)
                menu.add(0, 3, 0, R.string.uninstall)
            }
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> removeItem(item)
                    2 -> openAppInfo(pkgOf(item.key))
                    3 -> uninstall(pkgOf(item.key))
                }
                true
            }
        }.show()
    }

    // ---------- aksi ----------
    private fun pkgOf(key: String) = ComponentName.unflattenFromString(key)?.packageName ?: key

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
            view.setImageDrawable(IconShaper.shape(resources, ri.loadIcon(packageManager), size, Themes.isGlass(prefs.theme)))
            view.setOnClickListener {
                try { startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) } catch (_: Exception) {}
            }
        } else view.visibility = View.INVISIBLE
    }

    private fun openAppInfo(pkg: String) {
        startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:$pkg")))
    }

    private fun uninstall(pkg: String) {
        startActivity(Intent(Intent.ACTION_DELETE, Uri.parse("package:$pkg")))
    }

    private fun toast(res: Int) = Toast.makeText(this, res, Toast.LENGTH_SHORT).show()

    // ---------- drawer (halaman horizontal) ----------
    private fun openDrawer() {
        if (drawer.visibility == View.VISIBLE) return
        drawer.alpha = 0f
        drawer.translationY = 60 * resources.displayMetrics.density
        drawer.visibility = View.VISIBLE
        setBlur(true)
        drawer.animate().alpha(1f).translationY(0f).setDuration(220).start()
        drawerPager.doOnLayout { buildDrawer() }
    }

    private fun buildDrawer() {
        val w = drawerPager.width
        val h = drawerPager.height
        if (w <= 0 || h <= 0) return
        val cols = prefs.drawerCols
        val cellW = w / cols
        val cellH = (cellW * 1.15f).toInt()
        val rows = max(1, h / cellH)
        val pages = apps.chunked(cols * rows)
        drawerPager.adapter = DrawerPagerAdapter(pages, cols, cellW, cellH, ::launch, ::showDrawerMenu, ::startDragFromDrawer, prefs.theme)
        drawerPager.setCurrentItem(0, false)
        buildDots(pages.size)
    }

    private fun buildDots(n: Int) {
        drawerDots.removeAllViews()
        val d = resources.displayMetrics.density
        val size = (8 * d).toInt(); val m = (4 * d).toInt()
        for (i in 0 until n) {
            val dot = View(this)
            dot.layoutParams = LinearLayout.LayoutParams(size, size).apply { setMargins(m, 0, m, 0) }
            drawerDots.addView(dot)
        }
        updateDots(0)
    }

    private fun updateDots(selected: Int) {
        for (i in 0 until drawerDots.childCount) {
            drawerDots.getChildAt(i).background = GradientDrawable().apply {
                shape = GradientDrawable.OVAL
                setColor(if (i == selected) Themes.accent(this@MainActivity, prefs.theme) else Themes.dim(prefs.theme))
            }
        }
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE) return
        drawer.animate().alpha(0f).translationY(60 * resources.displayMetrics.density)
            .setDuration(180).withEndAction { drawer.visibility = View.GONE; setBlur(false) }.start()
    }
}
