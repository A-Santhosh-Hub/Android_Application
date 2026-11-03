package com.sanstudio.musicaod;

import android.app.Activity;
import android.app.KeyguardManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.ServiceInfo;
import android.media.AudioManager;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.os.PowerManager;
import android.util.Log;

import java.util.function.BooleanSupplier;

/**
 * Runs in the background: shows AodActivity when the screen turns off during music, and ChargingActivity
 * when the charger is plugged in. Also keeps the battery reader learning the capacity while it charges.
 */
public class WatchService extends Service {
    static final String TAG = "MusicAOD";
    static final int FG_ID = 1;
    static final int FSI_ID = 2;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AudioManager audio;
    private BatteryReader battery;

    private final BroadcastReceiver events = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (Intent.ACTION_SCREEN_OFF.equals(action)) {
                handler.postDelayed(WatchService.this::onScreenOff, 250);
            } else if (Intent.ACTION_POWER_CONNECTED.equals(action)) {
                handler.postDelayed(WatchService.this::onPowerConnected, 400);
            } else if (Intent.ACTION_BATTERY_CHANGED.equals(action)) {
                battery.read();
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        audio = getSystemService(AudioManager.class);
        battery = new BatteryReader(this);

        NotificationManager nm = getSystemService(NotificationManager.class);
        nm.createNotificationChannel(new NotificationChannel("status", "Running", NotificationManager.IMPORTANCE_MIN));
        nm.createNotificationChannel(new NotificationChannel("aod", "Music AOD", NotificationManager.IMPORTANCE_HIGH));

        Notification n = new Notification.Builder(this, "status")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Music AOD is on")
                .setContentText("Lock the phone while music plays, or plug in the charger")
                .setOngoing(true)
                .build();
        startForeground(FG_ID, n, ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE);

        IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
        filter.addAction(Intent.ACTION_POWER_CONNECTED);
        filter.addAction(Intent.ACTION_BATTERY_CHANGED);
        registerReceiver(events, filter, Context.RECEIVER_EXPORTED);
        Log.i(TAG, "service started");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(events);
        handler.removeCallbacksAndMessages(null);
        Log.i(TAG, "service stopped");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    private void onScreenOff() {
        // Power pressed while one of our screens was showing: let the screen really go dark.
        if (AodActivity.sInstance != null) {
            AodActivity.sInstance.finish();
            return;
        }
        if (ChargingActivity.sInstance != null) {
            ChargingActivity.sInstance.finish();
            return;
        }
        if (!AodPrefs.load(this).musicEnabled || !audio.isMusicActive()) return;

        Log.i(TAG, "screen off with music, launching AOD");
        launch(AodActivity.class, () -> AodActivity.sInstance != null);
    }

    private void onPowerConnected() {
        AodPrefs prefs = AodPrefs.load(this);
        if (!prefs.chargeAnim || AodActivity.sInstance != null || ChargingActivity.sInstance != null) return;
        boolean locked = getSystemService(KeyguardManager.class).isKeyguardLocked()
                || !getSystemService(PowerManager.class).isInteractive();
        if (prefs.chargeLockOnly && !locked) return;

        Log.i(TAG, "charger connected, launching charging animation");
        launch(ChargingActivity.class, () -> ChargingActivity.sInstance != null);
    }

    private void launch(Class<? extends Activity> activity, BooleanSupplier showing) {
        try {
            startActivity(intent(activity, "direct"));
        } catch (RuntimeException e) {
            Log.w(TAG, "direct start failed", e);
        }
        // vivo may silently block background starts; fall back to a full-screen notification.
        handler.postDelayed(() -> {
            if (!showing.getAsBoolean()) postFullScreenIntent(activity);
        }, 800);
    }

    private void postFullScreenIntent(Class<? extends Activity> activity) {
        Log.i(TAG, "direct start blocked, trying full-screen intent");
        PendingIntent pi = PendingIntent.getActivity(this, 0, intent(activity, "fullscreen-intent"),
                PendingIntent.FLAG_IMMUTABLE | PendingIntent.FLAG_UPDATE_CURRENT);
        Notification n = new Notification.Builder(this, "aod")
                .setSmallIcon(android.R.drawable.ic_media_play)
                .setContentTitle("Music AOD")
                .setCategory(Notification.CATEGORY_ALARM)
                .setFullScreenIntent(pi, true)
                .setAutoCancel(true)
                .build();
        getSystemService(NotificationManager.class).notify(FSI_ID, n);
    }

    private Intent intent(Class<? extends Activity> activity, String via) {
        return new Intent(this, activity)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_ANIMATION)
                .putExtra("via", via);
    }
}
