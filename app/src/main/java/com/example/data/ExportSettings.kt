package com.example.data

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Room entity representing video export settings.
 * Defines unique ID, metadata, resolution, aspect ratio, frame rate,
 * duration parameters, timestamps, and dual-scope scoping (global and project-specific).
 */
@Entity(tableName = "export_settings")
data class ExportSettings(
    @PrimaryKey
    val id: String, // "export_global" or "export_proj_<projectId>"
    val projectId: String? = null,
    val aspectRatioName: String = "9:16 HD",
    val width: Int = 720,
    val height: Int = 1280,
    val fps: Int = 30,
    val durationSeconds: Int = 15,
    val isFullTrack: Boolean = false,
    val lastUsedAt: Long = System.currentTimeMillis()
) {
    companion object {
        const val GLOBAL_ID = "export_global"
    }
}
