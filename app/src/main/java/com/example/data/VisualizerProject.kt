package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing a saved music visualizer project.
 * Defines unique ID, metadata, audio track association, configuration parameters,
 * and timestamps for auto-save and project duplication/cloning.
 */
@Entity(tableName = "visualizer_projects")
data class VisualizerProject(
    @PrimaryKey
    val id: String,
    val name: String,
    val trackId: String,
    val trackTitle: String,
    val trackArtist: String,
    val trackUriString: String,
    val trackDurationMs: Long,
    @Deprecated("Use layersJson instead")
    val mode: String, // SPECTRUM_BARS, CIRCULAR_SPECTRUM, WAVEFORM, RADIAL_RINGS, PARTICLE_BURST
    val primaryColorHex: String,
    val secondaryColorHex: String,
    val backgroundColorHex: String,
    val glowIntensity: Float = 0.8f,
    val sensitivity: Float = 1.0f,
    val barCount: Int = 48,
    val particleCount: Int = 120,
    val customImageUri: String? = null,
    val showTextOverlay: Boolean = true,
    val layersJson: String = "[]", // JSON array of VisualEffect
    val cloneCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastModifiedAt: Long = System.currentTimeMillis()
)
