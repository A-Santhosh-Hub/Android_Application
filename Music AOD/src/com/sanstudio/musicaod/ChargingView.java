package com.sanstudio.musicaod;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.DashPathEffect;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RadialGradient;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.format.DateFormat;
import android.view.View;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;

/**
 * Charging screen in one of two styles: a liquid-filled ring, or an Iron Man-style HUD with an arc-reactor
 * ring and live system panels. Both show the percentage with one or two decimals.
 */
public class ChargingView extends View {
    static final int STYLE_LIQUID = 0;
    static final int STYLE_HUD = 1;
    private static final int BUBBLES = 14;

    private final BatteryReader battery;
    private final SystemStats stats;
    private final AodPrefs prefs;
    private final float dp;
    private final long startedAt = SystemClock.uptimeMillis();
    private boolean running;
    private float shown; // displayed percentage, eased toward the live value so digits roll instead of jump
    private long lastFrame;
    private long sessionStartCounter = -1;
    private long sessionStartAt;

    private final float[] bubbleX = new float[BUBBLES];
    private final float[] bubbleY = new float[BUBBLES];
    private final float[] bubbleR = new float[BUBBLES];
    private final float[] bubbleSpeed = new float[BUBBLES];
    private final Random random = new Random();

    private final Paint track = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint arc = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint comet = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint head = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint liquidBack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint liquidFront = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint big = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint small = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint info = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint hint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint bolt = new Paint(Paint.ANTI_ALIAS_FLAG);
    // HUD
    private final Paint tick = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dash1 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dash2 = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint sweepWedge = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint mono = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint monoSmall = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellLabel = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint cellValue = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frame = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint chip = new Paint(Paint.ANTI_ALIAS_FLAG);

    private final Path wave = new Path();
    private final Path clip = new Path();
    private final Path boltPath = new Path();
    private final RectF oval = new RectF();
    private final RectF rect = new RectF();
    private final Matrix matrix = new Matrix();
    private SweepGradient sweep;
    private RadialGradient core;
    private int shaderColor1, shaderColor2;
    private float shaderCx, shaderCy, shaderR;

    ChargingView(Context context, BatteryReader battery, SystemStats stats, AodPrefs prefs) {
        super(context);
        this.battery = battery;
        this.stats = stats;
        this.prefs = prefs;
        dp = getResources().getDisplayMetrics().density;
        setBackgroundColor(Color.BLACK);

        track.setStyle(Paint.Style.STROKE);
        track.setStrokeWidth(5 * dp);
        track.setColor(0xFF1C1C1E);
        arc.setStyle(Paint.Style.STROKE);
        arc.setStrokeWidth(5 * dp);
        arc.setStrokeCap(Paint.Cap.ROUND);
        comet.setStyle(Paint.Style.STROKE);
        comet.setStrokeWidth(5 * dp);
        comet.setStrokeCap(Paint.Cap.ROUND);
        comet.setColor(0x88FFFFFF);
        big.setColor(Color.WHITE);
        big.setTextSize(72 * dp);
        big.setTypeface(Typeface.create("sans-serif-thin", Typeface.NORMAL));
        big.setShadowLayer(14 * dp, 0, 0, 0xAA000000);
        small.setColor(Color.WHITE);
        small.setTextSize(30 * dp);
        small.setTypeface(Typeface.create("sans-serif-light", Typeface.NORMAL));
        small.setShadowLayer(10 * dp, 0, 0, 0xAA000000);
        label.setColor(Color.WHITE);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(18 * dp);
        label.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        info.setColor(0xFF9A9AA0);
        info.setTextAlign(Paint.Align.CENTER);
        info.setTextSize(14 * dp);
        hint.setColor(0xFF4A4A4A);
        hint.setTextAlign(Paint.Align.CENTER);
        hint.setTextSize(12 * dp);
        bubble.setColor(0x66FFFFFF);

        tick.setStyle(Paint.Style.STROKE);
        tick.setStrokeCap(Paint.Cap.ROUND);
        dash1.setStyle(Paint.Style.STROKE);
        dash1.setStrokeWidth(2 * dp);
        dash1.setPathEffect(new DashPathEffect(new float[]{14 * dp, 10 * dp}, 0));
        dash2.setStyle(Paint.Style.STROKE);
        dash2.setStrokeWidth(1.5f * dp);
        dash2.setPathEffect(new DashPathEffect(new float[]{3 * dp, 7 * dp}, 0));
        mono.setTypeface(Typeface.create(Typeface.MONOSPACE, Typeface.BOLD));
        mono.setColor(Color.WHITE);
        mono.setTextSize(50 * dp);
        monoSmall.setTypeface(Typeface.MONOSPACE);
        monoSmall.setTextSize(11 * dp);
        monoSmall.setLetterSpacing(0.2f);
        cellLabel.setTypeface(Typeface.MONOSPACE);
        cellLabel.setTextSize(10 * dp);
        cellLabel.setLetterSpacing(0.15f);
        cellValue.setTypeface(Typeface.MONOSPACE);
        cellValue.setTextSize(15 * dp);
        cellValue.setColor(0xFFE8F6FF);
        frame.setStyle(Paint.Style.STROKE);
        frame.setStrokeWidth(1.5f * dp);
        chip.setTypeface(Typeface.MONOSPACE);
        chip.setTextSize(13 * dp);
    }

    void setRunning(boolean on) {
        running = on;
        lastFrame = 0;
        if (on) postInvalidateOnAnimation();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        long now = SystemClock.uptimeMillis();
        float dt = lastFrame == 0 ? 0.016f : Math.min(0.05f, (now - lastFrame) / 1000f);
        lastFrame = now;
        float intro = Math.min(1f, (now - startedAt) / 900f);
        float ease = 1f - (1f - intro) * (1f - intro) * (1f - intro);

        // Count up from 0 on entry, then follow the live value closely.
        shown += (battery.live() - shown) * (intro < 1f ? 0.07f : 0.2f);
        if (sessionStartCounter < 0 && battery.hasCounter) {
            sessionStartCounter = battery.counterUah;
            sessionStartAt = SystemClock.elapsedRealtime();
        }

        int[] c = prefs.colors(null);
        if (prefs.chargeStyle == STYLE_HUD) drawHud(canvas, now, ease, c[0], c[1]);
        else drawLiquid(canvas, now, dt, ease, c[0], c[1]);

        if (running) postInvalidateOnAnimation();
    }

    // ---------------------------------------------------------------- liquid style

    private void drawLiquid(Canvas canvas, long now, float dt, float ease, int c1, int c2) {
        float t = now / 1000f;
        float w = getWidth(), h = getHeight();
        boolean details = prefs.chargeDetails;
        float cx = w / 2f, cy = h * (details ? 0.30f : 0.40f);
        float ring = Math.min(w, h) * (details ? 0.29f : 0.34f) * (0.85f + 0.15f * ease);
        float fill = Math.max(0f, Math.min(1f, shown / 100f));

        // Ring: track, gradient progress arc, glowing head, and a comet circling while charging.
        oval.set(cx - ring, cy - ring, cx + ring, cy + ring);
        canvas.drawCircle(cx, cy, ring, track);
        updateShaders(cx, cy, ring, c1, c2);
        arc.setShader(sweep);
        arc.clearShadowLayer();
        canvas.drawArc(oval, -90, 360 * fill, false, arc);
        double a = Math.toRadians(-90 + 360 * fill);
        head.setColor(c2);
        head.setShadowLayer(12 * dp, 0, 0, c2);
        canvas.drawCircle(cx + ring * (float) Math.cos(a), cy + ring * (float) Math.sin(a), 5 * dp, head);
        if (battery.charging) canvas.drawArc(oval, (t * 160f) % 360f - 90, 22, false, comet);

        // Liquid inside the ring, filled to the battery level.
        float r = ring - 14 * dp;
        float surface = cy + r - 2 * r * fill * ease;
        clip.reset();
        clip.addCircle(cx, cy, r, Path.Direction.CW);
        canvas.save();
        canvas.clipPath(clip);
        liquidBack.setColor((c2 & 0x00FFFFFF) | 0x55000000);
        liquidFront.setColor((c1 & 0x00FFFFFF) | 0x99000000);
        drawWave(canvas, cx, cy + r, r, surface, t * 2.2f, 7 * dp, liquidBack);
        drawWave(canvas, cx, cy + r, r, surface + 3 * dp, t * 3.1f + 1.7f, 5 * dp, liquidFront);
        if (battery.charging) drawBubbles(canvas, cx, cy + r, r, surface, dt);
        canvas.restore();

        drawPercent(canvas, cx, cy, big, small);

        float y = cy + ring + 48 * dp;
        String status = statusText();
        float lw = label.measureText(status);
        drawBolt(canvas, cx - lw / 2 - 12 * dp, y - 6 * dp, c1);
        canvas.drawText(status, cx + 6 * dp, y, label);
        y += 28 * dp;
        StringBuilder line = new StringBuilder();
        if (battery.currentMa > 0 && battery.charging) {
            line.append(String.format(Locale.US, "%.1f W  ·  %.2f A  ·  ", battery.watts(), battery.currentMa / 1000f));
        }
        line.append(String.format(Locale.US, "%.2f V  ·  %.1f°C", battery.voltageMv / 1000f, battery.tempC));
        canvas.drawText(line.toString(), cx, y, info);
        if (battery.timeToFullMs > 0) {
            y += 22 * dp;
            canvas.drawText("Full in " + duration(battery.timeToFullMs), cx, y, info);
        }

        if (details) {
            y += 22 * dp;
            List<String[]> rows = new ArrayList<>();
            rows.add(new String[]{"MEMORY", memory()});
            rows.add(new String[]{"STORAGE", storage()});
            rows.add(new String[]{"CPU", cpu()});
            rows.add(new String[]{"THERMAL", thermal()});
            rows.add(new String[]{"NETWORK", stats.network});
            rows.add(new String[]{"SPEED", speed()});
            y = drawGrid(canvas, rows, y, 40 * dp, c1, now, false);
            drawApps(canvas, y + 8 * dp, h * 0.95f, c1, c2, now, false);
        }
        canvas.drawText("Tap to close", cx, h * 0.975f, hint);
    }

    // ---------------------------------------------------------------- HUD style

    private void drawHud(Canvas canvas, long now, float ease, int c1, int c2) {
        float t = now / 1000f;
        float w = getWidth(), h = getHeight();
        float pad = 20 * dp;
        float cx = w / 2f, cy = h * 0.235f;
        float R = Math.min(w * 0.27f, h * 0.13f) * (0.8f + 0.2f * ease);
        float fill = Math.max(0f, Math.min(1f, shown / 100f));

        // Header: title, live clock and a thin rule.
        monoSmall.setColor(c1);
        monoSmall.setTextAlign(Paint.Align.LEFT);
        float hy = h * 0.055f;
        canvas.drawText("POWER CORE  //  ONLINE", pad, hy, monoSmall);
        monoSmall.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText(DateFormat.format("HH:mm:ss", System.currentTimeMillis()).toString(), w - pad, hy, monoSmall);
        frame.setColor((c1 & 0x00FFFFFF) | 0x55000000);
        canvas.drawLine(pad, hy + 10 * dp, w - pad, hy + 10 * dp, frame);

        // Arc reactor: tick ring lit to the level, glowing progress arc, counter-rotating dashed rings,
        // a soft core and a radar sweep.
        updateShaders(cx, cy, R, c1, c2);
        int ticks = 72;
        for (int i = 0; i < ticks; i++) {
            boolean major = i % 6 == 0;
            boolean lit = i / (float) ticks < fill;
            tick.setStrokeWidth((major ? 2.5f : 1.5f) * dp);
            tick.setColor(lit ? VisualizerView.blend(c1, c2, i / (float) ticks) : 0xFF1E2A30);
            double a = Math.toRadians(i * 360.0 / ticks - 90);
            float r0 = R * 1.04f, r1 = R * (major ? 1.16f : 1.10f);
            float cos = (float) Math.cos(a), sin = (float) Math.sin(a);
            canvas.drawLine(cx + r0 * cos, cy + r0 * sin, cx + r1 * cos, cy + r1 * sin, tick);
        }
        oval.set(cx - R * 0.93f, cy - R * 0.93f, cx + R * 0.93f, cy + R * 0.93f);
        arc.setShader(sweep);
        arc.setStrokeWidth(6 * dp);
        arc.setShadowLayer(10 * dp, 0, 0, c1);
        canvas.drawArc(oval, -90, 360 * fill, false, arc);
        arc.setStrokeWidth(5 * dp);

        dash1.setColor((c1 & 0x00FFFFFF) | 0xAA000000);
        dash2.setColor((c2 & 0x00FFFFFF) | 0x99000000);
        canvas.save();
        canvas.rotate((t * 30f) % 360f, cx, cy);
        canvas.drawCircle(cx, cy, R * 0.78f, dash1);
        canvas.restore();
        canvas.save();
        canvas.rotate(-(t * 50f) % 360f, cx, cy);
        canvas.drawCircle(cx, cy, R * 0.66f, dash2);
        canvas.restore();
        glow.setShader(core);
        canvas.drawCircle(cx, cy, R * 0.6f, glow);
        sweepWedge.setColor((c1 & 0x00FFFFFF) | 0x1E000000);
        rect.set(cx - R * 0.6f, cy - R * 0.6f, cx + R * 0.6f, cy + R * 0.6f);
        canvas.drawArc(rect, (t * 120f) % 360f, 40, true, sweepWedge);

        mono.setTextSize(R * 0.42f);
        Paint dec = monoSmall;
        float saveSize = dec.getTextSize();
        dec.setTextSize(R * 0.2f);
        dec.setColor(Color.WHITE);
        dec.setTextAlign(Paint.Align.LEFT);
        mono.setTextAlign(Paint.Align.LEFT);
        drawPercent(canvas, cx, cy - R * 0.04f, mono, dec);
        dec.setTextSize(saveSize);
        monoSmall.setColor(c1);
        monoSmall.setTextAlign(Paint.Align.CENTER);
        canvas.drawText("BATTERY", cx, cy + R * 0.32f, monoSmall);

        // Status line under the reactor.
        float y = cy + R * 1.16f + 34 * dp;
        monoSmall.setColor(battery.charging ? c1 : 0xFF8A8A8A);
        canvas.drawText("●  " + statusText().toUpperCase(Locale.US)
                + (battery.timeToFullMs > 0 ? "  ·  FULL IN " + duration(battery.timeToFullMs).toUpperCase(Locale.US) : ""),
                cx, y, monoSmall);

        // Data panels.
        List<String[]> rows = new ArrayList<>();
        rows.add(new String[]{"POWER", battery.charging && battery.currentMa > 0
                ? String.format(Locale.US, "%.1f W", battery.watts()) : "—"});
        rows.add(new String[]{"CURRENT", String.format(Locale.US, "%.2f A", battery.currentMa / 1000f)});
        rows.add(new String[]{"VOLTAGE", String.format(Locale.US, "%.2f V", battery.voltageMv / 1000f)});
        rows.add(new String[]{"BATTERY TEMP", String.format(Locale.US, "%.1f °C", battery.tempC)});
        rows.add(new String[]{"SESSION", session()});
        rows.add(new String[]{"CAPACITY", battery.fullUah > 0 ? (battery.fullUah / 1000) + " mAh" : "—"});
        rows.add(new String[]{"HEALTH", stats.health + (stats.cycles >= 0 ? " · " + stats.cycles + " cyc" : "")});
        rows.add(new String[]{"THERMAL", thermal()});
        rows.add(new String[]{"CPU", cpu()});
        rows.add(new String[]{"MEMORY", memory()});
        rows.add(new String[]{"STORAGE", storage()});
        rows.add(new String[]{"NETWORK", stats.network});
        rows.add(new String[]{"SPEED", speed()});
        rows.add(new String[]{"UPTIME", duration(SystemClock.elapsedRealtime())});
        float top = y + 22 * dp;
        float bottom = drawGrid(canvas, rows, top, 38 * dp, c1, now, true);
        drawBrackets(canvas, pad - 6 * dp, top - 10 * dp, w - pad + 6 * dp, bottom - 4 * dp, c1);

        drawApps(canvas, bottom + 18 * dp, h * 0.935f, c1, c2, now, true);

        monoSmall.setColor(0xFF4A4A4A);
        monoSmall.setTextAlign(Paint.Align.LEFT);
        canvas.drawText(stats.chip + "  ·  " + stats.android, pad, h * 0.968f, monoSmall);
        monoSmall.setTextAlign(Paint.Align.RIGHT);
        canvas.drawText("TAP TO CLOSE", w - pad, h * 0.968f, monoSmall);
    }

    // ---------------------------------------------------------------- shared pieces

    /** Two-column label/value grid; rows fade in one after another like a boot sequence. Returns its bottom. */
    private float drawGrid(Canvas canvas, List<String[]> cells, float top, float rowH, int c1, long now, boolean hud) {
        float w = getWidth(), pad = 20 * dp, colW = (w - 2 * pad) / 2f;
        int rows = (cells.size() + 1) / 2;
        for (int i = 0; i < cells.size(); i++) {
            int row = i / 2, col = i % 2;
            float alpha = Math.max(0f, Math.min(1f, (now - startedAt - 350 - i * 70) / 250f));
            if (alpha <= 0) continue;
            float x = pad + col * colW + (hud ? 10 * dp : 0), y = top + row * rowH;
            cellLabel.setColor(c1);
            cellLabel.setAlpha((int) (170 * alpha));
            cellValue.setAlpha((int) (255 * alpha));
            if (hud) {
                frame.setColor(c1);
                frame.setAlpha((int) (200 * alpha));
                canvas.drawLine(x - 8 * dp, y + 2 * dp, x - 8 * dp, y + rowH - 10 * dp, frame);
            }
            canvas.drawText(cells.get(i)[0], x, y + 11 * dp, cellLabel);
            // Shrink long values to fit the column before resorting to "…".
            float max = colW - 24 * dp;
            String value = cells.get(i)[1];
            while (cellValue.measureText(value) > max && cellValue.getTextSize() > 11 * dp) {
                cellValue.setTextSize(cellValue.getTextSize() - 0.5f * dp);
            }
            canvas.drawText(fit(value, cellValue, max), x, y + 29 * dp, cellValue);
            cellValue.setTextSize(15 * dp);
        }
        return top + rows * rowH;
    }

    /** Recently used apps and apps running in the background, as small tags. */
    private void drawApps(Canvas canvas, float top, float limit, int c1, int c2, long now, boolean hud) {
        float w = getWidth(), pad = 20 * dp;
        float alpha = Math.max(0f, Math.min(1f, (now - startedAt - 1300) / 300f));
        if (alpha <= 0 || top > limit - 20 * dp) return;
        cellLabel.setColor(c1);
        cellLabel.setAlpha((int) (170 * alpha));
        String title = !stats.usageAccess ? "ACTIVE APPS"
                : "ACTIVE APPS  ·  " + stats.backgroundCount + " RUNNING IN BACKGROUND";
        canvas.drawText(title, pad + (hud ? 2 * dp : 0), top, cellLabel);
        float y = top + 12 * dp;
        if (!stats.usageAccess) {
            cellValue.setAlpha((int) (150 * alpha));
            canvas.drawText(fit("Allow Usage access in Music AOD to see apps", cellValue, w - 2 * pad),
                    pad, y + 16 * dp, cellValue);
            return;
        }
        if (stats.apps.isEmpty()) {
            cellValue.setAlpha((int) (150 * alpha));
            canvas.drawText("No recent apps", pad, y + 16 * dp, cellValue);
            return;
        }
        // Flow tags left to right, wrapping, until we run out of room.
        float x = pad, tagH = 26 * dp, gap = 8 * dp;
        long nowMs = System.currentTimeMillis();
        for (SystemStats.App app : stats.apps) {
            String text = app.label + "  " + (app.background ? "BG" : ago(nowMs - app.lastUsed));
            float tw = chip.measureText(text) + 20 * dp;
            if (x + tw > w - pad) {
                x = pad;
                y += tagH + gap;
            }
            if (y + tagH > limit) break;
            int tint = app.background ? c2 : c1;
            rect.set(x, y, x + tw, y + tagH);
            frame.setColor(tint);
            frame.setAlpha((int) (180 * alpha));
            canvas.drawRoundRect(rect, hud ? 3 * dp : tagH / 2, hud ? 3 * dp : tagH / 2, frame);
            chip.setColor(app.background ? tint : 0xFFE8F6FF);
            chip.setAlpha((int) (255 * alpha));
            canvas.drawText(text, x + 10 * dp, y + tagH / 2 + 4.5f * dp, chip);
            x += tw + gap;
        }
    }

    private void drawBrackets(Canvas canvas, float l, float t, float r, float b, int c1) {
        float s = 14 * dp;
        frame.setColor(c1);
        frame.setAlpha(160);
        canvas.drawLine(l, t, l + s, t, frame);
        canvas.drawLine(l, t, l, t + s, frame);
        canvas.drawLine(r, t, r - s, t, frame);
        canvas.drawLine(r, t, r, t + s, frame);
        canvas.drawLine(l, b, l + s, b, frame);
        canvas.drawLine(l, b, l, b - s, frame);
        canvas.drawLine(r, b, r - s, b, frame);
        canvas.drawLine(r, b, r, b - s, frame);
    }

    private void drawPercent(Canvas canvas, float cx, float cy, Paint wholePaint, Paint restPaint) {
        String whole, rest;
        if (battery.level >= 100) {
            whole = "100";
            rest = "%";
        } else {
            int decimals = Math.max(1, Math.min(2, prefs.chargeDecimals));
            String s = String.format(Locale.US, "%." + decimals + "f", shown);
            int dot = s.indexOf('.');
            whole = s.substring(0, dot);
            rest = s.substring(dot) + "%";
        }
        Paint.Align a1 = wholePaint.getTextAlign(), a2 = restPaint.getTextAlign();
        wholePaint.setTextAlign(Paint.Align.LEFT);
        restPaint.setTextAlign(Paint.Align.LEFT);
        float bw = wholePaint.measureText(whole), sw = restPaint.measureText(rest);
        float x = cx - (bw + sw) / 2f, baseline = cy + wholePaint.getTextSize() * 0.35f;
        canvas.drawText(whole, x, baseline, wholePaint);
        canvas.drawText(rest, x + bw, baseline, restPaint);
        wholePaint.setTextAlign(a1);
        restPaint.setTextAlign(a2);
    }

    private void updateShaders(float cx, float cy, float r, int c1, int c2) {
        if (sweep != null && c1 == shaderColor1 && c2 == shaderColor2 && cx == shaderCx && cy == shaderCy && r == shaderR) {
            return;
        }
        sweep = new SweepGradient(cx, cy, new int[]{c1, c2, c1}, null);
        matrix.setRotate(-90, cx, cy);
        sweep.setLocalMatrix(matrix);
        core = new RadialGradient(cx, cy, Math.max(1f, r * 0.6f),
                (c1 & 0x00FFFFFF) | 0x3A000000, c1 & 0x00FFFFFF, Shader.TileMode.CLAMP);
        shaderColor1 = c1;
        shaderColor2 = c2;
        shaderCx = cx;
        shaderCy = cy;
        shaderR = r;
    }

    private void drawWave(Canvas canvas, float cx, float bottom, float r, float surface, float phase, float amp, Paint p) {
        float left = cx - r, right = cx + r, step = 6 * dp;
        wave.reset();
        wave.moveTo(left, bottom);
        for (float x = left; x <= right + step; x += step) {
            wave.lineTo(x, surface + amp * (float) Math.sin(x / (r * 0.45f) + phase));
        }
        wave.lineTo(right, bottom);
        wave.close();
        canvas.drawPath(wave, p);
    }

    private void drawBubbles(Canvas canvas, float cx, float bottom, float r, float surface, float dt) {
        for (int i = 0; i < BUBBLES; i++) {
            if (bubbleR[i] == 0 || bubbleY[i] < surface) {
                bubbleX[i] = cx + (random.nextFloat() * 1.4f - 0.7f) * r;
                bubbleY[i] = bottom + random.nextFloat() * r * 0.6f;
                bubbleR[i] = (1.5f + random.nextFloat() * 3f) * dp;
                bubbleSpeed[i] = (25 + random.nextFloat() * 45) * dp;
            }
            bubbleY[i] -= bubbleSpeed[i] * dt;
            bubbleX[i] += (float) Math.sin(bubbleY[i] / (12 * dp)) * 0.3f * dp;
            canvas.drawCircle(bubbleX[i], bubbleY[i], bubbleR[i], bubble);
        }
    }

    private void drawBolt(Canvas canvas, float x, float y, int color) {
        float s = 9 * dp;
        boltPath.reset();
        boltPath.moveTo(x + 0.15f * s, y - s);
        boltPath.lineTo(x - 0.55f * s, y + 0.15f * s);
        boltPath.lineTo(x - 0.05f * s, y + 0.15f * s);
        boltPath.lineTo(x - 0.2f * s, y + s);
        boltPath.lineTo(x + 0.55f * s, y - 0.2f * s);
        boltPath.lineTo(x + 0.05f * s, y - 0.2f * s);
        boltPath.close();
        bolt.setColor(color);
        canvas.drawPath(boltPath, bolt);
    }

    // ---------------------------------------------------------------- text helpers

    private String statusText() {
        if (!battery.plugged) return "Not charging";
        if (battery.level >= 100) return "Fully charged";
        if (battery.watts() >= 15) return "Fast charging";
        return battery.charging ? "Charging" : "Plugged in";
    }

    private String session() {
        if (sessionStartCounter < 0) return "—";
        long gained = (battery.counterUah - sessionStartCounter) / 1000;
        long mins = (SystemClock.elapsedRealtime() - sessionStartAt) / 60_000;
        return (gained >= 0 ? "+" : "") + gained + " mAh · " + mins + " min";
    }

    private String memory() {
        if (stats.ramTotal <= 0) return "—";
        return String.format(Locale.US, "%.1f / %.1f GB", (stats.ramTotal - stats.ramAvail) / 1e9, stats.ramTotal / 1e9);
    }

    private String storage() {
        if (stats.storageTotal <= 0) return "—";
        return String.format(Locale.US, "%.0f / %.0f GB",
                (stats.storageTotal - stats.storageFree) / 1e9, stats.storageTotal / 1e9);
    }

    private String cpu() {
        if (stats.cpuMaxMhz > 0) {
            return String.format(Locale.US, "%d cores · %.1f GHz", stats.cores, stats.cpuMaxMhz / 1000f);
        }
        return stats.cores + " cores · " + stats.chip;
    }

    private String thermal() {
        String s = SystemStats.thermalName(stats.thermalStatus);
        if (!Float.isNaN(stats.thermalHeadroom)) s += String.format(Locale.US, " · %d%%", Math.round(stats.thermalHeadroom * 100));
        return s;
    }

    private String speed() {
        return "↓ " + bytes(stats.downBps) + "  ↑ " + bytes(stats.upBps);
    }

    private static String bytes(long bps) {
        if (bps < 1024) return Math.max(0, bps) + " B/s";
        if (bps < 1024 * 1024) return String.format(Locale.US, "%.0f KB/s", bps / 1024f);
        return String.format(Locale.US, "%.1f MB/s", bps / (1024f * 1024f));
    }

    private static String ago(long ms) {
        long min = ms / 60_000;
        if (min < 1) return "now";
        if (min < 60) return min + "m";
        return (min / 60) + "h";
    }

    private static String duration(long ms) {
        long min = Math.max(1, ms / 60_000);
        if (min >= 24 * 60) return (min / (24 * 60)) + " d " + (min % (24 * 60)) / 60 + " h";
        return min >= 60 ? (min / 60) + " h " + (min % 60) + " min" : min + " min";
    }

    private static String fit(String s, Paint p, float max) {
        if (p.measureText(s) <= max) return s;
        while (s.length() > 1 && p.measureText(s + "…") > max) s = s.substring(0, s.length() - 1);
        return s + "…";
    }
}
