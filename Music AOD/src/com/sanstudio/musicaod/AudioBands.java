package com.sanstudio.musicaod;

import android.media.audiofx.Visualizer;
import android.os.SystemClock;
import android.util.Log;

/** Turns Visualizer FFT frames into per-band levels (0..1), or plays a scripted rhythm when there is no audio. */
final class AudioBands implements Visualizer.OnDataCaptureListener {
    static final int MAX = 24;

    final float[] target = new float[MAX];

    private Visualizer visualizer;
    private long lastFrame;
    private int bands = 14;
    private float gain = 1f;
    private boolean rhythm;

    void configure(AodPrefs p) {
        bands = Math.min(MAX, p.bands);
        gain = p.sensitivity / 100f;
        rhythm = p.rhythmMode;
    }

    /** Starts real-audio capture unless rhythm mode is on. Without capture, update() falls back to the rhythm. */
    void start() {
        if (rhythm || visualizer != null) return;
        try {
            visualizer = new Visualizer(0); // session 0 = everything the phone is playing
            visualizer.setCaptureSize(Math.min(1024, Visualizer.getCaptureSizeRange()[1]));
            visualizer.setDataCaptureListener(this, Visualizer.getMaxCaptureRate(), false, true);
            visualizer.setEnabled(true);
        } catch (RuntimeException e) {
            Log.e(WatchService.TAG, "Visualizer failed, using rhythm mode", e);
            visualizer = null;
        }
    }

    void stop() {
        if (visualizer == null) return;
        visualizer.setEnabled(false);
        visualizer.release();
        visualizer = null;
    }

    /** Call once per frame. demoWhenSilent shows the rhythm whenever no audio arrived lately (settings preview). */
    void update(boolean demoWhenSilent) {
        long now = SystemClock.uptimeMillis();
        if (rhythm || visualizer == null || (demoWhenSilent && now - lastFrame > 500)) rhythm(now / 1000f);
    }

    private void rhythm(float t) {
        float beat = (float) Math.pow(Math.max(0, Math.sin(t * Math.PI * 4)), 8); // two beats a second
        for (int b = 0; b < bands; b++) {
            float v = 0.45f - b * 0.012f
                    + 0.22f * (float) Math.sin(t * (2.1f + b * 0.37f) + b * 1.3f)
                    + 0.12f * (float) Math.sin(t * (5.3f + b * 0.11f) + b * 0.7f)
                    + beat * (b < 4 ? 0.35f : 0.1f);
            target[b] = clamp(v * gain);
        }
    }

    @Override
    public void onWaveFormDataCapture(Visualizer v, byte[] waveform, int samplingRate) {
    }

    /** fft: re/im byte pairs; samplingRate in milliHertz. Bands are log-spaced from 45 Hz to 14 kHz. */
    @Override
    public void onFftDataCapture(Visualizer v, byte[] fft, int samplingRate) {
        int bins = fft.length / 2;
        float binHz = samplingRate / 1000f / fft.length;
        float lo = 45f, hi = 14000f, loudest = 0;
        for (int b = 0; b < bands; b++) {
            float f0 = (float) (lo * Math.pow(hi / lo, b / (double) bands));
            float f1 = (float) (lo * Math.pow(hi / lo, (b + 1) / (double) bands));
            int k0 = Math.max(1, (int) (f0 / binHz));
            int k1 = Math.min(bins - 1, Math.max(k0, (int) (f1 / binHz)));
            float peak = 0;
            for (int k = k0; k <= k1; k++) {
                float mag = (float) Math.hypot(fft[2 * k], fft[2 * k + 1]);
                if (mag > peak) peak = mag;
            }
            loudest = Math.max(loudest, peak);
            float db = (float) (20 * Math.log10(peak + 1));
            target[b] = clamp((db - 8f) / 34f * gain);
        }
        // Silent frames keep arriving while nothing plays; only real sound counts as audio.
        if (loudest > 2) lastFrame = SystemClock.uptimeMillis();
    }

    private static float clamp(float v) {
        return Math.max(0f, Math.min(1f, v));
    }
}
