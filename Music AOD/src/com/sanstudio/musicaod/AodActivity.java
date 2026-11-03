package com.sanstudio.musicaod;

import android.app.Activity;
import android.app.NotificationManager;
import android.media.AudioManager;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.util.Log;
import android.view.Window;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;

/** The black lock-screen page shown after the screen turns off while music plays. */
public class AodActivity extends Activity implements VisualizerView.Listener {
    static AodActivity sInstance;

    private final Handler handler = new Handler(Looper.getMainLooper());
    private AodPrefs prefs;
    private AudioBands bands;
    private MediaWatcher media;
    private VisualizerView view;
    private AudioManager audio;
    private int silentSeconds;
    private long keepUntil; // stay open after a manual pause (or a preview) even though nothing plays

    private final Runnable tick = new Runnable() {
        @Override
        public void run() {
            media.poll();
            if (audio.isMusicActive()) {
                silentSeconds = 0;
            } else if (++silentSeconds >= prefs.closeDelay && SystemClock.elapsedRealtime() > keepUntil) {
                Log.i(WatchService.TAG, "music stopped, closing AOD");
                finish();
                return;
            }
            handler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        sInstance = this;
        String via = getIntent().getStringExtra("via");
        Log.i(WatchService.TAG, "AOD shown via " + via);
        if ("preview".equals(via)) keepUntil = SystemClock.elapsedRealtime() + 15_000;

        prefs = AodPrefs.load(this);
        setShowWhenLocked(true);
        setTurnScreenOn(true);
        Window w = getWindow();
        w.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        WindowManager.LayoutParams lp = w.getAttributes();
        lp.screenBrightness = prefs.brightness / 100f;
        lp.layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_ALWAYS;
        w.setAttributes(lp);
        w.setDecorFitsSystemWindows(false);

        audio = getSystemService(AudioManager.class);
        bands = new AudioBands();
        media = new MediaWatcher(this, handler);
        view = new VisualizerView(this, bands, media, prefs, false);
        view.setListener(this);
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
        bands.configure(prefs);
        bands.start();
        media.start();
        view.setRunning(true);
        handler.post(tick);
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(tick);
        view.setRunning(false);
        bands.stop();
        media.stop();
        super.onPause();
    }

    @Override
    protected void onDestroy() {
        if (sInstance == this) sInstance = null;
        super.onDestroy();
    }

    @Override
    public void onPrevious() {
        media.previous();
    }

    @Override
    public void onNext() {
        media.next();
    }

    @Override
    public void onPlayPause() {
        keepUntil = SystemClock.elapsedRealtime() + 60_000;
        media.playPause();
    }

    @Override
    public void onUnlock() {
        finish(); // reveals the normal lock screen, where the fingerprint works
    }
}
