package com.ega.project.vibelauncher

import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import com.google.android.material.slider.LabelFormatter
import com.google.android.material.slider.Slider
import com.google.android.material.switchmaterial.SwitchMaterial

/**
 * Pembangun UI Pengaturan. Semua warna & bentuk diambil dari [Themes], jadi layar pengaturan
 * otomatis mengikuti tema yang dipilih (Material / Liquid Glass Gelap / Liquid Glass Terang).
 *
 * Susunan: setiap SECTION = label kecil + satu kartu terpisah. Di dalam kartu ada baris-baris
 * (nav / switch / slider / segmented) yang dipisah garis tipis.
 */
class SettingsUi(private val ctx: Context, theme: String) {
    private val t = Themes.normalize(theme)
    private val d = ctx.resources.displayMetrics.density
    private val textColor = Themes.textPrimary(ctx, t)
    private val subColor = Themes.secondaryText(ctx, t)
    private val accent = Themes.accent(ctx, t)
    private val onAccent = Themes.onAccent(t)

    class SliderRow(val root: View, val slider: Slider)

    private fun dp(v: Int) = (v * d).toInt()

    private fun lp(w: Int = ViewGroup.LayoutParams.MATCH_PARENT, h: Int = ViewGroup.LayoutParams.WRAP_CONTENT, weight: Float = 0f) =
        LinearLayout.LayoutParams(w, h, weight)

    private fun label(size: Float, color: Int, bold: Boolean = false) = TextView(ctx).apply {
        textSize = size
        setTextColor(color)
        if (bold) typeface = Typeface.DEFAULT_BOLD
    }

    private fun rounded(color: Int, radiusDp: Int) = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radiusDp).toFloat()
    }

    private fun checkedList(on: Int, off: Int) = ColorStateList(
        arrayOf(intArrayOf(android.R.attr.state_checked), intArrayOf()), intArrayOf(on, off)
    )

    // ---------------------------------------------------------------- section & kartu

    /** Satu section: label kecil (opsional) + kartu. Mengembalikan kartu tempat baris-baris ditambahkan. */
    fun card(parent: ViewGroup, sectionLabel: Int? = null): LinearLayout {
        if (sectionLabel != null) {
            parent.addView(
                label(12f, if (Themes.isGlass(t)) subColor else accent, bold = true).apply {
                    setText(sectionLabel)
                    isAllCaps = true
                    letterSpacing = 0.08f
                    setPadding(dp(12), 0, dp(12), dp(8))
                },
                lp().apply { topMargin = dp(28) }
            )
        }
        val c = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            background = Themes.settingsCard(ctx, t, 24f)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        parent.addView(c, lp().apply { topMargin = if (sectionLabel == null) dp(16) else 0 })
        return c
    }

    fun divider(card: LinearLayout) {
        card.addView(
            View(ctx).apply { setBackgroundColor(Themes.divider(t)) },
            lp(h = d.toInt().coerceAtLeast(1)).apply { marginStart = dp(14); marginEnd = dp(14) }
        )
    }

    /** Teks penjelas kecil di dalam kartu. */
    fun note(card: LinearLayout, text: Int): TextView = note(card, ctx.getString(text))

    fun note(card: LinearLayout, text: CharSequence): TextView {
        val v = label(13f, subColor).apply {
            this.text = text
            setPadding(dp(14), dp(12), dp(14), dp(8))
        }
        card.addView(v, lp())
        return v
    }

    /** Aktifkan / redupkan satu blok beserta seluruh anak-anaknya. */
    fun setEnabledDeep(v: View, on: Boolean) {
        v.isEnabled = on
        v.alpha = if (on) 1f else 0.4f
        if (v is ViewGroup) for (i in 0 until v.childCount) setEnabledChildren(v.getChildAt(i), on)
    }

    private fun setEnabledChildren(v: View, on: Boolean) {
        v.isEnabled = on
        if (v is ViewGroup) for (i in 0 until v.childCount) setEnabledChildren(v.getChildAt(i), on)
    }

    // ---------------------------------------------------------------- baris

    private fun row(card: LinearLayout, clickable: Boolean): LinearLayout {
        val r = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            minimumHeight = dp(64)
            setPadding(dp(14), dp(12), dp(14), dp(12))
            if (clickable) {
                isClickable = true
                isFocusable = true
                background = Themes.rippleBg(t, 20f, d)
            }
        }
        card.addView(r, lp())
        return r
    }

    private class TextCol(val root: LinearLayout, val summary: TextView)

    private fun textColumn(title: CharSequence, summary: CharSequence?): TextCol {
        val col = LinearLayout(ctx).apply { orientation = LinearLayout.VERTICAL }
        col.addView(label(16f, textColor).apply {
            text = title
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }, lp())
        val s = label(13f, subColor).apply {
            text = summary ?: ""
            visibility = if (summary.isNullOrEmpty()) View.GONE else View.VISIBLE
        }
        col.addView(s, lp().apply { topMargin = dp(2) })
        return TextCol(col, s)
    }

    private fun tile(icon: Int) = ImageView(ctx).apply {
        setImageResource(icon)
        imageTintList = ColorStateList.valueOf(Themes.tileFg(ctx, t))
        background = rounded(Themes.tileBg(ctx, t), 12)
        setPadding(dp(9), dp(9), dp(9), dp(9))
        importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
    }

    /**
     * Baris dengan tile ikon, judul & ringkasan. [onClick] null = baris informasi (tanpa panah).
     * Mengembalikan TextView ringkasan supaya status bisa diperbarui.
     */
    fun navRow(card: LinearLayout, icon: Int, title: Int, summary: CharSequence?, onClick: (() -> Unit)?): TextView {
        val r = row(card, onClick != null)
        r.addView(tile(icon), lp(dp(40), dp(40)).apply { marginEnd = dp(14) })
        val col = textColumn(ctx.getString(title), summary)
        r.addView(col.root, lp(0, weight = 1f))
        if (onClick != null) {
            r.addView(
                ImageView(ctx).apply {
                    setImageResource(R.drawable.ic_chevron_right)
                    imageTintList = ColorStateList.valueOf(subColor)
                    importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                },
                lp(dp(22), dp(22)).apply { marginStart = dp(8) }
            )
            r.setOnClickListener { onClick() }
        }
        return col.summary
    }

    fun switchRow(card: LinearLayout, title: Int, summary: Int?, checked: Boolean, onChange: (Boolean) -> Unit): SwitchMaterial {
        val r = row(card, true)
        val col = textColumn(ctx.getString(title), summary?.let { ctx.getString(it) })
        r.addView(col.root, lp(0, weight = 1f).apply { marginEnd = dp(12) })
        val sw = SwitchMaterial(ctx)
        sw.setUseMaterialThemeColors(false) // harus dipanggil sebelum tint kustom
        sw.thumbTintList = checkedList(onAccent, subColor)
        sw.trackTintList = checkedList(accent, Themes.trackInactive(t))
        sw.isChecked = checked
        sw.setOnCheckedChangeListener { _, on -> onChange(on) }
        r.addView(sw, lp(ViewGroup.LayoutParams.WRAP_CONTENT))
        r.setOnClickListener { sw.toggle() }
        return sw
    }

    /** Baris slider: judul di kiri, nilai (chip) di kanan, slider di bawahnya. */
    fun sliderRow(card: LinearLayout, title: Int, min: Int, max: Int, value: Int, onChange: (Int) -> Unit): SliderRow {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(14), dp(14), dp(6))
        }
        val head = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
        }
        head.addView(label(16f, textColor).apply {
            setText(title)
            typeface = Typeface.create("sans-serif-medium", Typeface.NORMAL)
        }, lp(0, weight = 1f))
        val v0 = value.coerceIn(min, max)
        val valueChip = label(14f, Themes.tileFg(ctx, t), bold = true).apply {
            text = v0.toString()
            gravity = Gravity.CENTER
            minWidth = dp(40)
            background = rounded(Themes.tileBg(ctx, t), 12)
            setPadding(dp(12), dp(5), dp(12), dp(5))
        }
        head.addView(valueChip, lp(ViewGroup.LayoutParams.WRAP_CONTENT))
        box.addView(head, lp())

        val slider = Slider(ctx).apply {
            valueFrom = min.toFloat()
            valueTo = max.toFloat()
            stepSize = 1f
            this.value = v0.toFloat()
            labelBehavior = LabelFormatter.LABEL_GONE
            setTickVisible(false)
            thumbTintList = ColorStateList.valueOf(accent)
            haloTintList = ColorStateList.valueOf(Themes.ripple(t))
            trackActiveTintList = ColorStateList.valueOf(accent)
            trackInactiveTintList = ColorStateList.valueOf(Themes.trackInactive(t))
            addOnChangeListener { _, v, _ ->
                val n = v.toInt()
                valueChip.text = n.toString()
                onChange(n)
            }
        }
        box.addView(slider, lp())
        card.addView(box, lp())
        return SliderRow(box, slider)
    }

    /** Pilihan segmented (mis. Bawaan | Google). Mengembalikan wadahnya. */
    fun segmented(card: LinearLayout, options: List<Int>, selected: Int, onSelect: (Int) -> Unit): LinearLayout {
        val wrap = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            background = rounded(Themes.tileBg(ctx, t), 16)
            setPadding(dp(4), dp(4), dp(4), dp(4))
        }
        val items = ArrayList<TextView>()
        fun paint(sel: Int) {
            items.forEachIndexed { i, v ->
                val on = i == sel
                v.background = if (on) rounded(accent, 12) else null
                v.setTextColor(if (on) onAccent else textColor)
                v.typeface = if (on) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
            }
        }
        options.forEachIndexed { i, res ->
            val v = label(14f, textColor).apply {
                setText(res)
                gravity = Gravity.CENTER
                setPadding(dp(8), dp(12), dp(8), dp(12))
                isClickable = true
                isFocusable = true
                setOnClickListener { paint(i); onSelect(i) }
            }
            items.add(v)
            wrap.addView(v, lp(0, weight = 1f))
        }
        paint(selected)
        card.addView(wrap, lp().apply { setMargins(dp(10), dp(4), dp(10), dp(10)) })
        return wrap
    }

    /** Kotak peringatan dengan tombol aksi (mis. izin akses wallpaper). */
    fun callout(card: LinearLayout, message: Int, action: Int, onClick: () -> Unit): LinearLayout {
        val box = LinearLayout(ctx).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(14), dp(4), dp(14), dp(14))
        }
        box.addView(label(13f, subColor).apply { setText(message) }, lp())
        box.addView(
            label(14f, onAccent, bold = true).apply {
                setText(action)
                gravity = Gravity.CENTER
                background = rounded(accent, 14)
                setPadding(dp(18), dp(10), dp(18), dp(10))
                isClickable = true
                isFocusable = true
                setOnClickListener { onClick() }
            },
            lp(ViewGroup.LayoutParams.WRAP_CONTENT).apply { topMargin = dp(10) }
        )
        card.addView(box, lp())
        return box
    }

    // ---------------------------------------------------------------- pemilih tema

    /** Tiga kartu pratinjau tema; ketuk untuk memilih. */
    fun themePicker(card: LinearLayout, selected: String, onSelect: (String) -> Unit) {
        val options = listOf(
            Themes.MATERIAL to R.string.theme_material_short,
            Themes.GLASS_DARK to R.string.theme_glass_dark_short,
            Themes.GLASS_LIGHT to R.string.theme_glass_light_short
        )
        val strip = LinearLayout(ctx).apply {
            orientation = LinearLayout.HORIZONTAL
            setPadding(dp(6), dp(4), dp(6), dp(10))
        }
        for ((id, name) in options) {
            val sel = id == selected
            val tile = LinearLayout(ctx).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(dp(6), dp(6), dp(6), dp(8))
                isClickable = true
                isFocusable = true
                background = GradientDrawable().apply {
                    cornerRadius = dp(20).toFloat()
                    setColor(if (sel) Themes.tileBg(ctx, t) else Color.TRANSPARENT)
                    setStroke(dp(if (sel) 2 else 1), if (sel) accent else Themes.divider(t))
                }
                setOnClickListener { if (!sel) onSelect(id) }
            }
            tile.addView(preview(id), lp(h = dp(112)))
            tile.addView(
                label(13f, if (sel) textColor else subColor, bold = sel).apply {
                    setText(name)
                    gravity = Gravity.CENTER
                    maxLines = 1
                },
                lp().apply { topMargin = dp(8) }
            )
            strip.addView(tile, lp(0, weight = 1f).apply { marginStart = dp(4); marginEnd = dp(4) })
        }
        card.addView(strip, lp())
    }

    /** Mini home screen: wallpaper warna-warni, beberapa ikon, dan dock bergaya tema [id]. */
    private fun preview(id: String): View {
        val f = FrameLayout(ctx).apply {
            background = GradientDrawable(
                GradientDrawable.Orientation.TL_BR,
                intArrayOf(0xFF3B5BDB.toInt(), 0xFF9C4FD6.toInt(), 0xFFF28C6B.toInt())
            ).apply { cornerRadius = dp(14).toFloat() }
        }
        val icons = LinearLayout(ctx).apply { orientation = LinearLayout.HORIZONTAL }
        repeat(3) {
            icons.addView(
                View(ctx).apply { background = rounded(0xE6FFFFFF.toInt(), 5) },
                lp(dp(16), dp(16)).apply { marginEnd = dp(6) }
            )
        }
        f.addView(icons, FrameLayout.LayoutParams(
            ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT
        ).apply {
            gravity = Gravity.TOP or Gravity.START
            setMargins(dp(10), dp(12), 0, 0)
        })
        f.addView(
            View(ctx).apply { background = Themes.panel(ctx, id, 10f) },
            FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, dp(26)).apply {
                gravity = Gravity.BOTTOM
                setMargins(dp(8), 0, dp(8), dp(8))
            }
        )
        return f
    }
}
