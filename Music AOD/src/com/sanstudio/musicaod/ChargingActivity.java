package com.sanstudio.musicaod;

import android.app.Activity;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Shown over the lock screen when the charger is plugged in. */
public class ChargingActivity extends Activity {
    static ChargingActivity sInstance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final ExecutorService slowReads = Executors.newSingleThreadExecutor();
    private BatteryReader battery;
    private SystemStats stats;
    private ChargingView view;
    private long closeAt; // 0 = stay until unplugged
    private int ticks;

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            battery.read();
            stats.sampleFast();
            if (ticks++ % 5 == 0) slowReads.execute(stats::sampleSlow);
            if (closeAt > 0 && SystemClock.elapsedRealtime() > closeAt) {
                finish();
                return;
            }
            handler.postDelayed(this, 1000);
        }
    };

    private final BroadcastReceiver unplugged = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            finish();
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        AodPrefs prefs = AodPrefs.load(this);
        boolean preview = "preview".equals(getIntent().getStringExtra("via"));
        int seconds = preview ? 10 : prefs.chargeSeconds;
        if (seconds > 0) closeAt = SystemClock.elapsedRealtime() + seconds * 1000L;

        setShowWhenLocked(true);
        setTurnScreenOn(true);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = Math.max(prefs.brightness, 25) / 100f; // it's on the charger, so a little brighter
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        w.setAttributes(lp);
        w.setDecorFitsSystemWindows(false);

        battery = new BatteryReader(this);
        battery.read();
        stats = new SystemStats(this);
        view = new ChargingView(this, battery, stats, prefs);
        view.setOnClickListener(v -> finish());
        setContentView(view);

        getSystemService(NotificationManager.class).cancel(WatchService.FSI_ID);
    }

    @Override
    protected void onResume() {
        super.onResume();
        WindowInsetsController c = getWindow().getInsetsController();
        if (c != null) {
            c.hide(WindowInsets.Type.systemBars());
            c.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        }
        registerReceiver(unplugged, new IntentFilter(Intent.ACTION_POWER_DISCONNECTED), Context.RECEIVER_EXPORTED);
        view.setRunning(true);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        view.setRunning(false);
        unregisterReceiver(unplugged);
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        slowReads.shutdownNow();
        if (sInstance == this) sInstance = null;
        super.onDestroy();
    }
}
