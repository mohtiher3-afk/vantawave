package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface PlaylistDao {
    @Query("SELECT * FROM playlists ORDER BY createdAt DESC")
    fun getAllPlaylists(): Flow<List<PlaylistEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPlaylist(playlist: PlaylistEntity)

    @Delete
    suspend fun deletePlaylist(playlist: PlaylistEntity)

    @Query("SELECT trackId FROM playlist_tracks WHERE playlistId = :playlistId ORDER BY addedAt ASC")
    fun getTrackIdsForPlaylist(playlistId: String): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addTrackToPlaylist(crossRef: PlaylistTrackCrossRef)

    @Query("DELETE FROM playlist_tracks WHERE playlistId = :playlistId AND trackId = :trackId")
    suspend fun removeTrackFromPlaylist(playlistId: String, trackId: String)
}

@Dao
interface FavoriteDao {
    @Query("SELECT trackId FROM favorites ORDER BY addedAt DESC")
    fun getAllFavoriteTrackIds(): Flow<List<String>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun addFavorite(favorite: FavoriteEntity)

    @Query("DELETE FROM favorites WHERE trackId = :trackId")
    suspend fun removeFavorite(trackId: String)

    @Query("SELECT EXISTS(SELECT 1 FROM favorites WHERE trackId = :trackId)")
    suspend fun isFavorite(trackId: String): Boolean
}

@Dao
interface VisualizerProjectDao {
    @Query("SELECT * FROM visualizer_projects ORDER BY lastModifiedAt DESC")
    fun getAllProjects(): Flow<List<VisualizerProjectEntity>>

    @Query("SELECT * FROM visualizer_projects ORDER BY lastModifiedAt DESC LIMIT :limit")
    fun getRecentProjects(limit: Int): Flow<List<VisualizerProjectEntity>>

    @Query("SELECT * FROM visualizer_projects WHERE id = :id LIMIT 1")
    suspend fun getProjectById(id: String): VisualizerProjectEntity?

    @Query("SELECT * FROM visualizer_projects WHERE trackId = :trackId ORDER BY lastModifiedAt DESC")
    fun getProjectsForTrack(trackId: String): Flow<List<VisualizerProjectEntity>>

    @Query("SELECT COUNT(*) FROM visualizer_projects")
    fun getProjectCount(): Flow<Int>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateProject(project: VisualizerProjectEntity)

    @Query("UPDATE visualizer_projects SET lastModifiedAt = :lastModifiedAt WHERE id = :id")
    suspend fun touchProject(id: String, lastModifiedAt: Long = System.currentTimeMillis())

    @Delete
    suspend fun deleteProject(project: VisualizerProjectEntity)

    @Query("DELETE FROM visualizer_projects WHERE id = :id")
    suspend fun deleteProjectById(id: String)

    @Query("DELETE FROM visualizer_projects")
    suspend fun deleteAllProjects()

    @Transaction
    suspend fun cloneProject(sourceId: String, customName: String? = null): VisualizerProjectEntity? {
        val source = getProjectById(sourceId) ?: return null
        val newCloneCount = source.cloneCount + 1
        val newId = "proj_${System.currentTimeMillis()}"
        val newName = customName ?: "${source.name} (Copy $newCloneCount)"
        val cloned = source.copy(
            id = newId,
            name = newName,
            cloneCount = 0,
            createdAt = System.currentTimeMillis(),
            lastModifiedAt = System.currentTimeMillis()
        )
        // Update source clone count and insert cloned project
        insertOrUpdateProject(source.copy(cloneCount = newCloneCount))
        insertOrUpdateProject(cloned)
        return cloned
    }
}

@Dao
interface ExportSettingsDao {
    @Query("SELECT * FROM export_settings WHERE id = :id LIMIT 1")
    fun getSettingsById(id: String): Flow<ExportSettingsEntity?>

    @Query("SELECT * FROM export_settings WHERE id = 'export_global' LIMIT 1")
    fun getGlobalSettings(): Flow<ExportSettingsEntity?>

    @Query("SELECT * FROM export_settings WHERE projectId = :projectId LIMIT 1")
    fun getSettingsForProject(projectId: String): Flow<ExportSettingsEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(settings: ExportSettingsEntity)

    @Query("DELETE FROM export_settings WHERE projectId = :projectId")
    suspend fun deleteSettingsForProject(projectId: String)
}

@Dao
interface ExportRecordDao {
    @Query("SELECT * FROM export_records ORDER BY createdAt DESC")
    fun getAllExports(): Flow<List<ExportRecordEntity>>

    @Query("SELECT * FROM export_records ORDER BY createdAt DESC LIMIT :limit")
    fun getRecentExports(limit: Int): Flow<List<ExportRecordEntity>>

    @Query("SELECT * FROM export_records WHERE id = :id LIMIT 1")
    suspend fun getExportById(id: String): ExportRecordEntity?

    @Query("SELECT COUNT(*) FROM export_records")
    fun getExportCount(): Flow<Int>

    @Query("SELECT SUM(fileSizeBytes) FROM export_records")
    fun getTotalExportedSizeBytes(): Flow<Long?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExport(export: ExportRecordEntity)

    @Delete
    suspend fun deleteExport(export: ExportRecordEntity)

    @Query("DELETE FROM export_records WHERE id = :id")
    suspend fun deleteExportById(id: String)

    @Query("DELETE FROM export_records")
    suspend fun deleteAllExports()
}
