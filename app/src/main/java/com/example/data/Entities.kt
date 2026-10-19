package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "playlists")
data class PlaylistEntity(
    @PrimaryKey
    val id: String,
    val name: String,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "playlist_tracks", primaryKeys = ["playlistId", "trackId"])
data class PlaylistTrackCrossRef(
    val playlistId: String,
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "favorites")
data class FavoriteEntity(
    @PrimaryKey
    val trackId: String,
    val addedAt: Long = System.currentTimeMillis()
)

typealias VisualizerProjectEntity = VisualizerProject
typealias ExportSettingsEntity = ExportSettings

@Entity(tableName = "export_records")
data class ExportRecordEntity(
    @PrimaryKey
    val id: String,
    val title: String,
    val videoUriString: String,
    val filePath: String,
    val durationMs: Long,
    val resolution: String, // e.g. "1080x1920"
    val aspectRatio: String, // e.g. "9:16"
    val fileSizeBytes: Long,
    val createdAt: Long = System.currentTimeMillis()
)

enum class VisualizerMode(val displayName: String, val description: String) {
    SPECTRUM_BARS("Spectrum Bars", "Dynamic vertical frequency columns with peak indicators"),
    CIRCULAR_SPECTRUM("Circular Spectrum", "Radial audio bars pulsing outward around an orbital center"),
    WAVEFORM("Waveform", "Fluid audio frequency wave reacting to amplitude transients"),
    RADIAL_RINGS("Radial Rings", "Concentric bass-reactive shockwaves expanding outward"),
    PARTICLE_BURST("Particle Burst", "Kinetic energy particles that explode with audio transients"),
    NEON_LIQUID_WAVE("Neon Wave", "Smooth multi-layered neon fluid wave reacting to melody"),
    CYBER_GRID("Cyber Grid", "Retro synthwave 3D perspective grid pulsing with bass"),
    VINYL_ORBIT("Vinyl Orbit", "Spinning vinyl record with reactive neon aura and sound waves"),
    GLITCH_SPECTRUM("Glitch Pulse", "Cybernetic bars with chromatic aberration and bass shockwaves"),
    STARFIELD_DRIFT("Cosmic Drift", "3D warping hyperspace starfield accelerating on beats")
}

data class VisualizerPreset(
    val id: String,
    val name: String,
    val mode: VisualizerMode,
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val backgroundColorHex: String,
    val glowIntensity: Float = 0.8f,
    val sensitivity: Float = 1.0f,
    val barCount: Int = 48,
    val particleCount: Int = 120
) {
    companion object {
        val PRESET_CYBERWAVE = VisualizerPreset(
            id = "preset_cyberwave",
            name = "Cyberwave",
            mode = VisualizerMode.CIRCULAR_SPECTRUM,
            primaryColorHex = "#8B5CF6", // Electric Purple
            secondaryColorHex = "#06B6D4", // Neon Cyan
            backgroundColorHex = "#0A0A0F",
            glowIntensity = 0.85f,
            sensitivity = 1.2f,
            barCount = 64,
            particleCount = 140
        )

        val PRESET_NEON_PULSE = VisualizerPreset(
            id = "preset_neon_pulse",
            name = "Neon Pulse",
            mode = VisualizerMode.SPECTRUM_BARS,
            primaryColorHex = "#EC4899", // Neon Pink
            secondaryColorHex = "#8B5CF6", // Electric Purple
            backgroundColorHex = "#08060E",
            glowIntensity = 0.9f,
            sensitivity = 1.1f,
            barCount = 52,
            particleCount = 100
        )

        val PRESET_DARK_SPECTRUM = VisualizerPreset(
            id = "preset_dark_spectrum",
            name = "Dark Spectrum",
            mode = VisualizerMode.SPECTRUM_BARS,
            primaryColorHex = "#38BDF8", // Ice Blue
            secondaryColorHex = "#6366F1", // Indigo
            backgroundColorHex = "#05070D",
            glowIntensity = 0.65f,
            sensitivity = 1.0f,
            barCount = 48,
            particleCount = 80
        )

        val PRESET_AURORA = VisualizerPreset(
            id = "preset_aurora",
            name = "Aurora",
            mode = VisualizerMode.PARTICLE_BURST,
            primaryColorHex = "#10B981", // Emerald Green
            secondaryColorHex = "#06B6D4", // Turquoise
            backgroundColorHex = "#050D0A",
            glowIntensity = 0.95f,
            sensitivity = 1.3f,
            barCount = 40,
            particleCount = 180
        )

        val PRESET_MINIMAL_WAVE = VisualizerPreset(
            id = "preset_minimal_wave",
            name = "Minimal Wave",
            mode = VisualizerMode.WAVEFORM,
            primaryColorHex = "#FFFFFF", // Pure White
            secondaryColorHex = "#A855F7", // Subtle Violet
            backgroundColorHex = "#0A0A0F",
            glowIntensity = 0.4f,
            sensitivity = 0.9f,
            barCount = 32,
            particleCount = 50
        )

        val PRESET_RADIAL_BASS = VisualizerPreset(
            id = "preset_radial_bass",
            name = "Radial Bass",
            mode = VisualizerMode.RADIAL_RINGS,
            primaryColorHex = "#F59E0B", // Amber Gold
            secondaryColorHex = "#EF4444", // Crimson Red
            backgroundColorHex = "#0D0705",
            glowIntensity = 0.9f,
            sensitivity = 1.4f,
            barCount = 40,
            particleCount = 120
        )

        val PRESET_NEON_LIQUID = VisualizerPreset(
            id = "preset_neon_liquid",
            name = "Neon Liquid",
            mode = VisualizerMode.NEON_LIQUID_WAVE,
            primaryColorHex = "#F472B6", // Soft Pink
            secondaryColorHex = "#60A5FA", // Sky Blue
            backgroundColorHex = "#0F172A", // Deep Navy
            glowIntensity = 0.95f,
            sensitivity = 1.2f,
            barCount = 64,
            particleCount = 100
        )

        val PRESET_CYBER_GRID = VisualizerPreset(
            id = "preset_cyber_grid",
            name = "Cyber Grid",
            mode = VisualizerMode.CYBER_GRID,
            primaryColorHex = "#A855F7", // Purple
            secondaryColorHex = "#EC4899", // Pink
            backgroundColorHex = "#050505", // Black
            glowIntensity = 0.8f,
            sensitivity = 1.1f,
            barCount = 48,
            particleCount = 150
        )

        val ALL_PRESETS = listOf(
            PRESET_CYBERWAVE,
            PRESET_NEON_PULSE,
            PRESET_DARK_SPECTRUM,
            PRESET_AURORA,
            PRESET_MINIMAL_WAVE,
            PRESET_RADIAL_BASS,
            PRESET_NEON_LIQUID,
            PRESET_CYBER_GRID
        )
    }
}
