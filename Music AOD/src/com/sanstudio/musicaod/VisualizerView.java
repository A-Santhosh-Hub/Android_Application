package com.sanstudio.musicaod;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.TextPaint;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.view.Display;
import android.view.DisplayCutout;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;

import java.util.Calendar;
import java.util.Locale;

/**
 * Draws the whole Music AOD page: edge bars, top island, clock, album art, song info, progress and controls.
 * Everything is laid out in full-screen pixels and scaled down when shown as the preview in settings.
 */
public class VisualizerView extends View {
    interface Listener {
        void onPrevious();

        void onPlayPause();

        void onNext();

        void onUnlock();
    }

    private final AudioBands bands;
    private final MediaWatcher media;
    private final AodPrefs prefs;
    private final boolean preview;
    private final boolean songInfo;
    private final float dp;
    private final int screenW, screenH;
    private Listener listener;
    private boolean running;
    private Rect cutout;

    private final float[] level = new float[AudioBands.MAX];
    private float envelope; // 0..1: bars grow in when music starts and settle when it pauses
    private int color1, color2; // eased toward the target colours so song changes cross-fade

    private final Paint bar = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint fill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint artPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint clock = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint date = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint title = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final TextPaint artist = new TextPaint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint note = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint icon = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint ring = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final RectF rect = new RectF();
    private final RectF artSrc = new RectF();
    private final Matrix matrix = new Matrix();
    private Bitmap shaderArt;
    private BitmapShader artShader;

    private final float[] buttonX = new float[3];
    private float buttonY = -1, buttonR, scale = 1;

    private String timeText = "", dateText = "";
    private float shiftX, shiftY;
    private long lastMinute = -1;

    VisualizerView(Context context, AudioBands bands, MediaWatcher media, AodPrefs prefs, boolean preview) {
        super(context);
        this.bands = bands;
        this.media = media;
        this.prefs = prefs;
        this.preview = preview;
        songInfo = Edition.hasSongInfo(context);
        dp = getResources().getDisplayMetrics().density;
        setBackgroundColor(Color.BLACK);

        Rect b = context.getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();
        screenW = Math.min(b.width(), b.height());
        screenH = Math.max(b.width(), b.height());
        int[] c = prefs.colors(null);
        color1 = c[0];
        color2 = c[1];

        bar.setStyle(Paint.Style.STROKE);
        bar.setStrokeCap(Paint.Cap.ROUND);
        pill.setColor(0xFF141414);
        track.setStyle(Paint.Style.STROKE);
        track.setStrokeCap(Paint.Cap.ROUND);
        track.setStrokeWidth(3 * dp);
        clock.setColor(Color.WHITE);
        clock.setTextAlign(Paint.Align.CENTER);
        clock.setTextSize(80 * dp);
        clock.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        date.setColor(0xFF9A9A9A);
        date.setTextAlign(Paint.Align.CENTER);
        date.setTextSize(16 * dp);
        title.setColor(Color.WHITE);
        title.setTextAlign(Paint.Align.CENTER);
        title.setTextSize(17 * dp);
        title.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        artist.setColor(0xFF8A8A8A);
        artist.setTextAlign(Paint.Align.CENTER);
        artist.setTextSize(14 * dp);
        small.setColor(0xFF6A6A6A);
        small.setTextSize(11 * dp);
        note.setColor(0xCCFFFFFF);
        note.setTextAlign(Paint.Align.CENTER);
        hint.setColor(0xFF4A4A4A);
        hint.setTextAlign(Paint.Align.CENTER);
        hint.setTextSize(12 * dp);
        ring.setStyle(Paint.Style.STROKE);
        ring.setStrokeWidth(1.5f * dp);
        ring.setColor(0xFF3A3A3A);
    }

    void setListener(Listener l) {
        listener = l;
    }

    void setRunning(boolean on) {
        running = on;
        if (on) postInvalidateOnAnimation();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        Display d = getDisplay();
        DisplayCutout dc = d != null ? d.getCutout() : null;
        if (dc != null && !dc.getBoundingRects().isEmpty()) cutout = new Rect(dc.getBoundingRects().get(0));
    }

    @Override
    public boolean onTouchEvent(MotionEvent e) {
        if (preview || listener == null) return false;
        if (e.getActionMasked() != MotionEvent.ACTION_UP) return true;
        float x = e.getX() / scale, y = e.getY() / scale;
        if (buttonY >= 0) {
            for (int i = 0; i < 3; i++) {
                if (Math.hypot(x - buttonX[i], y - buttonY) < buttonR * 1.3f) {
                    if (i == 0) listener.onPrevious();
                    else if (i == 1) listener.onPlayPause();
                    else listener.onNext();
                    return true;
                }
            }
        }
        listener.onUnlock();
        return true;
    }

    @Override
    protected void onDraw(Canvas canvas) {
        scale = getWidth() / (float) screenW;
        canvas.save();
        canvas.scale(scale, scale);
        bands.update(preview);
        step();
        if (prefs.rightEdge) drawEdge(canvas, true);
        if (prefs.leftEdge) drawEdge(canvas, false);
        if (prefs.island) drawIsland(canvas);
        if (prefs.minimal) buttonY = -1; // no buttons: any tap unlocks
        else drawContent(canvas);
        canvas.restore();
        if (running) postInvalidateOnAnimation();
    }

    /** Advances the per-frame animation state: bar levels, play envelope, colours and the clock. */
    private void step() {
        for (int i = 0; i < prefs.bands; i++) {
            float t = bands.target[i];
            level[i] += (t - level[i]) * (t > level[i] ? 0.55f : 0.12f); // fast rise, slow fall
        }
        float want = preview || media.isPlaying() ? 1f : 0f;
        envelope += (want - envelope) * 0.06f;
        int[] c = prefs.colors(media.artColors);
        color1 = blend(color1, c[0], 0.06f);
        color2 = blend(color2, c[1], 0.06f);

        long minute = System.currentTimeMillis() / 60000;
        if (minute != lastMinute) {
            lastMinute = minute;
            Calendar now = Calendar.getInstance();
            timeText = DateFormat.format(DateFormat.is24HourFormat(getContext()) ? "HH:mm" : "h:mm", now).toString();
            dateText = DateFormat.format("EEEE, d MMMM", now).toString();
            // Nudge static content a few pixels each minute so the OLED doesn't burn in.
            int m = now.get(Calendar.MINUTE);
            shiftX = ((m % 5) - 2) * 3 * dp;
            shiftY = (((m / 5) % 5) - 2) * 3 * dp;
        }
    }

    private void drawEdge(Canvas canvas, boolean right) {
        int n = prefs.bands;
        float stripH = screenH * prefs.stripLength / 100f;
        float mid = screenH / 2f + screenH * prefs.stripOffset / 100f;
        mid = Math.max(stripH / 2f, Math.min(screenH - stripH / 2f, mid));
        float step = stripH / (n * 2);
        float stroke = Math.max(1.5f * dp, step * prefs.thickness / 100f);
        float edge = right ? screenW - 6 * dp - stroke / 2f : 6 * dp + stroke / 2f;
        float dir = right ? -1 : 1;
        float minLen = 3 * dp, maxLen = prefs.barLength * dp;
        bar.setStrokeWidth(stroke);
        for (int i = 0; i < n; i++) {
            float v = level[i] * envelope;
            float len = minLen + v * maxLen;
            bar.setColor(blend(color1, color2, n > 1 ? i / (float) (n - 1) : 0));
            bar.setAlpha((int) (110 + 145 * v));
            float up = mid - step * (i + 0.5f), down = mid + step * (i + 0.5f);
            canvas.drawLine(edge, up, edge + dir * len, up, bar);
            canvas.drawLine(edge, down, edge + dir * len, down, bar);
        }
    }

    private void drawIsland(Canvas canvas) {
        float cx = cutout != null ? cutout.exactCenterX() : screenW / 2f;
        float cy = cutout != null ? cutout.exactCenterY() : 22 * dp;
        float camR = cutout != null ? cutout.width() / 2f : 0;
        float h = Math.max(cutout != null ? cutout.height() + 14 * dp : 0, 36 * dp);
        float bass = (level[0] + level[1] + level[2]) / 3f * envelope;
        boolean withArt = prefs.islandStyle == 1 && !prefs.minimal;
        float w = screenW * (withArt ? 0.6f : 0.5f) + bass * 12 * dp;
        rect.set(cx - w / 2, cy - h / 2, cx + w / 2, cy + h / 2);
        canvas.drawRoundRect(rect, h / 2, h / 2, pill);

        float pad = h / 2;
        if (withArt) {
            // iPhone style: album art on the left of the camera, bars on the right.
            float r = h * 0.34f, ax = cx - w / 2 + pad;
            rect.set(ax - r, cy - r, ax + r, cy + r);
            drawArt(canvas, rect, r);
            islandBars(canvas, cx + camR + 12 * dp, cx + w / 2 - pad * 0.6f, cx, cy, h, 5, false);
        } else {
            islandBars(canvas, cx + camR + 10 * dp, cx + w / 2 - pad, cx, cy, h, 6, true);
        }
    }

    private void islandBars(Canvas canvas, float from, float to, float cx, float cy, float h, int k, boolean mirror) {
        float step = (to - from) / k;
        float stroke = Math.max(1.5f * dp, step * prefs.thickness / 100f);
        bar.setStrokeWidth(stroke);
        for (int i = 0; i < k; i++) {
            float v = level[i * prefs.bands / k] * envelope;
            float len = Math.max(0f, h * 0.12f + v * h * 0.5f - stroke);
            float x = from + step * (i + 0.5f);
            bar.setColor(blend(color1, color2, i / (float) (k - 1)));
            canvas.drawLine(x, cy - len / 2, x, cy + len / 2, bar);
            if (mirror) canvas.drawLine(2 * cx - x, cy - len / 2, 2 * cx - x, cy + len / 2, bar);
        }
    }

    private void drawArt(Canvas canvas, RectF dst, float radius) {
        Bitmap art = media.art;
        if (art == null) {
            fill.setColor(blend(color1, Color.BLACK, 0.55f));
            canvas.drawRoundRect(dst, radius, radius, fill);
            note.setTextSize(dst.height() * 0.5f);
            canvas.drawText("♪", dst.centerX(), dst.centerY() + dst.height() * 0.17f, note);
            return;
        }
        if (art != shaderArt) {
            shaderArt = art;
            artShader = new BitmapShader(art, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP);
        }
        artSrc.set(0, 0, art.getWidth(), art.getHeight());
        matrix.setRectToRect(artSrc, dst, Matrix.ScaleToFit.FILL);
        artShader.setLocalMatrix(matrix);
        artPaint.setShader(artShader);
        canvas.drawRoundRect(dst, radius, radius, artPaint);
    }

    private void drawContent(Canvas canvas) {
        float cx = screenW / 2f + shiftX;
        float reserve = prefs.rightEdge || prefs.leftEdge ? (prefs.barLength + 20) * dp : 24 * dp;
        float width = Math.max(120 * dp, Math.min(screenW * 0.7f, screenW - 2 * reserve));
        boolean demo = preview && songInfo && !media.hasSong(); // placeholders only where song info exists

        float y = screenH * 0.14f + shiftY;
        if (prefs.showClock) {
            y += 70 * dp;
            canvas.drawText(timeText, cx, y, clock);
            y += 34 * dp;
            canvas.drawText(dateText, cx, y, date);
            y += 36 * dp;
        } else {
            y = screenH * 0.2f + shiftY;
        }

        if (prefs.showArt && (media.art != null || demo)) {
            float a = Math.min(150 * dp, width);
            rect.set(cx - a / 2, y, cx + a / 2, y + a);
            drawArt(canvas, rect, 16 * dp);
            y += a + 30 * dp;
        }

        if (prefs.showSong) {
            String t = demo ? "Song title" : media.title;
            String a = demo ? "Artist" : media.artist;
            if (!t.isEmpty()) {
                canvas.drawText(TextUtils.ellipsize(t, title, width, TextUtils.TruncateAt.END).toString(), cx, y, title);
                y += 24 * dp;
            }
            if (!a.isEmpty()) {
                canvas.drawText(TextUtils.ellipsize(a, artist, width, TextUtils.TruncateAt.END).toString(), cx, y, artist);
                y += 26 * dp;
            }
        }

        long dur = demo ? 215_000 : media.duration;
        if (prefs.showProgress && dur > 0) {
            long pos = demo ? 83_000 : media.position();
            float pw = Math.min(width, 220 * dp), x0 = cx - pw / 2;
            track.setColor(0xFF2A2A2A);
            canvas.drawLine(x0, y, x0 + pw, y, track);
            track.setColor(color1);
            canvas.drawLine(x0, y, x0 + pw * Math.min(1f, pos / (float) dur), y, track);
            y += 18 * dp;
            small.setTextAlign(Paint.Align.LEFT);
            canvas.drawText(formatTime(pos), x0, y, small);
            small.setTextAlign(Paint.Align.RIGHT);
            canvas.drawText("-" + formatTime(dur - pos), x0 + pw, y, small);
            y += 20 * dp;
        }

        if (prefs.showControls) {
            buttonY = y + 36 * dp;
            buttonR = 30 * dp;
            float spacing = 80 * dp;
            buttonX[0] = cx - spacing;
            buttonX[1] = cx;
            buttonX[2] = cx + spacing;
            canvas.drawCircle(buttonX[1], buttonY, buttonR, ring);
            icon.setColor(0xFFCFCFCF);
            drawSkip(canvas, buttonX[0], buttonY, false);
            drawSkip(canvas, buttonX[2], buttonY, true);
            icon.setColor(Color.WHITE);
            drawPlayPause(canvas, buttonX[1], buttonY, preview || media.isPlaying());
        } else {
            buttonY = -1;
        }

        canvas.drawText("Tap anywhere to unlock", screenW / 2f, screenH * 0.8f, hint);
    }

    private void drawSkip(Canvas canvas, float x, float y, boolean forward) {
        float s = 9 * dp, dir = forward ? 1 : -1;
        path.reset();
        path.moveTo(x - dir * s * 0.6f, y - s);
        path.lineTo(x + dir * s * 0.6f, y);
        path.lineTo(x - dir * s * 0.6f, y + s);
        path.close();
        canvas.drawPath(path, icon);
        float bx = x + dir * (s * 0.6f + 2.5f * dp);
        rect.set(bx - 1.5f * dp, y - s, bx + 1.5f * dp, y + s);
        canvas.drawRoundRect(rect, 1.5f * dp, 1.5f * dp, icon);
    }

    private void drawPlayPause(Canvas canvas, float x, float y, boolean playing) {
        if (playing) {
            float bw = 4 * dp, bh = 10 * dp, gap = 3.5f * dp;
            rect.set(x - gap - bw, y - bh, x - gap, y + bh);
            canvas.drawRoundRect(rect, 1.5f * dp, 1.5f * dp, icon);
            rect.set(x + gap, y - bh, x + gap + bw, y + bh);
            canvas.drawRoundRect(rect, 1.5f * dp, 1.5f * dp, icon);
        } else {
            path.reset();
            path.moveTo(x - 6 * dp, y - 11 * dp);
            path.lineTo(x + 11 * dp, y);
            path.lineTo(x - 6 * dp, y + 11 * dp);
            path.close();
            canvas.drawPath(path, icon);
        }
    }

    private static String formatTime(long ms) {
        long s = Math.max(0, ms / 1000);
        return String.format(Locale.US, "%d:%02d", s / 60, s % 60);
    }

    static int blend(int a, int b, float t) {
        int r = Math.round(Color.red(a) + (Color.red(b) - Color.red(a)) * t);
        int g = Math.round(Color.green(a) + (Color.green(b) - Color.green(a)) * t);
        int bl = Math.round(Color.blue(a) + (Color.blue(b) - Color.blue(a)) * t);
        return Color.rgb(r, g, bl);
    }
}
