package com.sanstudio.sanbar

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.bluetooth.BluetoothProfile
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.BatteryManager
import android.util.Log
import java.util.concurrent.ConcurrentHashMap

/** Phone battery + the connected Bluetooth headphones' battery. */
@SuppressLint("MissingPermission") // every Bluetooth call is behind hasBluetoothPermission()
object BatteryRepo {

    /** [fast] = vivo FlashCharge engine active, or the charger delivers 15 W or more. */
    data class Phone(val level: Int, val charging: Boolean, val fast: Boolean, val watts: Int)

    private const val FAST_WATTS = 15.0

    /** [level] is -1 when the headphones don't report a battery level. */
    data class Headphones(val name: String, val level: Int)

    /** Hidden Bluetooth broadcast + extra (the same ones vivo's Device battery widget listens to). */
    const val ACTION_BT_BATTERY_LEVEL_CHANGED = "android.bluetooth.device.action.BATTERY_LEVEL_CHANGED"
    const val EXTRA_BT_BATTERY_LEVEL = "android.bluetooth.device.extra.BATTERY_LEVEL"

    /** Levels received from [ACTION_BT_BATTERY_LEVEL_CHANGED], by device address. */
    val reportedLevels = ConcurrentHashMap<String, Int>()

    /** Audio profile connections (A2DP = music, HEADSET = calls), opened once and kept. */
    private val proxies = ConcurrentHashMap<Int, BluetoothProfile>()
    private var proxiesRequested = false

    fun phone(ctx: Context): Phone {
        val bm = ctx.getSystemService(BatteryManager::class.java)
        val level = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CAPACITY) ?: 0
        // Last battery broadcast (sticky): status, voltage and vivo's own "engine" extra.
        val sticky = ctx.applicationContext.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val status = sticky?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = bm?.isCharging == true || status == BatteryManager.BATTERY_STATUS_CHARGING

        // Found with REA in com.vivo.devicepower: PhoneBatteryHelper reads intent extra "engine"
        // from BATTERY_CHANGED; non-zero means vivo's FlashCharge engine is running.
        val engine = sticky?.getIntExtra("engine", 0) ?: 0
        val millivolts = sticky?.getIntExtra(BatteryManager.EXTRA_VOLTAGE, 0) ?: 0
        var current = bm?.getIntProperty(BatteryManager.BATTERY_PROPERTY_CURRENT_NOW)?.toLong() ?: 0L
        if (current == Int.MIN_VALUE.toLong()) current = 0
        current = kotlin.math.abs(current)
        // Most phones report µA; some report mA.
        val amps = if (current < 20_000) current / 1_000.0 else current / 1_000_000.0
        val watts = if (charging) amps * millivolts / 1_000.0 else 0.0
        val fast = charging && (engine > 0 || watts >= FAST_WATTS)

        return Phone(level.coerceIn(0, 100), charging, fast, watts.toInt())
    }

    fun hasBluetoothPermission(ctx: Context) =
        ctx.checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED

    /**
     * Opens the A2DP and HEADSET profile proxies. They arrive asynchronously, so [onReady]
     * runs once each is connected (the caller redraws the widget then).
     */
    fun init(ctx: Context, onReady: () -> Unit) {
        if (proxiesRequested || !hasBluetoothPermission(ctx)) return
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return
        proxiesRequested = true
        val listener = object : BluetoothProfile.ServiceListener {
            override fun onServiceConnected(profile: Int, proxy: BluetoothProfile) {
                proxies[profile] = proxy
                onReady()
            }

            override fun onServiceDisconnected(profile: Int) {
                proxies.remove(profile)
            }
        }
        val app = ctx.applicationContext
        adapter.getProfileProxy(app, listener, BluetoothProfile.A2DP)
        adapter.getProfileProxy(app, listener, BluetoothProfile.HEADSET)
    }

    fun headphones(ctx: Context): Headphones? {
        if (!hasBluetoothPermission(ctx)) return null
        val device = connectedAudioDevice(ctx) ?: return null
        val name = (device.alias ?: device.name)?.takeIf { it.isNotBlank() } ?: "Headphones"
        return Headphones(name, level(device))
    }

    private fun connectedAudioDevice(ctx: Context): BluetoothDevice? {
        // 1) Ask the music (A2DP) and call (HEADSET) profiles: public API.
        for (profile in listOf(BluetoothProfile.A2DP, BluetoothProfile.HEADSET)) {
            proxies[profile]?.connectedDevices?.firstOrNull()?.let { return it }
        }
        // 2) Before the proxies are ready: paired devices that are connected (hidden isConnected()).
        val adapter = ctx.getSystemService(BluetoothManager::class.java)?.adapter ?: return null
        return adapter.bondedDevices.orEmpty().firstOrNull { dev ->
            val audio = dev.bluetoothClass?.majorDeviceClass == android.bluetooth.BluetoothClass.Device.Major.AUDIO_VIDEO
            audio && callBool(dev, "isConnected")
        }
    }

    private fun level(dev: BluetoothDevice): Int {
        val reported = reportedLevels[dev.address] ?: -1
        return try {
            // BluetoothDevice.getBatteryLevel() is a hidden Android method; many apps reach it this way.
            val v = BluetoothDevice::class.java.getMethod("getBatteryLevel").invoke(dev) as Int
            if (v in 0..100) v else reported
        } catch (t: Throwable) {
            reported
        }
    }

    private fun callBool(dev: BluetoothDevice, method: String): Boolean = try {
        BluetoothDevice::class.java.getMethod(method).invoke(dev) as Boolean
    } catch (t: Throwable) {
        false
    }
}
