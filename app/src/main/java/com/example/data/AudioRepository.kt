package com.example.data

import android.content.ContentUris
import android.content.Context
import android.net.Uri
import android.provider.MediaStore
import android.util.Log
import com.example.playback.DemoAudioGenerator
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext
import java.io.File

class AudioRepository(private val context: Context) {
    private val TAG = "AudioRepository"
    private val db = VantaWaveDatabase.getInstance(context)
    private val favoriteDao = db.favoriteDao()
    private val playlistDao = db.playlistDao()

    private val _tracks = MutableStateFlow<List<AudioTrack>>(emptyList())
    val tracks: StateFlow<List<AudioTrack>> = _tracks.asStateFlow()

    val favoriteIds: Flow<List<String>> = favoriteDao.getAllFavoriteTrackIds()
    val playlists: Flow<List<PlaylistEntity>> = playlistDao.getAllPlaylists()

    suspend fun refreshTracks() = withContext(Dispatchers.IO) {
        val trackList = mutableListOf<AudioTrack>()
        val favIds = try {
            favoriteDao.getAllFavoriteTrackIds().first().toSet()
        } catch (e: Exception) {
            emptySet()
        }

        // 1. Scan device media files if permission allows
        try {
            val projection = arrayOf(
                MediaStore.Audio.Media._ID,
                MediaStore.Audio.Media.TITLE,
                MediaStore.Audio.Media.ARTIST,
                MediaStore.Audio.Media.ALBUM,
                MediaStore.Audio.Media.DURATION,
                MediaStore.Audio.Media.ALBUM_ID
            )

            val selection = "${MediaStore.Audio.Media.IS_MUSIC} != 0 AND ${MediaStore.Audio.Media.DURATION} > 5000"
            val sortOrder = "${MediaStore.Audio.Media.TITLE} ASC"

            context.contentResolver.query(
                MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                projection,
                selection,
                null,
                sortOrder
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media._ID)
                val titleCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.TITLE)
                val artistCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ARTIST)
                val albumCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.DURATION)
                val albumIdCol = cursor.getColumnIndexOrThrow(MediaStore.Audio.Media.ALBUM_ID)

                while (cursor.moveToNext()) {
                    val id = cursor.getLong(idCol)
                    val title = cursor.getString(titleCol) ?: "Unknown Title"
                    val artist = cursor.getString(artistCol) ?: "Unknown Artist"
                    val album = cursor.getString(albumCol) ?: "Unknown Album"
                    val duration = cursor.getLong(durCol)
                    val albumId = cursor.getLong(albumIdCol)

                    val contentUri = ContentUris.withAppendedId(MediaStore.Audio.Media.EXTERNAL_CONTENT_URI, id)
                    val artUri = "content://media/external/audio/albumart/$albumId"

                    val trackId = "media_$id"
                    trackList.add(
                        AudioTrack(
                            id = trackId,
                            uriString = contentUri.toString(),
                            title = title,
                            artist = if (artist == "<unknown>") "Unknown Artist" else artist,
                            album = album,
                            durationMs = duration,
                            albumArtUri = artUri,
                            isDemo = false,
                            isFavorite = favIds.contains(trackId)
                        )
                    )
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "MediaStore query note: ${e.message}")
        }

        // 2. Generate and add built-in demo tracks
        try {
            val demoFiles = DemoAudioGenerator.ensureDemoTracksExist(context)
            for (file in demoFiles) {
                val demoInfo = DemoAudioGenerator.DEMO_TRACKS.find { it.filename == file.name }
                val title = demoInfo?.title ?: file.nameWithoutExtension
                val artist = demoInfo?.artist ?: "VantaWave Original"
                val id = "demo_${file.name}"
                val uri = Uri.fromFile(file).toString()

                trackList.add(
                    AudioTrack(
                        id = id,
                        uriString = uri,
                        title = title,
                        artist = artist,
                        album = "VantaWave Synthwave Edition",
                        durationMs = (demoInfo?.durationSeconds ?: 30) * 1000L,
                        albumArtUri = null,
                        isDemo = true,
                        isFavorite = favIds.contains(id)
                    )
                )
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error adding demo tracks", e)
        }

        _tracks.value = trackList
    }

    suspend fun toggleFavorite(trackId: String) = withContext(Dispatchers.IO) {
        val isFav = favoriteDao.isFavorite(trackId)
        if (isFav) {
            favoriteDao.removeFavorite(trackId)
        } else {
            favoriteDao.addFavorite(FavoriteEntity(trackId))
        }
        // Update state in memory
        _tracks.value = _tracks.value.map {
            if (it.id == trackId) it.copy(isFavorite = !isFav) else it
        }
    }

    suspend fun createPlaylist(name: String) = withContext(Dispatchers.IO) {
        val id = "pl_${System.currentTimeMillis()}"
        playlistDao.insertPlaylist(PlaylistEntity(id = id, name = name))
    }

    suspend fun deletePlaylist(playlist: PlaylistEntity) = withContext(Dispatchers.IO) {
        playlistDao.deletePlaylist(playlist)
    }

    suspend fun addTrackToPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.addTrackToPlaylist(PlaylistTrackCrossRef(playlistId = playlistId, trackId = trackId))
    }

    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String) = withContext(Dispatchers.IO) {
        playlistDao.removeTrackFromPlaylist(playlistId = playlistId, trackId = trackId)
    }

    fun getTrackIdsForPlaylist(playlistId: String): Flow<List<String>> {
        return playlistDao.getTrackIdsForPlaylist(playlistId)
    }
}
