package com.sanstudio.sanbar

import android.Manifest
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.provider.Settings
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Accent = Color(0xFF4ADE80)
private val Bg = Color(0xFF0B0F0C)
private val CardBg = Color(0xFF16201A)
private val Dim = Color(0xB3FFFFFF)

private val PRESETS = listOf(
    0xFF4ADE80, 0xFF22D3EE, 0xFF3B82F6, 0xFFA78BFA,
    0xFFF472B6, 0xFFF43F5E, 0xFFFB923C, 0xFFFACC15, 0xFFFFFFFF,
).map { it.toInt() }

/** Setup (permissions, add widget) + Customize (colours, RGB effects, visualizer). */
class MainActivity : ComponentActivity() {

    private var refreshKey by mutableIntStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(primary = Accent, background = Bg, surface = CardBg)) {
                Screen(refreshKey)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        refreshKey++
        SanBarWidget.wakeListener(this)
        WidgetRenderer.render(this, force = true)
    }

    private fun applied() {
        WidgetRenderer.render(this, force = true)
        StatsRenderer.render(this, force = true)
    }

    private fun pin(provider: Class<*>) {
        val awm = AppWidgetManager.getInstance(this)
        if (awm.isRequestPinAppWidgetSupported) awm.requestPinAppWidget(ComponentName(this, provider), null, null)
    }

    @Composable
    private fun Screen(@Suppress("UNUSED_PARAMETER") key: Int) {
        val musicOk = MediaRepo.hasAccess(this)
        val btOk = BatteryRepo.hasBluetoothPermission(this)
        val btLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) {
            refreshKey++
            applied()
        }

        Column(
            Modifier
                .fillMaxSize()
                .safeDrawingPadding()
                .verticalScroll(rememberScrollState())
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
        ) {
            Text("SanBar", color = Accent, fontSize = 32.sp, fontWeight = FontWeight.Bold)
            Text("Music controls + phone & headphone battery in one full-width home screen bar.", color = Dim, fontSize = 14.sp)

            if (!musicOk || !btOk) {
                Step(1, "Music access", "Shows the song and controls any music app. Android lists it as \"Notification access\"; SanBar turns every notification type off and never reads them.", musicOk, "Allow") {
                    val cn = ComponentName(this@MainActivity, MediaListenerService::class.java)
                    try {
                        startActivity(
                            Intent(Settings.ACTION_NOTIFICATION_LISTENER_DETAIL_SETTINGS)
                                .putExtra(Settings.EXTRA_NOTIFICATION_LISTENER_COMPONENT_NAME, cn.flattenToString()),
                        )
                    } catch (e: Exception) {
                        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS))
                    }
                }
                Step(2, "Headphone battery", "\"Nearby devices\" permission, to read your Bluetooth headphones' name and battery.", btOk, "Allow") {
                    btLauncher.launch(Manifest.permission.BLUETOOTH_CONNECT)
                }
            }

            Customize()

            Step(0, "Add the music bar", "Long-press the home screen → Widgets → SanBar. 1 row high = slim bar, 2 rows = big card.", false, "Add SanBar") {
                pin(SanBarWidget::class.java)
            }
            Step(0, "Add SanStats", "CPU usage, RAM and temperature rings (2×1). Stretch to 2 rows for GHz, GB and heat level. GPU isn't shown: vivo blocks it for normal apps.", false, "Add SanStats") {
                pin(StatsWidget::class.java)
            }
        }
    }

    // ---------------------------------------------------------------- Customize

    @Composable
    private fun Customize() {
        val ctx = this
        var accent by remember { mutableIntStateOf(Prefs.accent(ctx)) }
        var hue by remember { mutableFloatStateOf(hueOf(Prefs.accent(ctx))) }
        var album by remember { mutableStateOf(Prefs.albumColor(ctx)) }
        var glass by remember { mutableFloatStateOf(Prefs.glassAlpha(ctx).toFloat()) }
        var fx by remember { mutableStateOf(Prefs.fx(ctx)) }
        var fxFast by remember { mutableStateOf(Prefs.fxFast(ctx)) }
        var viz by remember { mutableStateOf(Prefs.viz(ctx)) }

        val micLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
            if (granted) {
                viz = Prefs.VIZ_REAL
                Prefs.setViz(ctx, Prefs.VIZ_REAL)
                applied()
            }
        }

        Section("Colour") {
            Row(
                Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                PRESETS.forEach { c ->
                    val selected = !album && c == accent
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(Color(c))
                            .border(if (selected) 3.dp else 0.dp, Color.White, CircleShape)
                            .clickable {
                                accent = c
                                hue = hueOf(c)
                                album = false
                                Prefs.setAccent(ctx, c)
                                Prefs.setAlbumColor(ctx, false)
                                applied()
                            },
                    )
                }
            }
            Label("Any colour (RGB / hue)")
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(10.dp)
                    .clip(RoundedCornerShape(5.dp))
                    .background(Brush.horizontalGradient((0..6).map { Color.hsv(it * 60f % 360f, 0.75f, 1f) })),
            )
            Slider(
                value = hue,
                onValueChange = {
                    hue = it
                    accent = android.graphics.Color.HSVToColor(floatArrayOf(it, 0.72f, 1f))
                },
                onValueChangeFinished = {
                    album = false
                    Prefs.setAccent(ctx, accent)
                    Prefs.setAlbumColor(ctx, false)
                    applied()
                },
                valueRange = 0f..360f,
                colors = SliderDefaults.colors(thumbColor = Color(accent), activeTrackColor = Color(accent)),
            )
            SwitchRow("Album colour", "Accent follows the song cover", album) {
                album = it
                Prefs.setAlbumColor(ctx, it)
                applied()
            }
            Label("Glass darkness")
            Slider(
                value = glass,
                onValueChange = { glass = it },
                onValueChangeFinished = {
                    Prefs.setGlassAlpha(ctx, glass.toInt())
                    applied()
                },
                valueRange = 0f..255f,
            )
        }

        Section("RGB effect") {
            Chips(
                listOf(Prefs.FX_OFF to "Off", Prefs.FX_SPIN to "Rainbow spin", Prefs.FX_CYCLE to "Colour cycle", Prefs.FX_BREATHE to "Breathing"),
                fx,
            ) {
                fx = it
                Prefs.setFx(ctx, it)
                applied()
            }
            SwitchRow("Fast", "Faster animation", fxFast) {
                fxFast = it
                Prefs.setFxFast(ctx, it)
                applied()
            }
            Text("Runs inside the home screen itself: almost no battery for SanBar.", color = Dim, fontSize = 12.sp)
        }

        var barClear by remember { mutableStateOf(Prefs.barTransparent(ctx)) }
        Section("Music bar") {
            SwitchRow("Transparent background", "Clean look: no box, only the controls", barClear) {
                barClear = it
                Prefs.setBarTransparent(ctx, it)
                applied()
            }
        }

        var statsClear by remember { mutableStateOf(Prefs.statsTransparent(ctx)) }
        Section("SanStats") {
            SwitchRow("Transparent background", "Only the rings and text, no box", statsClear) {
                statsClear = it
                Prefs.setStatsTransparent(ctx, it)
                applied()
            }
        }

        Section("Visualizer") {
            Chips(
                listOf(Prefs.VIZ_OFF to "Off", Prefs.VIZ_ANIMATED to "Animated", Prefs.VIZ_REAL to "Real (beat)"),
                viz,
            ) {
                if (it == Prefs.VIZ_REAL &&
                    checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED
                ) {
                    micLauncher.launch(Manifest.permission.RECORD_AUDIO)
                } else {
                    viz = it
                    Prefs.setViz(ctx, it)
                    applied()
                }
            }
            Text(
                when (viz) {
                    Prefs.VIZ_REAL -> "Bars follow the real music. Uses Android's Visualizer, which reads the sound going to your speaker/headphones, NOT the microphone; Android still files it under the \"Microphone\" permission. Runs only while music plays and the screen is on."
                    Prefs.VIZ_ANIMATED -> "Bars dance while music plays. No permission, almost no battery."
                    else -> "No bars."
                },
                color = Dim,
                fontSize = 12.sp,
            )
        }
    }

    // ---------------------------------------------------------------- building blocks

    @Composable
    private fun Section(title: String, content: @Composable () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text(title, color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold)
                content()
            }
        }
    }

    @Composable
    private fun Label(text: String) = Text(text, color = Dim, fontSize = 13.sp)

    @Composable
    private fun SwitchRow(title: String, sub: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text(title, color = Color.White, fontSize = 15.sp)
                Text(sub, color = Dim, fontSize = 12.sp)
            }
            Switch(
                checked = checked,
                onCheckedChange = onChange,
                colors = SwitchDefaults.colors(checkedTrackColor = Accent, checkedThumbColor = Color.Black),
            )
        }
    }

    @Composable
    private fun Chips(options: List<Pair<String, String>>, selected: String, onSelect: (String) -> Unit) {
        Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            options.forEach { (value, label) ->
                FilterChip(
                    selected = value == selected,
                    onClick = { onSelect(value) },
                    label = { Text(label) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Accent,
                        selectedLabelColor = Color.Black,
                        labelColor = Color.White,
                    ),
                )
            }
        }
    }

    @Composable
    private fun Step(number: Int, title: String, body: String, done: Boolean, button: String, onClick: () -> Unit) {
        Card(colors = CardDefaults.cardColors(containerColor = CardBg), shape = RoundedCornerShape(20.dp), modifier = Modifier.fillMaxWidth()) {
            Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (number > 0) "$number. $title" else title,
                        color = Color.White, fontSize = 18.sp, fontWeight = FontWeight.SemiBold, modifier = Modifier.weight(1f),
                    )
                    if (done) Text("✓ Done", color = Accent, fontWeight = FontWeight.Bold)
                }
                Text(body, color = Dim, fontSize = 13.sp)
                if (!done) {
                    Spacer(Modifier.width(0.dp))
                    Button(onClick = onClick, colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color.Black)) {
                        Text(button, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }

    private fun hueOf(color: Int): Float {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(color, hsv)
        return hsv[0]
    }
}
