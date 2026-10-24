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
import android.graphics.Color
import android.view.DragEvent
import android.view.HapticFeedbackConstants
import android.view.GestureDetector
import android.view.MotionEvent
import android.view.LayoutInflater
import android.text.Editable
import android.text.TextWatcher
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import androidx.appcompat.widget.PopupMenu
import android.view.ContextThemeWrapper
import android.widget.TextView
import android.widget.Toast
import androidx.activity.OnBackPressedCallback
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        val fromHome: Boolean, val col: Int = -1, val row: Int = -1, val dockSlot: Int = -1
    )

    private class Pending(val id: Int, val info: AppWidgetProviderInfo, val purpose: Int)

    private lateinit var prefs: Prefs
    private lateinit var homeGrid: HomeGridLayout
    private lateinit var homeLayer: View
    private lateinit var drawer: View
    private lateinit var dockSlots: List<ImageView>
    private var drawerOpen = false
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
    private var lastDrawerPage = 0
    private var lastHlCol = -1
    private var lastHlRow = -1

    private var apps: List<AppInfo> = emptyList()
    private var appsLoaded = false
    private lateinit var drawerSearch: EditText
    private var suppressSearch = false
    private val executor = Executors.newSingleThreadExecutor()

    private val pkgReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) = loadApps()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Status bar & navigation bar transparan, konten tergambar di belakangnya
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        if (Build.VERSION.SDK_INT >= 29) {
            window.isStatusBarContrastEnforced = false
            window.isNavigationBarContrastEnforced = false
        }
        setContentView(R.layout.activity_main)
        prefs = Prefs(this)
        widgetMgr = AppWidgetManager.getInstance(this)
        widgetHost = LauncherAppWidgetHost(this, HOST_ID)

        homeGrid = findViewById(R.id.home_grid)
        homeLayer = findViewById(R.id.home_layer)
        drawer = findViewById(R.id.drawer)
        dockSlots = listOf(R.id.dock_slot_0, R.id.dock_slot_1, R.id.dock_slot_2, R.id.dock_slot_3)
            .map { findViewById<ImageView>(it) }
        drawerPager = findViewById(R.id.drawer_pager)
        drawerSearch = findViewById(R.id.drawer_search)
        drawerSearch.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun onTextChanged(s: CharSequence?, a: Int, b: Int, c: Int) {}
            override fun afterTextChanged(s: Editable?) { if (!suppressSearch) buildDrawer() }
        })
        drawerSearch.setOnEditorActionListener { _, id, _ ->
            if (id == EditorInfo.IME_ACTION_SEARCH) { drawerResults().firstOrNull()?.let { launch(it) }; true } else false
        }
        drawerDots = findViewById(R.id.drawer_dots)
        searchTop = findViewById(R.id.search_bar)
        searchDock = findViewById(R.id.search_bar_dock)
        dockRow2 = findViewById(R.id.dock_search_row)
        dockWidgetContainer = findViewById(R.id.dock_widget_container)
        dragBarWrap = findViewById(R.id.drag_bar_wrap)
        dragRemove = findViewById(R.id.drag_remove)
        dragInfo = findViewById(R.id.drag_info)
        dragUninstall = findViewById(R.id.drag_uninstall)

        findViewById<View>(R.id.btn_apps).setOnClickListener { haptic(HapticFeedbackConstants.VIRTUAL_KEY); openDrawer() }
        searchTop.setOnClickListener { openSearch() }
        searchDock.setOnClickListener { openSearch() }
        homeGrid.setOnLongClickListener { showHomeMenu(); true } // area kosong
        findViewById<View>(R.id.drawer_overflow).setOnClickListener { v ->
            PopupMenu(popupCtx(), v).apply {
                menu.add(0, 1, 0, R.string.settings)
                setOnMenuItemClickListener { openSettings(); true }
            }.show()
        }

        drawerPager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                if (drawerOpen && position != lastDrawerPage) haptic(tickConst())
                lastDrawerPage = position
                updateDots(position)
            }
        })

        setupHomeDrag()
        setupDock()
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
        } else if (up) { haptic(gestureConst()); openDrawer() } else { haptic(gestureConst()); expandNotifications() }
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
        executor.execute {
            val list = AppRepository.load(this, false)
            runOnUiThread { apps = list; appsLoaded = true; renderHome(); renderDock() }
        }
    }

    private fun render() {
        applyTheme()
        renderHome()
        renderDock()
        applyDock()
    }

    // ---------- tema ----------
    /** Tema (Holo / Liquid) hanya berlaku untuk dock. Drawer & elemen lain selalu Holo gelap. */
    private fun applyTheme() {
        val t = prefs.theme
        val glass = Themes.isGlass(t)
        val light = Themes.isLight(t)
        val d = resources.displayMetrics.density

        fun holoSearch(tv: TextView) {
            tv.setBackgroundResource(R.drawable.bg_search)
            tv.setTextColor(0xB3FFFFFF.toInt())
            TextViewCompat.setCompoundDrawableTintList(tv, null)
        }
        holoSearch(searchTop as TextView)
        if (glass) { // search bar di dalam dock ikut tema dock
            val searchText = if (light) 0x99000000.toInt() else 0xCCFFFFFF.toInt()
            val tv = searchDock as TextView
            tv.background = Themes.glassPanel(this, light, 24f)
            tv.setTextColor(searchText)
            TextViewCompat.setCompoundDrawableTintList(tv, ColorStateList.valueOf(searchText))
        } else holoSearch(searchDock as TextView)

        // Dock: panel melayang bersudut membulat (Holo & Liquid berbagi bentuk yang sama)
        val dock = findViewById<View>(R.id.dock)
        val lp = dock.layoutParams as ViewGroup.MarginLayoutParams
        val m = (12 * d).toInt()
        lp.setMargins(m, 0, m, (10 * d).toInt())
        dock.layoutParams = lp
        dock.background = if (glass) Themes.glassPanel(this, light, 30f) else Themes.holoPanel(this, 30f)
        findViewById<View>(R.id.dock_divider).visibility = View.GONE

        // Drawer: selalu Holo gelap
        val accent = Themes.accent(this, Themes.HOLO)
        drawer.setBackgroundColor(Themes.drawerBg(Themes.HOLO, false))
        findViewById<TextView>(R.id.drawer_title).setTextColor(accent)
        findViewById<View>(R.id.drawer_underline).setBackgroundColor(accent)
        findViewById<View>(R.id.drawer_divider).setBackgroundColor(ContextCompat.getColor(this, R.color.holo_line))
        findViewById<TextView>(R.id.drawer_overflow).setTextColor(0xFFFFFFFF.toInt())
        drawerSearch.visibility = if (prefs.drawerSearch) View.VISIBLE else View.GONE
        findViewById<View>(R.id.drawer_spacer).visibility = if (prefs.drawerSearch) View.GONE else View.VISIBLE

        // Bar target drag
        findViewById<View>(R.id.drag_bar).setBackgroundColor(Themes.dragBarBg(Themes.HOLO))
        for (id in listOf(R.id.drag_remove, R.id.drag_info, R.id.drag_uninstall))
            findViewById<TextView>(id).setTextColor(accent)

        updateSystemBars()
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
        AlertDialog.Builder(this, dialogRes()).setItems(items) { _, which ->
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
        val adapter = object : ArrayAdapter<AppWidgetProviderInfo>(ContextThemeWrapper(this, dialogRes()), R.layout.item_widget, list) {
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
        AlertDialog.Builder(this, dialogRes()).setTitle(R.string.widgets)
            .setAdapter(adapter) { _, i -> startBind(list[i], PURPOSE_HOME) }
            .show()
    }

    private fun startBind(info: AppWidgetProviderInfo, purpose: Int) {
        try {
            val id = widgetHost.allocateAppWidgetId()
            val p = Pending(id, info, purpose)
            setPending(p)
            val allowed = widgetMgr.bindAppWidgetIdIfAllowed(id, info.profile, info.provider, null)
            if (allowed) onBound(p)
            else {
                val i = Intent(AppWidgetManager.ACTION_APPWIDGET_BIND)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, id)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER, info.provider)
                    .putExtra(AppWidgetManager.EXTRA_APPWIDGET_PROVIDER_PROFILE, info.profile)
                @Suppress("DEPRECATION")
                startActivityForResult(i, REQ_BIND)
            }
        } catch (e: Exception) {
            cancelPending("bind: ${e.javaClass.simpleName} ${e.message ?: ""}".trim())
        }
    }

    private fun onBound(p: Pending) {
        if (p.info.configure != null) {
            try {
                widgetHost.startAppWidgetConfigureActivityForResult(this, p.id, 0, REQ_CONFIGURE, null)
            } catch (e: Exception) {
                // Konfigurasi gagal dibuka -> coba tambahkan widget apa adanya
                if (widgetMgr.getAppWidgetInfo(p.id) != null) finishPending(p)
                else cancelPending("config: ${e.javaClass.simpleName} ${e.message ?: ""}".trim())
            }
        } else finishPending(p)
    }

    @Suppress("DEPRECATION")
    override fun onActivityResult(requestCode: Int, resultCode: Int, data: Intent?) {
        super.onActivityResult(requestCode, resultCode, data)
        if (requestCode != REQ_BIND && requestCode != REQ_CONFIGURE) return
        val p = pending ?: restorePending() ?: return
        when (requestCode) {
            REQ_BIND -> if (resultCode == RESULT_OK) onBound(p) else cancelPending("bind denied ($resultCode)")
            REQ_CONFIGURE ->
                // Sebagian widget mengembalikan CANCELED padahal sudah ter-bind; cek langsung ke sistem
                if (resultCode == RESULT_OK || data?.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1) == p.id)
                    finishPending(p)
                else cancelPending("config cancelled ($resultCode)")
        }
    }

    // Pending disimpan ke prefs: activity launcher bisa dibuat ulang saat dialog izin / konfigurasi terbuka
    private fun setPending(p: Pending?) {
        pending = p
        val e = getSharedPreferences("holo_widget_pending", MODE_PRIVATE).edit()
        if (p == null) e.clear()
        else e.putInt("id", p.id).putInt("purpose", p.purpose).putString("provider", p.info.provider.flattenToString())
        e.apply()
    }

    private fun restorePending(): Pending? {
        val sp = getSharedPreferences("holo_widget_pending", MODE_PRIVATE)
        val id = sp.getInt("id", -1)
        if (id < 0) return null
        val comp = ComponentName.unflattenFromString(sp.getString("provider", "") ?: "")
        val info = widgetMgr.getAppWidgetInfo(id)
            ?: widgetMgr.installedProviders.firstOrNull { it.provider == comp }
            ?: return null
        return Pending(id, info, sp.getInt("purpose", PURPOSE_HOME)).also { pending = it }
    }

    private fun cancelPending(reason: String? = null) {
        val p = pending ?: restorePending()
        setPending(null)
        if (p == null) return
        widgetHost.deleteAppWidgetId(p.id)
        if (p.purpose == PURPOSE_DOCK) { prefs.dockSearchGoogle = false; applyDock() }
        else Toast.makeText(
            this, getString(R.string.widget_failed) + (reason?.let { " ($it)" } ?: ""), Toast.LENGTH_LONG
        ).show()
    }

    private fun finishPending(p: Pending) {
        setPending(null)
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
            haptic(rejectConst())
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
                        if (t[0] != lastHlCol || t[1] != lastHlRow) {
                            lastHlCol = t[0]; lastHlRow = t[1]
                            haptic(tickConst())
                        }
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
        haptic(HapticFeedbackConstants.LONG_PRESS)
        lastHlCol = -1; lastHlRow = -1
        dragRemove.visibility = if (p.fromHome) View.VISIBLE else View.GONE
        dragInfo.visibility = if (isApp) View.VISIBLE else View.GONE
        dragUninstall.visibility = if (isApp) View.VISIBLE else View.GONE
        dragBarWrap.visibility = View.VISIBLE
        val ok = ViewCompat.startDragAndDrop(
            view, ClipData.newPlainText("vibe", p.key), View.DragShadowBuilder(view), p, 0
        )
        if (!ok) { drag = null; dragBarWrap.visibility = View.GONE; return }
        if (!p.fromHome) { // home & dock harus terlihat untuk drop
            drawerOpen = false
            drawer.animate().cancel(); drawer.visibility = View.GONE
            homeLayer.animate().cancel(); homeLayer.alpha = 1f; homeLayer.visibility = View.VISIBLE
            updateSystemBars()
        }
    }

    private fun showDrawerMenu(anchor: View, app: AppInfo) {
        currentPopup?.dismiss()
        currentPopup = PopupMenu(popupCtx(), anchor).apply {
            menu.add(0, 1, 0, R.string.add_to_home)
            menu.add(0, 4, 0, R.string.add_to_dock)
            menu.add(0, 2, 0, R.string.app_info)
            menu.add(0, 3, 0, R.string.uninstall)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> addToHome(app)
                    4 -> addToDock(app)
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
        if (pos == null) { haptic(rejectConst()); toast(R.string.home_full); return }
        items.add(HomeItem(HomeItem.KIND_APP, app.key, pos[0], pos[1]))
        prefs.saveHomeItems(items)
        removeFromDock(app.key) // satu app hanya di satu tempat
        haptic(confirmConst())
        renderHome()
        renderDock()
        closeDrawer()
    }

    private fun startDragFromDrawer(view: View, app: AppInfo) =
        startDrag(view, DragPayload(HomeItem.KIND_APP, app.key, 1, 1, false))

    private fun onDragEnded(dropped: Boolean) {
        val p = drag ?: return // dipanggil dari beberapa listener; hanya yang pertama bekerja
        drag = null
        dragBarWrap.visibility = View.GONE
        dockSlots.forEach { it.background = null }
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
                if (pos == null) { haptic(rejectConst()); toast(R.string.home_full); return false }
                col = pos[0]; row = pos[1]
            }
        }
        if (existing != null) { existing.col = col; existing.row = row }
        else items.add(HomeItem(p.kind, p.key, col, row, sx, sy))
        prefs.saveHomeItems(items)
        if (p.kind == HomeItem.KIND_APP && removeFromDock(p.key)) renderDock()
        haptic(confirmConst())
        renderHome()
        return true
    }

    private fun removeDragged(p: DragPayload): Boolean {
        if (p.kind == HomeItem.KIND_APP && removeFromDock(p.key)) {
            haptic(confirmConst()); renderDock(); return true
        }
        val items = prefs.homeItems()
        val item = items.firstOrNull { it.kind == p.kind && it.key == p.key } ?: return false
        haptic(confirmConst())
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
        PopupMenu(popupCtx(), anchor).apply {
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
        haptic(HapticFeedbackConstants.VIRTUAL_KEY)
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
        drawerOpen = true
        lastDrawerPage = 0
        resetDrawerSearch()
        drawer.animate().cancel()
        drawer.alpha = 0f
        drawer.translationY = 60 * resources.displayMetrics.density
        drawer.visibility = View.VISIBLE
        updateSystemBars()
        drawer.animate().alpha(1f).translationY(0f).setDuration(220).start()
        // Home (termasuk dock) disembunyikan supaya tidak tembus di balik drawer
        homeLayer.animate().cancel()
        homeLayer.animate().alpha(0f).setDuration(220).withEndAction {
            if (drawerOpen) homeLayer.visibility = View.INVISIBLE
        }.start()
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
        val pages = drawerResults().chunked(cols * rows)
        drawerPager.adapter = DrawerPagerAdapter(pages, cols, cellW, cellH, ::launch, ::showDrawerMenu, ::startDragFromDrawer, Themes.HOLO)
        drawerPager.setCurrentItem(0, false)
        buildDots(pages.size)
    }

    /** Daftar app di drawer, disaring oleh search bar (kalau aktif). */
    private fun drawerResults(): List<AppInfo> {
        val q = if (prefs.drawerSearch) drawerSearch.text.toString().trim() else ""
        return if (q.isEmpty()) apps else apps.filter { it.label.contains(q, ignoreCase = true) }
    }

    private fun resetDrawerSearch() {
        suppressSearch = true
        drawerSearch.setText("")
        suppressSearch = false
        drawerSearch.clearFocus()
        (getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .hideSoftInputFromWindow(drawerSearch.windowToken, 0)
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
                setColor(if (i == selected) Themes.accent(this@MainActivity, Themes.HOLO) else Themes.dim(Themes.HOLO))
            }
        }
    }

    private fun closeDrawer() {
        if (drawer.visibility != View.VISIBLE || !drawerOpen) return
        drawerOpen = false
        resetDrawerSearch()
        updateSystemBars()
        homeLayer.animate().cancel()
        homeLayer.visibility = View.VISIBLE
        homeLayer.animate().alpha(1f).setDuration(180).start()
        drawer.animate().cancel()
        drawer.animate().alpha(0f).translationY(60 * resources.displayMetrics.density)
            .setDuration(180).withEndAction {
                if (!drawerOpen) { drawer.visibility = View.GONE }
            }.start()
    }

    // ---------- popup & dialog: bulat, mengikuti tema ----------
    private fun popupCtx() = ContextThemeWrapper(this, when (prefs.theme) {
        Themes.GLASS_DARK -> R.style.PopupOverlay_GlassDark
        Themes.GLASS_LIGHT -> R.style.PopupOverlay_GlassLight
        else -> R.style.PopupOverlay_Holo
    })

    private fun dialogRes() = when (prefs.theme) {
        Themes.GLASS_DARK -> R.style.Dialog_GlassDark
        Themes.GLASS_LIGHT -> R.style.Dialog_GlassLight
        else -> R.style.Dialog_Holo
    }

    // ---------- status bar ----------
    /** Drawer selalu gelap, jadi ikon status bar selalu putih. */
    private fun updateSystemBars() {
        val c = WindowInsetsControllerCompat(window, window.decorView)
        c.isAppearanceLightStatusBars = false
        c.isAppearanceLightNavigationBars = false
    }

    // ---------- haptic ----------
    private fun haptic(type: Int) {
        if (!prefs.haptics) return
        window.decorView.performHapticFeedback(type)
    }

    private fun tickConst() =
        if (Build.VERSION.SDK_INT >= 34) HapticFeedbackConstants.SEGMENT_FREQUENT_TICK
        else HapticFeedbackConstants.CLOCK_TICK
    private fun confirmConst() =
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.CONFIRM else HapticFeedbackConstants.VIRTUAL_KEY
    private fun rejectConst() =
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.REJECT else HapticFeedbackConstants.LONG_PRESS
    private fun gestureConst() =
        if (Build.VERSION.SDK_INT >= 30) HapticFeedbackConstants.GESTURE_START else HapticFeedbackConstants.CLOCK_TICK

    // ---------- dock (4 slot + tombol drawer di tengah) ----------
    private fun setupDock() {
        dockSlots.forEachIndexed { i, slot ->
            slot.setOnDragListener { view, e ->
                when (e.action) {
                    DragEvent.ACTION_DRAG_STARTED -> drag?.kind == HomeItem.KIND_APP
                    DragEvent.ACTION_DRAG_ENTERED -> {
                        view.background = Themes.glassPanel(this, Themes.isLight(prefs.theme), 18f)
                        haptic(tickConst()); true
                    }
                    DragEvent.ACTION_DRAG_EXITED -> { view.background = null; true }
                    DragEvent.ACTION_DROP -> { view.background = null; dropOnDock(i) }
                    DragEvent.ACTION_DRAG_ENDED -> { view.background = null; onDragEnded(e.result); true }
                    else -> true
                }
            }
        }
    }

    /** Hapus app dari dock; true kalau ada yang dihapus. */
    private fun removeFromDock(key: String): Boolean {
        val dock = prefs.dockApps()
        val i = dock.indexOf(key)
        if (i < 0) return false
        dock[i] = ""
        prefs.saveDockApps(dock)
        return true
    }

    private fun renderDock() {
        if (!appsLoaded) return
        if (prefs.dockUnset) initDefaultDock()
        val byKey = apps.associateBy { it.key }
        val dock = prefs.dockApps()
        var dirty = false
        val light = Themes.isLight(prefs.theme)
        val d = resources.displayMetrics.density
        dockSlots.forEachIndexed { i, slot ->
            val key = dock[i]
            val app = if (key.isNotEmpty()) byKey[key] else null
            if (key.isNotEmpty() && app == null) { dock[i] = ""; dirty = true } // sudah di-uninstall
            if (app != null) {
                slot.setPadding((14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt(), (14 * d).toInt())
                slot.setImageDrawable(app.icon)
                slot.imageTintList = null
                slot.contentDescription = app.label
                slot.setOnClickListener { launch(app) }
                slot.setOnLongClickListener {
                    startDrag(it, DragPayload(HomeItem.KIND_APP, app.key, 1, 1, true, dockSlot = i)); true
                }
            } else {
                // Slot kosong: tanda "+" samar, ketuk untuk memilih app
                slot.setPadding((26 * d).toInt(), (26 * d).toInt(), (26 * d).toInt(), (26 * d).toInt())
                slot.setImageResource(R.drawable.ic_add)
                slot.imageTintList = ColorStateList.valueOf(if (light) 0x40000000 else 0x40FFFFFF)
                slot.contentDescription = getString(R.string.add_to_dock)
                slot.setOnClickListener { haptic(HapticFeedbackConstants.VIRTUAL_KEY); showDockPicker(i) }
                slot.setOnLongClickListener { showDockPicker(i); true }
            }
        }
        if (dirty) prefs.saveDockApps(dock)
    }

    /** Pertama kali: isi dock dengan app telepon & browser bawaan (kalau ada), tetap bisa diganti. */
    private fun initDefaultDock() {
        val dock = MutableList(Prefs.DOCK_SLOTS) { "" }
        fun find(intent: Intent): String? {
            val ri = packageManager.resolveActivity(intent, PackageManager.MATCH_DEFAULT_ONLY) ?: return null
            val pkg = ri.activityInfo.packageName
            if (pkg == "android") return null
            return apps.firstOrNull { it.packageName == pkg }?.key
        }
        find(Intent(Intent.ACTION_DIAL))?.let { dock[0] = it }
        find(Intent(Intent.ACTION_VIEW, Uri.parse("http://")).addCategory(Intent.CATEGORY_BROWSABLE))
            ?.let { if (it !in dock) dock[3] = it }
        prefs.saveDockApps(dock)
    }

    /** Taruh app di slot. Kalau app sudah ada di dock -> tukar; kalau slot terisi -> app lama pindah ke home. */
    private fun placeInDock(key: String, slot: Int) {
        val dock = prefs.dockApps()
        val old = dock[slot]
        val from = dock.indexOf(key)
        if (from == slot) return
        if (from >= 0) {
            dock[from] = old
            dock[slot] = key
            prefs.saveDockApps(dock)
        } else {
            dock[slot] = key
            prefs.saveDockApps(dock)
            val items = prefs.homeItems()
            val inHome = items.firstOrNull { it.kind == HomeItem.KIND_APP && it.key == key }
            if (inHome != null) items.remove(inHome) // pindah dari home
            if (old.isNotEmpty()) { // app yang tergeser dikembalikan ke home kalau muat
                val pos = HomeLayout.findNearest(items, prefs.homeCols, prefs.homeRows, 1, 1, 0, 0)
                if (pos != null) items.add(HomeItem(HomeItem.KIND_APP, old, pos[0], pos[1]))
            }
            prefs.saveHomeItems(items)
        }
        haptic(confirmConst())
        renderHome()
        renderDock()
    }

    private fun dropOnDock(slot: Int): Boolean {
        val p = drag ?: return false
        if (p.kind != HomeItem.KIND_APP) return false
        if (p.dockSlot == slot) { // dilepas di slot semula -> menu
            val anchor = dockSlots[slot]
            anchor.post { showDockMenu(anchor, p.key) }
            return true
        }
        placeInDock(p.key, slot)
        return true
    }

    private fun addToDock(app: AppInfo) {
        val dock = prefs.dockApps()
        if (dock.contains(app.key)) { closeDrawer(); return }
        val slot = dock.indexOfFirst { it.isEmpty() }
        if (slot < 0) { haptic(rejectConst()); toast(R.string.dock_full); return }
        placeInDock(app.key, slot)
        closeDrawer()
    }

    private fun showDockMenu(anchor: View, key: String) {
        PopupMenu(popupCtx(), anchor).apply {
            menu.add(0, 1, 0, R.string.remove_from_dock)
            menu.add(0, 2, 0, R.string.app_info)
            menu.add(0, 3, 0, R.string.uninstall)
            setOnMenuItemClickListener {
                when (it.itemId) {
                    1 -> { removeFromDock(key); renderDock() }
                    2 -> openAppInfo(pkgOf(key))
                    3 -> uninstall(pkgOf(key))
                }
                true
            }
        }.show()
    }

    private fun showDockPicker(slot: Int) {
        if (!appsLoaded || apps.isEmpty()) return
        val list = apps
        val adapter = object : ArrayAdapter<AppInfo>(ContextThemeWrapper(this, dialogRes()), R.layout.item_widget, list) {
            override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
                val v = convertView ?: LayoutInflater.from(context).inflate(R.layout.item_widget, parent, false)
                val a = getItem(position)!!
                v.findViewById<ImageView>(R.id.widget_icon).setImageDrawable(a.icon)
                v.findViewById<TextView>(R.id.widget_title).text = a.label
                v.findViewById<TextView>(R.id.widget_size).visibility = View.GONE
                return v
            }
        }
        AlertDialog.Builder(this, dialogRes()).setTitle(R.string.choose_app)
            .setAdapter(adapter) { _, i -> placeInDock(list[i].key, slot) }
            .show()
    }

}
