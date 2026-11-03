package com.sanstudio.musicaod;

import android.Manifest;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.AppOpsManager;
import android.app.NotificationManager;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Insets;
import android.graphics.LinearGradient;
import android.graphics.Outline;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.Shader;
import android.graphics.SweepGradient;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewOutlineProvider;
import android.view.WindowInsets;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.HorizontalScrollView;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.ScrollView;
import android.widget.SeekBar;
import android.widget.Switch;
import android.widget.TextView;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.IntConsumer;

/** Settings: setup checklist, live preview, and every visual option. */
public class MainActivity extends Activity {
    private static final int BG = 0xFF000000;
    private static final int CARD = 0xFF141416;
    private static final int TEXT = 0xFFF2F2F2;
    private static final int MUTED = 0xFF9A9AA0;
    private static final String SANSTUDIO_URL = "https://sanstudio-hub.github.io/in/";

    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<SwatchView> swatches = new ArrayList<>();
    private AodPrefs prefs;
    private AudioBands bands;
    private MediaWatcher media;
    private VisualizerView preview;
    private TextView setupTitle;
    private LinearLayout setupList;
    private TextView paletteName;
    private LinearLayout customColors;
    private ValueAnimator neon;
    private BatteryReader batteryReader;
    private TextView batteryLine;
    private TextView usageRow;
    private final Runnable batteryTick = new Runnable() {
        @Override
        public void run() {
            refreshBattery();
            handler.postDelayed(this, 1000);
        }
    };
    private boolean songInfo; // false in the Lite build
    private int setupDone, setupTotal;
    private float dp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        dp = getResources().getDisplayMetrics().density;
        prefs = AodPrefs.load(this);
        songInfo = Edition.hasSongInfo(this);
        batteryReader = new BatteryReader(this);
        bands = new AudioBands();
        media = new MediaWatcher(this, handler);

        ScrollView scroll = new ScrollView(this);
        scroll.setBackgroundColor(BG);
        scroll.setOnApplyWindowInsetsListener((v, insets) -> {
            Insets bars = insets.getInsets(WindowInsets.Type.systemBars());
            v.setPadding(0, bars.top, 0, bars.bottom);
            return insets;
        });
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        int pad = px(18);
        col.setPadding(pad, pad, pad, pad * 2);
        scroll.addView(col);

        col.addView(text("Music AOD", 28, TEXT, true));
        col.addView(text("A music visualizer and charging animation on your lock screen.", 14, MUTED, false));
        LinearLayout master = card(col, null);
        toggle(master, "Music visualizer when locked", prefs.musicEnabled, v -> {
            prefs.musicEnabled = v;
            changed();
            updateService();
        });
        toggle(master, "Charging animation", prefs.chargeAnim, v -> {
            prefs.chargeAnim = v;
            changed();
            updateService();
        });

        LinearLayout setup = card(col, null);
        setupTitle = text("", 15, TEXT, true);
        setup.addView(setupTitle);
        setupList = new LinearLayout(this);
        setupList.setOrientation(LinearLayout.VERTICAL);
        setup.addView(setupList);

        buildPreview(card(col, "Preview"));

        LinearLayout where = card(col, "Where");
        toggle(where, "Top island", prefs.island, v -> { prefs.island = v; changed(); });
        toggle(where, "Right edge", prefs.rightEdge, v -> { prefs.rightEdge = v; changed(); });
        toggle(where, "Left edge", prefs.leftEdge, v -> { prefs.leftEdge = v; changed(); });
        where.addView(text("Island style", 13, MUTED, false));
        RadioGroup style = new RadioGroup(this);
        String[] styles = {"Bars around the camera", "Album art + bars (iPhone style)"};
        for (int i = 0; i < styles.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setId(i + 1);
            r.setText(styles[i]);
            r.setTextColor(TEXT);
            style.addView(r);
        }
        style.check(prefs.islandStyle + 1);
        style.setOnCheckedChangeListener((g, id) -> { prefs.islandStyle = id - 1; changed(); });
        where.addView(style);

        buildColours(card(col, "Colours"));

        LinearLayout barsCard = card(col, "Bars");
        slider(barsCard, "Thickness", "%", 20, 90, prefs.thickness, v -> { prefs.thickness = v; changed(); });
        slider(barsCard, "Length", " dp", 30, 140, prefs.barLength, v -> { prefs.barLength = v; changed(); });
        slider(barsCard, "Number of bars", "", 6, 20, prefs.bands, v -> { prefs.bands = v; changed(); });
        slider(barsCard, "Sensitivity", "%", 50, 200, prefs.sensitivity, v -> { prefs.sensitivity = v; changed(); });
        toggle(barsCard, "Rhythm mode", prefs.rhythmMode, v -> {
            prefs.rhythmMode = v;
            changed();
            restartAudio();
            refreshSetup();
        });
        barsCard.addView(text("Plays a beat animation instead of reading the real sound. No microphone needed.",
                12, MUTED, false));

        LinearLayout position = card(col, "Edge position");
        slider(position, "Strip length", "%", 15, 90, prefs.stripLength, v -> { prefs.stripLength = v; changed(); });
        slider(position, "Up / down", "%", -30, 30, prefs.stripOffset, v -> { prefs.stripOffset = v; changed(); });

        LinearLayout screen = card(col, "Screen");
        slider(screen, "Brightness", "%", 2, 50, prefs.brightness, v -> { prefs.brightness = v; changed(); });
        List<View> details = new ArrayList<>();
        toggle(screen, "Visualizer only", prefs.minimal, v -> {
            prefs.minimal = v;
            changed();
            for (View d : details) d.setEnabled(!v);
        });
        screen.addView(text("Just the bars on black — hides the clock, song details, album art and buttons.",
                12, MUTED, false));
        details.add(toggle(screen, "Clock", prefs.showClock, v -> { prefs.showClock = v; changed(); }));
        if (songInfo) {
            details.add(toggle(screen, "Song title + artist", prefs.showSong, v -> { prefs.showSong = v; changed(); }));
            details.add(toggle(screen, "Album art", prefs.showArt, v -> { prefs.showArt = v; changed(); }));
            details.add(toggle(screen, "Progress bar", prefs.showProgress, v -> { prefs.showProgress = v; changed(); }));
        }
        details.add(toggle(screen, "Play controls", prefs.showControls, v -> { prefs.showControls = v; changed(); }));
        for (View d : details) d.setEnabled(!prefs.minimal);
        if (!songInfo) {
            screen.addView(text("Lite edition: song title, album art and progress bar aren't included, "
                    + "so the app can be installed from WhatsApp or a browser.", 12, MUTED, false));
        }
        slider(screen, "Close after music stops", " s", 3, 60, prefs.closeDelay, v -> { prefs.closeDelay = v; changed(); });

        buildCharging(card(col, "Charging animation"));

        LinearLayout more = card(col, null);
        button(more, "Reset all settings", () -> {
            new AodPrefs().save(this);
            recreate();
        });
        col.addView(text("Uses more battery than the normal AOD because the screen stays on dimly. "
                + "Best for trips with a power bank. Open the app again after restarting your phone.", 12, MUTED, false));
        buildCredit(col);

        setContentView(scroll);
        updateService();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refreshSetup();
        bands.configure(prefs);
        if (micGranted()) bands.start();
        media.start();
        preview.setRunning(true);
        neon.start();
        handler.post(batteryTick);
        refreshUsageRow();
    }

    @Override
    protected void onPause() {
        handler.removeCallbacks(batteryTick);
        neon.cancel();
        preview.setRunning(false);
        bands.stop();
        media.stop();
        super.onPause();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        refreshSetup();
        restartAudio();
    }

    // --- sections ---

    private void buildPreview(LinearLayout card) {
        preview = new VisualizerView(this, bands, media, prefs, true);
        Rect b = getSystemService(WindowManager.class).getMaximumWindowMetrics().getBounds();
        int w = px(170);
        int h = Math.round(w * Math.max(b.width(), b.height()) / (float) Math.min(b.width(), b.height()));
        preview.setOutlineProvider(new ViewOutlineProvider() {
            @Override
            public void getOutline(View v, Outline o) {
                o.setRoundRect(0, 0, v.getWidth(), v.getHeight(), 18 * dp);
            }
        });
        preview.setClipToOutline(true);

        FrameLayout frame = new FrameLayout(this);
        GradientDrawable border = new GradientDrawable();
        border.setColor(0xFF000000);
        border.setStroke(px(2), 0xFF2E2E33);
        border.setCornerRadius(21 * dp);
        frame.setBackground(border);
        frame.setPadding(px(3), px(3), px(3), px(3));
        frame.addView(preview, new FrameLayout.LayoutParams(w, h));
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.gravity = Gravity.CENTER_HORIZONTAL;
        lp.topMargin = px(8);
        card.addView(frame, lp);
        button(card, "Full-screen preview", () ->
                startActivity(new Intent(this, AodActivity.class).putExtra("via", "preview")));
    }

    private void buildCharging(LinearLayout card) {
        batteryLine = text("", 14, TEXT, false);
        card.addView(batteryLine);
        card.addView(text("Shows a liquid ring and the exact percentage, like 72.14%, when you plug in the charger.",
                12, MUTED, false));
        card.addView(text("Style", 13, MUTED, false));
        RadioGroup style = new RadioGroup(this);
        String[] styles = {"Liquid ring", "Iron Man HUD (arc reactor + live system panels)"};
        int[] styleIds = {ChargingView.STYLE_LIQUID, ChargingView.STYLE_HUD};
        for (int i = 0; i < styles.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setId(200 + styleIds[i]);
            r.setText(styles[i]);
            r.setTextColor(TEXT);
            style.addView(r);
        }
        style.check(200 + prefs.chargeStyle);
        style.setOnCheckedChangeListener((g, id) -> { prefs.chargeStyle = id - 200; changed(); });
        card.addView(style);

        toggle(card, "Phone details", prefs.chargeDetails, v -> { prefs.chargeDetails = v; changed(); });
        card.addView(text("RAM, storage, CPU, heat, network speed, battery health and active apps.", 12, MUTED, false));
        usageRow = new TextView(this);
        usageRow.setTextSize(14);
        usageRow.setPadding(0, px(10), 0, px(10));
        usageRow.setOnClickListener(v -> openUsageAccess());
        card.addView(usageRow);

        toggle(card, "Only when the phone is locked", prefs.chargeLockOnly, v -> { prefs.chargeLockOnly = v; changed(); });
        slider(card, "Decimal places", "", 1, 2, prefs.chargeDecimals, v -> { prefs.chargeDecimals = v; changed(); });

        card.addView(text("Show for", 13, MUTED, false));
        RadioGroup duration = new RadioGroup(this);
        int[] seconds = {5, 10, 30, 0};
        String[] labels = {"5 seconds", "10 seconds", "30 seconds", "Until unplugged"};
        for (int i = 0; i < seconds.length; i++) {
            RadioButton r = new RadioButton(this);
            r.setId(100 + i);
            r.setText(labels[i]);
            r.setTextColor(TEXT);
            duration.addView(r);
            if (seconds[i] == prefs.chargeSeconds) duration.check(r.getId());
        }
        duration.setOnCheckedChangeListener((g, id) -> { prefs.chargeSeconds = seconds[id - 100]; changed(); });
        card.addView(duration);

        button(card, "Preview charging animation", () ->
                startActivity(new Intent(this, ChargingActivity.class).putExtra("via", "preview")));
    }

    private void refreshUsageRow() {
        boolean granted = getSystemService(AppOpsManager.class).unsafeCheckOpNoThrow(AppOpsManager.OPSTR_GET_USAGE_STATS,
                android.os.Process.myUid(), getPackageName()) == AppOpsManager.MODE_ALLOWED;
        usageRow.setText(granted ? "✅  Usage access: recent and background apps are shown"
                : "❌  Tap to allow Usage access, to show recent and background apps (optional)");
        usageRow.setTextColor(granted ? TEXT : 0xFFFF8A80);
        usageRow.setClickable(!granted);
    }

    private void openUsageAccess() {
        try {
            startActivity(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS, packageUri()));
        } catch (ActivityNotFoundException e) {
            openSettings(new Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS));
        }
    }

    private void refreshBattery() {
        batteryReader.read();
        String now = String.format(Locale.US, "%.2f%%", batteryReader.precise);
        if (!batteryReader.hasCounter) {
            batteryLine.setText("Now " + batteryReader.level + "%  ·  this phone doesn't report a charge counter, "
                    + "so decimals stay at .00");
        } else {
            batteryLine.setText("Now " + now + "  ·  learned capacity " + (batteryReader.fullUah / 1000) + " mAh"
                    + (batteryReader.charging ? "  ·  charging" : ""));
        }
    }

    /** "Developed by SanStudio" with a pulsing neon glow; tapping it opens the SanStudio site. */
    private void buildCredit(LinearLayout col) {
        LinearLayout credit = new LinearLayout(this);
        credit.setGravity(Gravity.CENTER);
        credit.setPadding(0, px(28), 0, px(4));
        credit.addView(text("Developed by ", 15, MUTED, false));
        TextView name = text("SanStudio", 20, 0xFFB9F6FF, true);
        name.setPadding(px(6), px(8), px(6), px(8)); // room for the glow
        credit.addView(name);
        credit.setOnClickListener(v -> {
            try {
                startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(SANSTUDIO_URL)));
            } catch (ActivityNotFoundException ignored) {
                // No browser installed.
            }
        });
        col.addView(credit, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        TextView version = text("Music AOD " + versionName(), 11, 0xFF55555A, false);
        version.setGravity(Gravity.CENTER);
        col.addView(version, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));

        neon = ValueAnimator.ofFloat(0f, 1f);
        neon.setDuration(1600);
        neon.setRepeatCount(ValueAnimator.INFINITE);
        neon.setRepeatMode(ValueAnimator.REVERSE);
        neon.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            name.setTextColor(VisualizerView.blend(0xFFB9F6FF, 0xFFFBCFFF, t));
            name.setShadowLayer(8 * dp + 6 * dp * t, 0, 0, VisualizerView.blend(0xFF22D3EE, 0xFFE879F9, t));
        });
    }

    private void buildColours(LinearLayout card) {
        paletteName = text("", 14, TEXT, false);
        card.addView(paletteName);
        LinearLayout row = new LinearLayout(this);
        for (int i = 0; i < AodPrefs.PALETTE_NAMES.length; i++) {
            if (i == AodPrefs.PALETTE_ALBUM && !songInfo) continue; // needs the album art
            final int index = i;
            SwatchView s = new SwatchView(this, index);
            s.setOnClickListener(v -> {
                prefs.palette = index;
                changed();
                refreshColours();
            });
            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(px(42), px(42));
            lp.setMargins(px(4), px(8), px(8), px(8));
            row.addView(s, lp);
            swatches.add(s);
        }
        HorizontalScrollView scroller = new HorizontalScrollView(this);
        scroller.setHorizontalScrollBarEnabled(false);
        scroller.addView(row);
        card.addView(scroller);

        customColors = new LinearLayout(this);
        customColors.setOrientation(LinearLayout.VERTICAL);
        slider(customColors, "Colour 1", "°", 0, 359, prefs.customHue1, v -> {
            prefs.customHue1 = v;
            changed();
            refreshColours();
        });
        slider(customColors, "Colour 2", "°", 0, 359, prefs.customHue2, v -> {
            prefs.customHue2 = v;
            changed();
            refreshColours();
        });
        card.addView(customColors);
        refreshColours();
    }

    private void refreshColours() {
        String name = AodPrefs.PALETTE_NAMES[prefs.palette];
        if (prefs.palette == AodPrefs.PALETTE_ALBUM) name += " — matches each song's cover";
        paletteName.setText(name);
        customColors.setVisibility(prefs.palette == AodPrefs.PALETTE_CUSTOM ? View.VISIBLE : View.GONE);
        for (SwatchView s : swatches) s.invalidate();
    }

    private void refreshSetup() {
        setupList.removeAllViews();
        NotificationManager nm = getSystemService(NotificationManager.class);
        boolean mic = prefs.rhythmMode || micGranted();
        boolean notif = checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) == PackageManager.PERMISSION_GRANTED;
        boolean fsi = nm.canUseFullScreenIntent();
        boolean overlay = Settings.canDrawOverlays(this);
        boolean listener = nm.isNotificationListenerAccessGranted(new ComponentName(this, MediaListener.class));
        boolean battery = getSystemService(PowerManager.class).isIgnoringBatteryOptimizations(getPackageName());

        setupDone = 0;
        setupTotal = 0;
        setupRow(mic, "Microphone", prefs.rhythmMode ? "Not needed in Rhythm mode" : "Reads the music to move the bars",
                () -> requestPermissions(new String[]{Manifest.permission.RECORD_AUDIO}, 1));
        setupRow(notif, "Notifications", "Needed to stay running in the background",
                () -> requestPermissions(new String[]{Manifest.permission.POST_NOTIFICATIONS}, 2));
        setupRow(fsi, "Full-screen alerts", "Lets it light up the screen after you lock the phone",
                () -> openSettings(new Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, packageUri())));
        setupRow(overlay, "Display over other apps", "Lets it open on top of the lock screen",
                () -> openSettings(new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, packageUri())));
        if (songInfo) {
            setupRow(listener, "Song info + controls",
                    "Shows the song and makes the buttons work. Switch greyed out? Settings → Apps → Music AOD → ⋮ → Allow restricted settings",
                    this::openListenerSettings);
        }
        setupRow(battery, "No battery restriction", "Stops the phone from closing Music AOD",
                () -> openSettings(new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS, packageUri())));

        setupTitle.setText(setupDone == setupTotal ? "Setup  ✅ all done"
                : "Setup  " + setupDone + "/" + setupTotal + " — tap a row to fix it");
    }

    private void setupRow(boolean ok, String title, String detail, Runnable fix) {
        setupTotal++;
        if (ok) setupDone++;
        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.VERTICAL);
        row.setPadding(0, px(10), 0, px(10));
        row.addView(text((ok ? "✅  " : "❌  ") + title, 15, ok ? TEXT : 0xFFFF8A80, false));
        if (!ok) {
            row.addView(text(detail, 12, MUTED, false));
            row.setOnClickListener(v -> fix.run());
            row.setBackgroundResource(android.R.drawable.list_selector_background);
        }
        setupList.addView(row);
    }

    // --- actions ---

    private void changed() {
        prefs.save(this);
        bands.configure(prefs);
        preview.invalidate();
    }

    /** The background service runs while either lock-screen feature is on. */
    private void updateService() {
        Intent service = new Intent(this, WatchService.class);
        if (prefs.musicEnabled || prefs.chargeAnim) startForegroundService(service);
        else stopService(service);
    }

    private void restartAudio() {
        bands.stop();
        bands.configure(prefs);
        if (micGranted()) bands.start();
    }

    private boolean micGranted() {
        return checkSelfPermission(Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED;
    }

    private String versionName() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (PackageManager.NameNotFoundException e) {
            return "";
        }
    }

    private Uri packageUri() {
        return Uri.parse("package:" + getPackageName());
    }

    private void openSettings(Intent intent) {
        try {
            startActivity(intent);
        } catch (ActivityNotFoundException e) {
            startActivity(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, packageUri()));
        }
    }

    private void openListenerSettings() {
        Intent detail = new Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME,
                        new ComponentName(this, MediaListener.class).flattenToString());
        try {
            startActivity(detail);
        } catch (ActivityNotFoundException e) {
            openSettings(new Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS));
        }
    }

    // --- small UI helpers ---

    private int px(float dps) {
        return Math.round(dps * dp);
    }

    private TextView text(String s, float sp, int color, boolean bold) {
        TextView t = new TextView(this);
        t.setText(s);
        t.setTextSize(sp);
        t.setTextColor(color);
        if (bold) t.setTypeface(Typeface.create("sans-serif-medium", Typeface.NORMAL));
        t.setPadding(0, px(4), 0, px(4));
        return t;
    }

    private LinearLayout card(LinearLayout parent, String title) {
        LinearLayout c = new LinearLayout(this);
        c.setOrientation(LinearLayout.VERTICAL);
        int p = px(16);
        c.setPadding(p, px(12), p, px(12));
        GradientDrawable bg = new GradientDrawable();
        bg.setColor(CARD);
        bg.setCornerRadius(20 * dp);
        c.setBackground(bg);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(14);
        parent.addView(c, lp);
        if (title != null) c.addView(text(title.toUpperCase(), 12, MUTED, true));
        return c;
    }

    private Switch toggle(LinearLayout parent, String label, boolean value, Consumer<Boolean> onChange) {
        Switch s = new Switch(this);
        s.setText(label);
        s.setTextSize(15);
        s.setTextColor(TEXT);
        s.setChecked(value);
        s.setPadding(0, px(10), 0, px(10));
        s.setOnCheckedChangeListener((b, on) -> onChange.accept(on));
        parent.addView(s, new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        return s;
    }

    private void slider(LinearLayout parent, String label, String unit, int min, int max, int value, IntConsumer onChange) {
        TextView t = text(label + "   " + value + unit, 14, TEXT, false);
        t.setPadding(0, px(10), 0, 0);
        SeekBar sb = new SeekBar(this);
        sb.setMin(min);
        sb.setMax(max);
        sb.setProgress(value);
        sb.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int v, boolean fromUser) {
                t.setText(label + "   " + v + unit);
                if (fromUser) onChange.accept(v);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });
        parent.addView(t);
        parent.addView(sb);
    }

    private void button(LinearLayout parent, String label, Runnable action) {
        Button b = new Button(this);
        b.setText(label);
        b.setAllCaps(false);
        b.setOnClickListener(v -> action.run());
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        lp.topMargin = px(8);
        parent.addView(b, lp);
    }

    /** A round colour-theme button; Album art shows a rainbow, Custom shows the two chosen hues. */
    private class SwatchView extends View {
        private final int index;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Paint ringPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

        SwatchView(Context context, int index) {
            super(context);
            this.index = index;
            ringPaint.setStyle(Paint.Style.STROKE);
            ringPaint.setStrokeWidth(2.5f * dp);
            ringPaint.setColor(Color.WHITE);
            setContentDescription(AodPrefs.PALETTE_NAMES[index]);
        }

        @Override
        protected void onDraw(Canvas canvas) {
            float w = getWidth(), h = getHeight(), r = Math.min(w, h) / 2f - 4 * dp;
            if (index == AodPrefs.PALETTE_ALBUM) {
                paint.setShader(new SweepGradient(w / 2, h / 2,
                        new int[]{0xFFF43F5E, 0xFFFBBF24, 0xFFA3E635, 0xFF22D3EE, 0xFF6366F1, 0xFFE879F9, 0xFFF43F5E}, null));
            } else {
                int[] c = index == AodPrefs.PALETTE_CUSTOM
                        ? new int[]{AodPrefs.hueColor(prefs.customHue1), AodPrefs.hueColor(prefs.customHue2)}
                        : AodPrefs.PALETTES[index];
                paint.setShader(new LinearGradient(0, 0, w, h, c[0], c[1], Shader.TileMode.CLAMP));
            }
            canvas.drawCircle(w / 2, h / 2, r, paint);
            if (prefs.palette == index) canvas.drawCircle(w / 2, h / 2, r + 2.5f * dp, ringPaint);
        }
    }
}
