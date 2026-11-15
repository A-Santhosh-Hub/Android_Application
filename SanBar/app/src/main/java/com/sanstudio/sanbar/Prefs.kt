package com.sanstudio.sanbar

import android.content.Context
import android.content.SharedPreferences

/** The user's Customize choices. */
object Prefs {
    const val FX_OFF = "off"
    const val FX_SPIN = "spin"
    const val FX_CYCLE = "cycle"
    const val FX_BREATHE = "breathe"

    const val VIZ_OFF = "off"
    const val VIZ_ANIMATED = "animated"
    const val VIZ_REAL = "real"

    const val DEFAULT_ACCENT = 0xFF4ADE80.toInt()

    private const val K_ACCENT = "accent"
    private const val K_ALBUM = "album_color"
    private const val K_GLASS = "glass_alpha"
    private const val K_FX = "fx"
    private const val K_FX_FAST = "fx_fast"
    private const val K_VIZ = "viz"
    private const val K_STATS_CLEAR = "stats_transparent"
    private const val K_BAR_CLEAR = "bar_transparent"

    /** Music bar without its box: clean controls on the wallpaper. */
    fun barTransparent(c: Context) = sp(c).getBoolean(K_BAR_CLEAR, false)
    fun setBarTransparent(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_BAR_CLEAR, v).apply()

    /** SanStats without its box: only the rings and text on the wallpaper. */
    fun statsTransparent(c: Context) = sp(c).getBoolean(K_STATS_CLEAR, false)
    fun setStatsTransparent(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_STATS_CLEAR, v).apply()

    private fun sp(c: Context): SharedPreferences =
        c.applicationContext.getSharedPreferences("sanbar", Context.MODE_PRIVATE)

    fun accent(c: Context) = sp(c).getInt(K_ACCENT, DEFAULT_ACCENT)
    fun albumColor(c: Context) = sp(c).getBoolean(K_ALBUM, false)
    fun glassAlpha(c: Context) = sp(c).getInt(K_GLASS, 0xB3)
    fun fx(c: Context) = sp(c).getString(K_FX, FX_OFF) ?: FX_OFF
    fun fxFast(c: Context) = sp(c).getBoolean(K_FX_FAST, false)
    fun viz(c: Context) = sp(c).getString(K_VIZ, VIZ_ANIMATED) ?: VIZ_ANIMATED

    fun setAccent(c: Context, v: Int) = sp(c).edit().putInt(K_ACCENT, v).apply()
    fun setAlbumColor(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_ALBUM, v).apply()
    fun setGlassAlpha(c: Context, v: Int) = sp(c).edit().putInt(K_GLASS, v.coerceIn(0, 255)).apply()
    fun setFx(c: Context, v: String) = sp(c).edit().putString(K_FX, v).apply()
    fun setFxFast(c: Context, v: Boolean) = sp(c).edit().putBoolean(K_FX_FAST, v).apply()
    fun setViz(c: Context, v: String) = sp(c).edit().putString(K_VIZ, v).apply()

    fun register(c: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(c).registerOnSharedPreferenceChangeListener(l)

    fun unregister(c: Context, l: SharedPreferences.OnSharedPreferenceChangeListener) =
        sp(c).unregisterOnSharedPreferenceChangeListener(l)

    /** Changes whenever any setting changes (used to skip identical widget redraws). */
    fun signature(c: Context) = sp(c).all.toSortedMap().toString()
}
