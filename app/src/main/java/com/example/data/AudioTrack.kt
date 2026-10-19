package com.example.data

import android.net.Uri

data class AudioTrack(
    val id: String,
    val uriString: String,
    val title: String,
    val artist: String,
    val album: String,
    val durationMs: Long,
    val albumArtUri: String? = null,
    val isDemo: Boolean = false,
    val isFavorite: Boolean = false
) {
    val uri: Uri
        get() = Uri.parse(uriString)

    val formattedDuration: String
        get() {
            val totalSeconds = (durationMs / 1000).coerceAtLeast(0)
            val minutes = totalSeconds / 60
            val seconds = totalSeconds % 60
            return "%d:%02d".format(minutes, seconds)
        }
}
