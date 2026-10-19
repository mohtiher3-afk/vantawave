# VantaWave — Professional Android Music Visualizer & Video Creator

**Music. Visuals. Create.**

VantaWave is a native Android music player, audio-reactive visualization studio, and video export creator designed for musicians, beatmakers, podcasters, and content creators to turn their music into high-definition videos ready for TikTok, Instagram Reels, and YouTube Shorts.

---

## Key Features

### 1. Modern Local Music Player & Library
- **MediaStore Scanner**: Scans on-device audio files across title, artist, album, and duration.
- **Built-in Synthwave Tracks**: Bundled high-energy electronic WAV tracks (`Neon Horizon`, `Cyber Pulse`, `Midnight Echo`) for immediate testing and visualization offline with zero initial setup.
- **Media3 ExoPlayer Engine**: Robust playback, queue management, shuffle, repeat (Off, All, One), and smooth seeking.
- **Background Playback & MediaSession**: Supported via `PlaybackService` with notification and lock-screen controls.
- **Playlists & Favorites**: Room-persisted custom playlists and favorite song curation.
- **Sleep Timer**: Built-in sleep timer with 15m, 30m, 45m, and 60m auto-pause.

### 2. Professional Visualizer Studio (5 Distinct Modes)
1. **Spectrum Bars**: Dynamic vertical frequency columns with gradient color transitions and floating peak indicators.
2. **Circular Spectrum**: Radial audio bars radiating outward around an orbital pulsating center core.
3. **Waveform**: Fluid audio waveform oscillating smoothly with multi-pass neon glow bloom.
4. **Radial Rings**: Multi-layered concentric shockwaves expanding outward on bass drops.
5. **Particle Burst**: High-energy kinetic particles that orbit and explode outward on audio transients.

### 3. Studio Customization & Presets
- **Curated Presets**: *Cyberwave*, *Neon Pulse*, *Dark Spectrum*, *Aurora*, *Minimal Wave*, and *Radial Bass*.
- **Live Controls**: Primary/Secondary color palettes, audio sensitivity (0.4x - 2.2x), glow intensity, bar and particle counts, and song title/artist text overlay.
- **Background Support**: Dark ambient gradients and custom background photo selection via the modern Android Photo Picker.
- **Local Project Storage**: Save, restore, and duplicate visualizer scene configurations.

### 4. Native Hardware-Accelerated Video Export Engine
- **MediaCodec & MediaMuxer Pipeline**: Renders visualizer frames directly to H.264/AVC MP4 without screen capture overlays.
- **Audio/Video Synchronization**: Synchronizes source audio with frame timestamps.
- **Social Media Presets**:
  - `9:16 Vertical HD` (720 × 1280) — Fast export for Reels/TikTok/Shorts.
  - `9:16 Vertical FHD` (1080 × 1920) — Full HD.
  - `1:1 Square` (720 × 720) — Instagram feeds.
  - `16:9 Landscape` (1280 × 720) — YouTube.
  - `4:5 Portrait` (720 × 900) — Instagram portrait posts.
- **Clip Trimming**: Export full song or quick 15s, 30s, or 60s clips.
- **Live Progress & Sharing**: Real-time render progress bar with cancel capability, in-app video playback, and native Android Share Sheet integration. Saved directly to `Movies/VantaWave`.

---

## Technical Stack & Architecture

- **Language**: Kotlin 2.2.10
- **UI Framework**: Jetpack Compose with Material 3
- **Audio Core**: AndroidX Media3 ExoPlayer (`1.5.1`)
- **Audio Analysis**: Android `Visualizer` API (`AudioEffect`) + Software FFT/PCM Analysis
- **Video Export**: Android `MediaCodec`, `MediaMuxer`, `MediaExtractor`
- **Database**: Room 2.7.0 (`VantaWaveDatabase`)
- **Image Loading**: Coil Compose 2.7.0
- **Architecture**: MVVM with Repository pattern and StateFlows
