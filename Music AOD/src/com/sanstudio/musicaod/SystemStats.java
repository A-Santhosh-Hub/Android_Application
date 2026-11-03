package com.sanstudio.musicaod;

import android.app.ActivityManager;
import android.app.AppOpsManager;
import android.app.usage.UsageEvents;
import android.app.usage.UsageStats;
import android.app.usage.UsageStatsManager;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.TrafficStats;
import android.os.BatteryManager;
import android.os.Build;
import android.os.Environment;
import android.os.PowerManager;
import android.os.Process;
import android.os.StatFs;
import android.os.SystemClock;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Phone details for the charging screen, all from public APIs. Other apps' running processes are hidden from
 * apps since Android 5.1, so "what's running" comes from usage stats instead: recently used apps, and apps
 * running a foreground service right now. That part needs the user to switch on Usage access.
 */
final class SystemStats {
    static final class App {
        final String label;
        final long lastUsed;
        final boolean background;

        App(String label, long lastUsed, boolean background) {
            this.label = label;
            this.lastUsed = lastUsed;
            this.background = background;
        }
    }

    // Fast-changing values, sampled every second.
    volatile long ramTotal, ramAvail;
    volatile int cpuMaxMhz = -1, cpuAvgMhz = -1;
    volatile int thermalStatus;
    volatile float thermalHeadroom = Float.NaN;
    volatile String network = "Offline";
    volatile long downBps, upBps;

    // Slow-changing values, sampled every few seconds off the main thread.
    volatile long storageTotal, storageFree;
    volatile boolean usageAccess;
    volatile List<App> apps = Collections.emptyList();
    volatile int backgroundCount;

    // Fixed for the session.
    final int cores = Runtime.getRuntime().availableProcessors();
    final String chip = Build.SOC_MODEL;
    final String android = "Android " + Build.VERSION.RELEASE;
    final String kernel = shortKernel(System.getProperty("os.version", ""));
    int cycles = -1;
    String health = "—";

    private final Context context;
    private long lastRx = -1, lastTx = -1, lastNetAt;
    private long lastHeadroomAt;

    SystemStats(Context c) {
        context = c.getApplicationContext();
        Intent b = context.registerReceiver(null, new IntentFilter(Intent.ACTION_BATTERY_CHANGED));
        if (b != null) {
            cycles = b.getIntExtra(BatteryManager.EXTRA_CYCLE_COUNT, -1);
            health = healthName(b.getIntExtra(BatteryManager.EXTRA_HEALTH, 0));
        }
    }

    /** Cheap reads; call about once a second on the main thread. */
    void sampleFast() {
        ActivityManager.MemoryInfo mi = new ActivityManager.MemoryInfo();
        context.getSystemService(ActivityManager.class).getMemoryInfo(mi);
        ramTotal = mi.totalMem;
        ramAvail = mi.availMem;

        readCpu();

        PowerManager pm = context.getSystemService(PowerManager.class);
        thermalStatus = pm.getCurrentThermalStatus();
        long now = SystemClock.elapsedRealtime();
        if (now - lastHeadroomAt >= 2000) { // the API returns NaN if asked more often than about once a second
            lastHeadroomAt = now;
            float h = pm.getThermalHeadroom(10);
            if (!Float.isNaN(h)) thermalHeadroom = h;
        }

        readNetwork(now);
    }

    /** Heavier reads (storage, usage stats); call every few seconds from a background thread. */
    void sampleSlow() {
        StatFs fs = new StatFs(Environment.getDataDirectory().getPath());
        storageTotal = fs.getTotalBytes();
        storageFree = fs.getAvailableBytes();

        AppOpsManager ops = context.getSystemService(AppOpsManager.class);
        usageAccess = ops.unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS, Process.myUid(),
                context.getPackageName()) == AppOpsManager.MODE_ALLOWED;
        if (usageAccess) readApps();
    }

    private void readCpu() {
        int max = -1;
        long sum = 0;
        int read = 0;
        for (int i = 0; i < cores; i++) {
            try (BufferedReader r = new BufferedReader(
                    new FileReader("/sys/devices/system/cpu/cpu" + i + "/cpufreq/scaling_cur_freq"))) {
                int mhz = Integer.parseInt(r.readLine().trim()) / 1000;
                max = Math.max(max, mhz);
                sum += mhz;
                read++;
            } catch (IOException | RuntimeException ignored) {
                // Offline core, or this phone doesn't let apps read CPU speeds.
            }
        }
        cpuMaxMhz = max;
        cpuAvgMhz = read > 0 ? (int) (sum / read) : -1;
    }

    private void readNetwork(long now) {
        ConnectivityManager cm = context.getSystemService(ConnectivityManager.class);
        Network n = cm.getActiveNetwork();
        NetworkCapabilities caps = n != null ? cm.getNetworkCapabilities(n) : null;
        if (caps == null) network = "Offline";
        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)) network = "Wi-Fi";
        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)) network = "Mobile data";
        else if (caps.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)) network = "Ethernet";
        else network = "Connected";
        if (caps != null && caps.hasTransport(NetworkCapabilities.TRANSPORT_VPN)) network += " + VPN";

        long rx = TrafficStats.getTotalRxBytes(), tx = TrafficStats.getTotalTxBytes();
        if (rx >= 0 && lastRx >= 0 && now > lastNetAt) {
            float secs = (now - lastNetAt) / 1000f;
            downBps = (long) ((rx - lastRx) / secs);
            upBps = (long) ((tx - lastTx) / secs);
        }
        lastRx = rx;
        lastTx = tx;
        lastNetAt = now;
    }

    private void readApps() {
        UsageStatsManager usm = context.getSystemService(UsageStatsManager.class);
        PackageManager pm = context.getPackageManager();
        long now = System.currentTimeMillis();

        // Apps running a foreground service right now (music, navigation, downloads...).
        Set<String> running = new HashSet<>();
        UsageEvents events = usm.queryEvents(now - 6 * 3_600_000L, now);
        UsageEvents.Event e = new UsageEvents.Event();
        while (events.hasNextEvent()) {
            events.getNextEvent(e);
            String key = e.getPackageName() + "/" + e.getClassName();
            if (e.getEventType() == UsageEvents.Event.FOREGROUND_SERVICE_START) running.add(key);
            else if (e.getEventType() == UsageEvents.Event.FOREGROUND_SERVICE_STOP) running.remove(key);
        }
        Set<String> backgroundPkgs = new HashSet<>();
        for (String key : running) backgroundPkgs.add(key.substring(0, key.indexOf('/')));
        backgroundPkgs.remove(context.getPackageName());

        // Recently used apps, newest first, skipping the launcher and ourselves.
        ResolveInfo home = pm.resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME), 0);
        String launcher = home != null ? home.activityInfo.packageName : "";
        Map<String, UsageStats> recent = usm.queryAndAggregateUsageStats(now - 3 * 3_600_000L, now);
        List<UsageStats> sorted = new ArrayList<>(recent.values());
        sorted.sort((a, b) -> Long.compare(b.getLastTimeUsed(), a.getLastTimeUsed()));

        List<App> list = new ArrayList<>();
        Set<String> shown = new HashSet<>();
        for (String pkg : backgroundPkgs) { // background ones first: they're what's running now
            String label = label(pm, pkg);
            if (label != null && shown.add(pkg)) list.add(new App(label, now, true));
        }
        for (UsageStats u : sorted) {
            if (list.size() >= 6) break;
            String pkg = u.getPackageName();
            if (u.getTotalTimeInForeground() <= 0 || pkg.equals(launcher) || pkg.equals(context.getPackageName())
                    || pkg.equals("com.android.systemui")) continue;
            String label = label(pm, pkg);
            if (label != null && shown.add(pkg)) list.add(new App(label, u.getLastTimeUsed(), false));
        }
        apps = list;
        backgroundCount = backgroundPkgs.size();
    }

    /** Null for packages we can't see (only launcher apps are visible to us) or that have no label. */
    private static String label(PackageManager pm, String pkg) {
        try {
            return pm.getApplicationLabel(pm.getApplicationInfo(pkg, 0)).toString();
        } catch (PackageManager.NameNotFoundException e) {
            return null;
        }
    }

    static String thermalName(int status) {
        switch (status) {
            case PowerManager.THERMAL_STATUS_NONE: return "Normal";
            case PowerManager.THERMAL_STATUS_LIGHT: return "Warm";
            case PowerManager.THERMAL_STATUS_MODERATE: return "Hot";
            case PowerManager.THERMAL_STATUS_SEVERE: return "Very hot";
            default: return "Critical";
        }
    }

    private static String healthName(int h) {
        switch (h) {
            case BatteryManager.BATTERY_HEALTH_GOOD: return "Good";
            case BatteryManager.BATTERY_HEALTH_OVERHEAT: return "Overheating";
            case BatteryManager.BATTERY_HEALTH_DEAD: return "Dead";
            case BatteryManager.BATTERY_HEALTH_OVER_VOLTAGE: return "Over voltage";
            case BatteryManager.BATTERY_HEALTH_COLD: return "Cold";
            default: return "Unknown";
        }
    }

    private static String shortKernel(String v) {
        int dash = v.indexOf('-');
        return dash > 0 ? v.substring(0, dash) : v;
    }
}
