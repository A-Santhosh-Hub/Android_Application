package com.sanstudio.sanbar

import android.app.ActivityManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.PowerManager
import android.os.SystemClock
import java.io.File

/**
 * Phone stats a normal app can read on this iQOO (verified by running as SanBar with `run-as`):
 * - /proc/stat (CPU usage) and cpufreq (GHz): readable on vivo's Android 16.
 * - RAM: ActivityManager.
 * - Temperature: battery temperature + Android's public thermal status / headroom.
 * GPU usage is NOT readable: MediaTek's GPU files are denied and vivo's own route
 * (vivo_daemon.service, found with REA in GameWatch) is hidden from normal apps.
 */
object StatsRepo {

    data class Stats(
        val cpuPct: Int,        // -1 = not known yet
        val cpuGhz: Float,
        val ramPct: Int,
        val ramUsedGb: Float,
        val ramTotalGb: Float,
        val tempC: Float,
        val thermalStatus: Int, // PowerManager.THERMAL_STATUS_*
        val heatPct: Int,       // 0..100 = how close to heat throttling, -1 unknown
    )

    private var prevTotal = 0L
    private var prevIdle = 0L
    private var prevSampleAt = 0L
    private var cpuPct = -1

    private var headroomAt = 0L
    private var heatPct = -1

    @Synchronized
    fun read(ctx: Context): Stats {
        updateCpu()
        val (ramPct, usedGb, totalGb) = ram(ctx)
        val pm = ctx.getSystemService(PowerManager::class.java)
        updateHeat(pm)
        val sticky = ctx.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val tempC = (sticky?.getIntExtra(BatteryManager.EXTRA_TEMPERATURE, 0) ?: 0) / 10f
        return Stats(
            cpuPct = cpuPct,
            cpuGhz = cpuGhz(),
            ramPct = ramPct,
            ramUsedGb = usedGb,
            ramTotalGb = totalGb,
            tempC = tempC,
            thermalStatus = pm?.currentThermalStatus ?: PowerManager.THERMAL_STATUS_NONE,
            heatPct = heatPct,
        )
    }

    /** The real app (untrusted_app) is denied /proc/stat even though `run-as` could read it. */
    private var procStatBlocked = false
    private val maxKhz = HashMap<Int, Long>()

    // ---- CPU: busy share between two /proc/stat samples, or core-speed load as fallback ----
    private fun updateCpu() {
        val now = SystemClock.elapsedRealtime()
        if (prevSampleAt != 0L && now - prevSampleAt < 1500) return // keep the last number
        if (procStatBlocked) {
            cpuPct = freqLoad()
            prevSampleAt = now
            return
        }
        if (prevSampleAt == 0L) {
            // First call: two quick samples so the widget doesn't start empty.
            val first = sample()
            if (first == null) {
                procStatBlocked = true
                cpuPct = freqLoad()
                prevSampleAt = now
                return
            }
            prevTotal = first.first
            prevIdle = first.second
            Thread.sleep(200)
        }
        val s = sample() ?: return
        val dTotal = s.first - prevTotal
        val dIdle = s.second - prevIdle
        if (dTotal > 0) cpuPct = (((dTotal - dIdle) * 100) / dTotal).toInt().coerceIn(0, 100)
        prevTotal = s.first
        prevIdle = s.second
        prevSampleAt = SystemClock.elapsedRealtime()
    }

    /** (total, idle) jiffies from the first line of /proc/stat. */
    private fun sample(): Pair<Long, Long>? = try {
        val parts = File("/proc/stat").bufferedReader().use { it.readLine() }
            .trim().split(Regex("\\s+")).drop(1).map { it.toLong() }
        val idle = parts[3] + parts[4] // idle + iowait
        val total = parts.take(8).sum()
        total to idle
    } catch (e: Exception) {
        android.util.Log.w("SanBar", "/proc/stat unreadable: $e")
        null
    }

    /**
     * CPU load estimate: average of (current speed / max speed) over all cores.
     * Phones speed cores up under load and down when idle, so this tracks real usage closely.
     */
    private fun freqLoad(): Int {
        var sum = 0f
        var n = 0
        for (c in 0 until Runtime.getRuntime().availableProcessors()) {
            val cur = readKhz("/sys/devices/system/cpu/cpu$c/cpufreq/scaling_cur_freq")
            val max = maxKhz.getOrPut(c) { readKhz("/sys/devices/system/cpu/cpu$c/cpufreq/cpuinfo_max_freq") }
            if (cur > 0 && max > 0) {
                sum += cur.toFloat() / max
                n++
            }
        }
        if (n == 0) android.util.Log.w("SanBar", "cpufreq unreadable too")
        return if (n == 0) -1 else (sum / n * 100).toInt().coerceIn(0, 100)
    }

    private fun readKhz(path: String): Long = try {
        File(path).readText().trim().toLong()
    } catch (e: Exception) {
        0L
    }

    /** Fastest current core clock, in GHz. */
    private fun cpuGhz(): Float {
        var maxKhz = 0L
        for (c in 0 until Runtime.getRuntime().availableProcessors()) {
            val khz = try {
                File("/sys/devices/system/cpu/cpu$c/cpufreq/scaling_cur_freq").readText().trim().toLong()
            } catch (e: Exception) {
                0L
            }
            if (khz > maxKhz) maxKhz = khz
        }
        return maxKhz / 1_000_000f
    }

    // ---- RAM ----
    private fun ram(ctx: Context): Triple<Int, Float, Float> {
        val am = ctx.getSystemService(ActivityManager::class.java)
        val mi = ActivityManager.MemoryInfo().also { am?.getMemoryInfo(it) }
        if (mi.totalMem <= 0) return Triple(0, 0f, 0f)
        val used = mi.totalMem - mi.availMem
        val gb = 1024f * 1024f * 1024f
        return Triple((used * 100 / mi.totalMem).toInt(), used / gb, mi.totalMem / gb)
    }

    // ---- Heat: Android's thermal headroom (1.0 = phone starts slowing down to cool) ----
    private fun updateHeat(pm: PowerManager?) {
        val now = SystemClock.elapsedRealtime()
        if (pm == null || now - headroomAt < 1100) return // Android allows ~1 call per second
        headroomAt = now
        val h = pm.getThermalHeadroom(0)
        if (!h.isNaN()) heatPct = (h * 100).toInt().coerceIn(0, 100)
    }
}
