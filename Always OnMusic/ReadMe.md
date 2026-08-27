# 🌌 SanStudio Aura

### Real-Time Audio → Visual Intelligence for Android

**SanStudio Aura** is an advanced Android audio-reactive visual engine that transforms music into dynamic, intelligent visual experiences.

Instead of simply displaying traditional equalizer bars, Aura analyzes real-time audio characteristics such as **FFT frequency data, bass, mids, treble, RMS energy, peaks, and beat events**, then converts them into GPU-accelerated visual effects.

The long-term goal is to bring this experience into **OLED-friendly lock-screen and Always-On Display environments wherever Android and the device manufacturer provide legitimate support**.

> **Turn your music into light.**

---

## ✨ Core Concept

```text
Music
  ↓
Media / Audio Source
  ↓
Audio Analysis
  ↓
FFT + DSP
  ↓
Frequency Bands
  ↓
Beat Detection
  ↓
Visual State Engine
  ↓
GPU Renderer
  ↓
Audio-Reactive Visual Experience
```

---

## 🚀 What Makes Aura Different?

Most music visualizers simply map audio volume to moving bars.

Aura is designed as a complete **audio-to-visual engine**.

```text
Bass       → Energy / Scale
Mid        → Shape / Motion
Treble     → Particles / Detail
Beat       → Pulse / Impact
RMS        → Global Intensity
Spectrum   → Color
Album Art  → Dynamic Palette
```

The result is a visualizer that reacts to the **character of the music**, not just its volume.

---

## 🖤 OLED-First Design

Aura is designed specifically with OLED displays and screen-off experiences in mind.

The rendering engine is built around:

* OLED-safe brightness
* Pixel shifting
* Reduced static content
* Dark backgrounds
* Adaptive frame rates
* Battery-aware rendering
* Burn-in-conscious animation
* Minimal illuminated pixels

The goal is not simply to make the visualizer beautiful.

The goal is to make it **beautiful without unnecessarily wasting power**.

---

## 🎧 Real-Time Audio Analysis

Aura's audio engine can analyze:

* Sub-bass
* Bass
* Low-mid
* Mid
* Upper-mid
* Treble
* High-treble
* RMS energy
* Peak level
* Spectral energy
* Beat events

Android's `Visualizer` API provides waveform and FFT data for visualization, while Android's playback-capture framework can provide eligible playback from other apps when the required permissions, user authorization, and source-app policies allow it.

---

## 🎨 Visual Engine

Aura is designed around multiple visual systems instead of a single equalizer.

### Current / Planned Visual Themes

* 🌈 Neon Wave
* 🌌 Galaxy Pulse
* ⚡ Cyber Grid
* 🌊 Liquid Light
* 🌠 Aurora
* 📊 Spectrum
* 🔵 Ring Pulse
* ✨ Particle Field
* 🖤 Minimal OLED
* 🎨 Custom Themes

Each visual system can respond differently to the audio signal.

---

## 🧠 Device-Aware Architecture

Android devices do not provide identical capabilities.

Aura therefore uses a capability-driven architecture:

```text
              Device
                 ↓
        Capability Detection
                 ↓
       ┌─────────┼─────────┐
       ↓         ↓         ↓
      AOD      Lock      Fullscreen
   Supported  Supported   Supported
       ↓         ↓         ↓
          Best Available
             Experience
```

Aura will never pretend that a device supports a feature when Android does not provide a legitimate way to implement it.

---

## 🔋 Adaptive Performance

The visualizer automatically adapts to device conditions.

```text
High Battery
     ↓
High Quality / High FPS

Normal Battery
     ↓
Balanced Rendering

Low Battery
     ↓
Reduced FPS / Effects

Critical Battery
     ↓
Minimal or Disabled
```

The same principle applies to thermal conditions and device performance.

---

## 🛠️ Technology Stack

### Android

* Kotlin
* Jetpack Compose
* AndroidX
* Coroutines
* Flow
* Hilt
* DataStore
* Media3

### Audio

* Android Visualizer
* FFT
* AudioPlaybackCapture where supported
* RMS / Peak analysis
* Beat detection
* Frequency-band processing

### Graphics

* Canvas
* Hardware-accelerated rendering
* OpenGL ES where appropriate
* Shader-based effects
* Particle systems

### Architecture

* Modular architecture
* MVVM / MVI
* Clean separation of audio, rendering and UI
* Capability-based device abstraction

### Testing

* JUnit
* Android Instrumentation Tests
* Compose UI Tests
* Performance / benchmark testing
* Real-device testing

---

## 🏗️ Architecture

```text
SanStudio Aura
│
├── Audio Engine
│   ├── Media Session
│   ├── Audio Capture
│   ├── FFT
│   ├── Frequency Analysis
│   └── Beat Detection
│
├── Visual Engine
│   ├── Visual State
│   ├── Theme Engine
│   ├── Color Engine
│   └── Animation Engine
│
├── Renderer
│   ├── Canvas
│   ├── OpenGL
│   └── Future Renderers
│
├── Device Engine
│   ├── Capability Detection
│   ├── AOD Adapter
│   ├── Lock Screen
│   └── Display Information
│
├── Performance Engine
│   ├── Battery
│   ├── Thermal
│   ├── FPS
│   └── Quality Scaling
│
└── UI
    ├── Home
    ├── Visualizer
    ├── Themes
    ├── Theme Editor
    ├── Settings
    └── Diagnostics
```

---

## 🔬 Project Status

> 🚧 **Active Development**

SanStudio Aura is being developed as a real Android product rather than a static visual prototype.

The project focuses on building the underlying audio-processing, rendering, device-capability, performance and OLED-safety systems required for a production-quality experience.

---

## 🗺️ Roadmap

### Phase 1 — Foundation

* [ ] Android project architecture
* [ ] MediaSession integration
* [ ] Audio source detection
* [ ] Permission architecture
* [ ] Basic FFT pipeline

### Phase 2 — Audio Intelligence

* [ ] Frequency-band analysis
* [ ] RMS / peak analysis
* [ ] Audio smoothing
* [ ] Beat detection
* [ ] Adaptive normalization

### Phase 3 — Visual Engine

* [ ] Visual state engine
* [ ] Color engine
* [ ] Animation engine
* [ ] Neon Wave
* [ ] Spectrum
* [ ] Ring Pulse

### Phase 4 — Advanced Graphics

* [ ] Particle system
* [ ] GPU rendering
* [ ] Shader effects
* [ ] Galaxy Pulse
* [ ] Liquid Light
* [ ] Aurora

### Phase 5 — OLED Experience

* [ ] OLED-safe rendering
* [ ] Pixel shifting
* [ ] Adaptive brightness
* [ ] Adaptive FPS
* [ ] Battery-aware rendering
* [ ] Screen-off / lock-screen capability detection

### Phase 6 — Customization

* [ ] Theme editor
* [ ] Custom colors
* [ ] Custom gradients
* [ ] Sensitivity controls
* [ ] Animation controls
* [ ] User presets

### Phase 7 — Device Compatibility

* [ ] Capability detection
* [ ] Generic Android fallback
* [ ] OEM-specific adapters where officially supported
* [ ] Physical-device testing

### Phase 8 — Production

* [ ] Automated testing
* [ ] Performance profiling
* [ ] Battery profiling
* [ ] Crash handling
* [ ] Accessibility
* [ ] Release build
* [ ] Documentation

---

## ⚠️ Platform Limitations

Android does not provide a universal public API that allows every third-party application to replace the manufacturer's Always-On Display.

Therefore Aura uses a **capability-first architecture**.

Depending on the device, Android version and available APIs, Aura may provide:

* True supported AOD integration
* Lock-screen visualization
* Screen-off-style experience
* Fullscreen visualizer
* In-app visualizer

Aura will never use undocumented or restricted Android APIs simply to simulate unsupported functionality.

---

## 🔐 Privacy

Aura is designed with local processing in mind.

The project does not require uploading music or raw audio to a server for visualization.

Audio analysis should happen locally whenever possible.

The application should not store raw audio as part of the visualization pipeline.

---

## 📚 Documentation

Detailed technical documentation will be available in:

* `ARCHITECTURE.md`
* `AUDIO_ENGINE.md`
* `RENDERING.md`
* `AOD_COMPATIBILITY.md`
* `OLED_SAFETY.md`
* `BATTERY.md`
* `TESTING.md`

---

## 👨‍💻 Developed By

### SanStudio

**Developed by SanStudio**

[Visit SanStudio](https://sanstudio-hub.github.io/in/)

---

## 📄 License

License information will be added as the project approaches its first public release.

---

### Project Philosophy

> **Don't make the music move.**
>
> **Make the music become the movement.**

**SanStudio Aura — Turn your music into light.**
