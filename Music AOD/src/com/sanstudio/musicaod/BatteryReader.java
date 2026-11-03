package com.sanstudio.musicaod;

import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.BatteryManager;
import android.os.SystemClock;

/**
 * Reads the battery through the public BatteryManager API and turns the fuel gauge's charge counter into a
 * percentage with decimals. vivo's charging screen does the same maths from the gauge's remaining and full
 * capacity (bat_rm / bat_fcc), which apps can't read; here the full capacity is learned instead, from how much
 * charge it takes to climb one whole percent.
 */
final class BatteryReader {
    int level;
    boolean plugged;
    int plugType;
    boolean charging;
    int voltageMv;
    float tempC;
    int currentMa;
    boolean hasCounter;
    long counterUah;
    long fullUah;
    float precise;
    long timeToFullMs = -1;

    private final Context context;
    private final BatteryManager manager;
    private final SharedPreferences store;
    private int lastLevel = -1;
    private int anchorLevel;     // whole percent the anchor belongs to
    private long anchorCounter;  // counter value at the start of anchorLevel
    private boolean anchorExact; // true when the anchor was taken the moment the level ticked up
    private long counterChangedAt;
    private boolean counterRising; // a weak USB port can report "charging" while the counter still falls

    BatteryReader(Context c) {
        context = c.getApplicationContext();
        manager = context.getSystemService(BatteryManager.class);
        store = context.getSharedPreferences("battery", Context.MODE_PRIVATE);
        fullUah = store.getLong("fullUah", 0);
        anchorLevel = store.getInt("anchorLevel", -1);
        anchorCounter = store.getLong("anchorCounter", 0);
        anchorExact = store.getBoolean("anchorExact", false);
    }

    void read() {
        Intent b = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            int raw = b.getIntExtra(BatteryManager.EXTRA_LEVEL, -1);
            int scale = b.getIntExtra(BatteryManager.EXTRA_SCALE, 100);
            level = raw >= 0 && scale > 0 ? Math.round(raw * 100f / scale)
                    : manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY);
            plugType = b.getIntExtra(BatteryManager.EXTRA_PLUGGED, 0);
            plugged = plugType != 0;
            charging = b.getIntExtra(BatteryManager.EXTRA_STATUS, -1) == BatteryManager.BATTERY_STATUS_CHARGING;
            voltageMv = b.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0);
            tempC = b.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) / 10f;
        }

        int cc = manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CHARGE_COUNTER);
        boolean valid = cc > 0; // unsupported reads as Integer.MIN_VALUE or 0
        if (valid && cc != counterUah) {
            counterRising = counterUah > 0 && cc > counterUah;
            counterChangedAt = SystemClock.elapsedRealtime();
        }
        hasCounter = valid;
        counterUah = valid ? cc : 0;
        currentMa = toMilliamps(manager.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW));
        timeToFullMs = charging ? manager.computeChargeTimeRemaining() : -1;

        learn();
        precise = estimate();
    }

    /** The live percentage: the last gauge reading, moved forward by the real charging current since then. */
    float live() {
        if (!hasCounter || !charging || !counterRising || level >= 100 || fullUah <= 0) return precise;
        float hours = (SystemClock.elapsedRealtime() - counterChangedAt) / 3_600_000f;
        float gained = currentMa * 1000f * hours * 100f / fullUah;
        return Math.min(Math.min(level + 0.99f, precise + 0.25f), precise + gained);
    }

    float watts() {
        return voltageMv / 1000f * currentMa / 1000f;
    }

    private void learn() {
        if (!hasCounter || level <= 0 || level >= 100) {
            lastLevel = level;
            return;
        }
        if (fullUah <= 0) {
            fullUah = Math.round(counterUah * 100.0 / (level + 0.5));
            store.edit().putLong("fullUah", fullUah).apply();
        }
        if (lastLevel > 0 && level != lastLevel) {
            if (level == lastLevel + 1) {
                // A full step from one exact tick to the next measures 1% of the capacity directly.
                if (anchorExact && anchorLevel == lastLevel) {
                    long measured = (counterUah - anchorCounter) * 100;
                    if (measured > fullUah / 2 && measured < fullUah * 3 / 2) {
                        fullUah = Math.round(fullUah * 0.7 + measured * 0.3);
                    }
                }
                setAnchor(level, counterUah, true);
            } else if (level == lastLevel - 1) {
                // Dropping into this percent means we're at its top end.
                setAnchor(level, counterUah - fullUah * 99 / 10_000, false);
            } else {
                setAnchor(-1, 0, false);
            }
        }
        // No anchor for this percent yet (or a stale one from an earlier charge): assume we're mid-way, so the
        // decimals move with the real counter from .50 instead of sitting at .00 or .99 until the next tick.
        float fromAnchor = level + (counterUah - anchorCounter) * 100f / fullUah;
        if (anchorLevel != level || fromAnchor < level - 0.05f || fromAnchor > level + 1.05f) {
            setAnchor(level, counterUah - fullUah * 50 / 10_000, false);
        }
        lastLevel = level;
    }

    private float estimate() {
        if (level >= 100) return 100f;
        if (!hasCounter || fullUah <= 0) return level;
        float p = level + (counterUah - anchorCounter) * 100f / fullUah;
        return Math.max(level, Math.min(level + 0.99f, p)); // never disagree with the system's whole percent
    }

    private void setAnchor(int lvl, long counter, boolean exact) {
        anchorLevel = lvl;
        anchorCounter = counter;
        anchorExact = exact;
        store.edit()
                .putLong("fullUah", fullUah)
                .putInt("anchorLevel", lvl)
                .putLong("anchorCounter", counter)
                .putBoolean("anchorExact", exact)
                .apply();
    }

    /** CURRENT_NOW is meant to be microamps, but some phones report milliamps or a negative sign. */
    private static int toMilliamps(int raw) {
        if (raw == Integer.MIN_VALUE) return 0;
        long v = Math.abs((long) raw);
        return (int) (v >= 20_000 ? v / 1000 : v);
    }
}
