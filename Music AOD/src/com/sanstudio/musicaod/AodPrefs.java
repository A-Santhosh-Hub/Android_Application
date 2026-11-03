package com.sanstudio.musicaod;

import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;

/** Every user setting, loaded from and saved to SharedPreferences. */
final class AodPrefs {
    private static final String FILE = "prefs";

    static final String[] PALETTE_NAMES = {"Aurora", "Sunset", "Ocean", "Lime", "Mono", "Album art", "Custom"};
    static final int[][] PALETTES = {
            {0xFF22D3EE, 0xFFE879F9},
            {0xFFFBBF24, 0xFFF43F5E},
            {0xFF38BDF8, 0xFF6366F1},
            {0xFFA3E635, 0xFF14B8A6},
            {0xFFFFFFFF, 0xFF9CA3AF},
    };
    static final int PALETTE_ALBUM = 5;
    static final int PALETTE_CUSTOM = 6;

    boolean musicEnabled = true;    // music visualizer when the screen turns off
    boolean chargeAnim = true;      // charging animation when the charger is plugged in
    boolean chargeLockOnly = true;  // only when the phone is locked or the screen is off
    int chargeSeconds = 10;         // how long it stays; 0 = until unplugged
    int chargeDecimals = 2;
    int chargeStyle = ChargingView.STYLE_HUD;
    boolean chargeDetails = true;   // phone details and active apps on the charging screen
    boolean island = false;
    boolean rightEdge = true;
    boolean leftEdge = false;
    int islandStyle = 1;        // 0 = bars around the camera, 1 = album art + bars (iPhone style)
    int palette = 0;
    int customHue1 = 190;
    int customHue2 = 300;
    int thickness = 50;         // % of each bar's slot that the bar fills
    int barLength = 72;         // dp, longest an edge bar can grow
    int bands = 14;             // bars per half of an edge strip
    int sensitivity = 100;      // %
    boolean rhythmMode = false; // scripted beat instead of real audio; needs no microphone
    int stripLength = 36;       // % of screen height
    int stripOffset = 0;        // % of screen height, positive moves down
    int brightness = 10;        // %
    boolean minimal = false;    // visualizer only: no clock, song details, art or buttons
    boolean showClock = true;
    boolean showSong = true;
    boolean showArt = true;
    boolean showProgress = true;
    boolean showControls = true;
    int closeDelay = 5;         // seconds after music stops

    static AodPrefs load(Context context) {
        SharedPreferences p = context.getSharedPreferences(FILE, Context.MODE_PRIVATE);
        AodPrefs a = new AodPrefs();
        a.musicEnabled = p.getBoolean("musicEnabled", a.musicEnabled);
        a.chargeAnim = p.getBoolean("chargeAnim", a.chargeAnim);
        a.chargeLockOnly = p.getBoolean("chargeLockOnly", a.chargeLockOnly);
        a.chargeSeconds = p.getInt("chargeSeconds", a.chargeSeconds);
        a.chargeDecimals = p.getInt("chargeDecimals", a.chargeDecimals);
        a.chargeStyle = p.getInt("chargeStyle", a.chargeStyle);
        a.chargeDetails = p.getBoolean("chargeDetails", a.chargeDetails);
        a.island = p.getBoolean("island", a.island);
        a.rightEdge = p.getBoolean("rightEdge", a.rightEdge);
        a.leftEdge = p.getBoolean("leftEdge", a.leftEdge);
        a.islandStyle = p.getInt("islandStyle", a.islandStyle);
        a.palette = p.getInt("palette", a.palette);
        a.customHue1 = p.getInt("customHue1", a.customHue1);
        a.customHue2 = p.getInt("customHue2", a.customHue2);
        a.thickness = p.getInt("thickness", a.thickness);
        a.barLength = p.getInt("barLength", a.barLength);
        a.bands = p.getInt("bands", a.bands);
        a.sensitivity = p.getInt("sensitivity", a.sensitivity);
        a.rhythmMode = p.getBoolean("rhythmMode", a.rhythmMode);
        a.stripLength = p.getInt("stripLength", a.stripLength);
        a.stripOffset = p.getInt("stripOffset", a.stripOffset);
        a.brightness = p.getInt("brightness", a.brightness);
        a.minimal = p.getBoolean("minimal", a.minimal);
        a.showClock = p.getBoolean("showClock", a.showClock);
        a.showSong = p.getBoolean("showSong", a.showSong);
        a.showArt = p.getBoolean("showArt", a.showArt);
        a.showProgress = p.getBoolean("showProgress", a.showProgress);
        a.showControls = p.getBoolean("showControls", a.showControls);
        a.closeDelay = p.getInt("closeDelay", a.closeDelay);
        return a;
    }

    void save(Context context) {
        context.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit()
                .putBoolean("musicEnabled", musicEnabled)
                .putBoolean("chargeAnim", chargeAnim)
                .putBoolean("chargeLockOnly", chargeLockOnly)
                .putInt("chargeSeconds", chargeSeconds)
                .putInt("chargeDecimals", chargeDecimals)
                .putInt("chargeStyle", chargeStyle)
                .putBoolean("chargeDetails", chargeDetails)
                .putBoolean("island", island)
                .putBoolean("rightEdge", rightEdge)
                .putBoolean("leftEdge", leftEdge)
                .putInt("islandStyle", islandStyle)
                .putInt("palette", palette)
                .putInt("customHue1", customHue1)
                .putInt("customHue2", customHue2)
                .putInt("thickness", thickness)
                .putInt("barLength", barLength)
                .putInt("bands", bands)
                .putInt("sensitivity", sensitivity)
                .putBoolean("rhythmMode", rhythmMode)
                .putInt("stripLength", stripLength)
                .putInt("stripOffset", stripOffset)
                .putInt("brightness", brightness)
                .putBoolean("minimal", minimal)
                .putBoolean("showClock", showClock)
                .putBoolean("showSong", showSong)
                .putBoolean("showArt", showArt)
                .putBoolean("showProgress", showProgress)
                .putBoolean("showControls", showControls)
                .putInt("closeDelay", closeDelay)
                .apply();
    }

    /** The two gradient colours to draw with; albumColors is used for the Album art palette when available. */
    int[] colors(int[] albumColors) {
        if (palette == PALETTE_ALBUM) return albumColors != null ? albumColors : PALETTES[0];
        if (palette == PALETTE_CUSTOM) return new int[]{hueColor(customHue1), hueColor(customHue2)};
        return PALETTES[Math.max(0, Math.min(PALETTES.length - 1, palette))];
    }

    static int hueColor(int hue) {
        return Color.HSVToColor(new float[]{hue, 0.75f, 1f});
    }
}
