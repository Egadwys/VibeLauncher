package com.ega.project.vibelauncher

import android.appwidget.AppWidgetHost
import android.appwidget.AppWidgetHostView
import android.appwidget.AppWidgetProviderInfo
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.view.MotionEvent
import android.view.ViewConfiguration
import kotlin.math.abs

class LauncherAppWidgetHost(ctx: Context, id: Int) : AppWidgetHost(ctx, id) {
    override fun onCreateView(
        context: Context, appWidgetId: Int, appWidget: AppWidgetProviderInfo?
    ): AppWidgetHostView = LauncherWidgetHostView(context)
}

/**
 * Host view widget. Kalau [onLongPress] di-set, tekan-lama di atas widget (yang biasanya "dimakan"
 * oleh widget itu sendiri) dicegat supaya bisa dipakai untuk memulai drag.
 */
class LauncherWidgetHostView(ctx: Context) : AppWidgetHostView(ctx) {
    var onLongPress: (() -> Unit)? = null

    private val uiHandler = Handler(Looper.getMainLooper())
    private var fired = false
    private var downX = 0f
    private var downY = 0f
    private val slop = ViewConfiguration.get(ctx).scaledTouchSlop
    private val longPress = Runnable { fired = true; onLongPress?.invoke() }

    override fun onInterceptTouchEvent(ev: MotionEvent): Boolean {
        if (onLongPress == null) return false
        when (ev.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                fired = false
                downX = ev.x; downY = ev.y
                uiHandler.postDelayed(longPress, ViewConfiguration.getLongPressTimeout().toLong())
            }
            MotionEvent.ACTION_MOVE ->
                if (abs(ev.x - downX) > slop || abs(ev.y - downY) > slop) uiHandler.removeCallbacks(longPress)
            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> uiHandler.removeCallbacks(longPress)
        }
        return fired
    }

    override fun onTouchEvent(ev: MotionEvent): Boolean = if (fired) true else super.onTouchEvent(ev)

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        if (w > 0 && h > 0) {
            val d = resources.displayMetrics.density
            val wd = (w / d).toInt(); val hd = (h / d).toInt()
            updateAppWidgetSize(null, wd, hd, wd, hd)
        }
    }

    override fun onDetachedFromWindow() {
        uiHandler.removeCallbacks(longPress)
        super.onDetachedFromWindow()
    }
}
