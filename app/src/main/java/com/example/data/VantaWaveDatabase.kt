package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [
        PlaylistEntity::class,
        PlaylistTrackCrossRef::class,
        FavoriteEntity::class,
        VisualizerProject::class,
        ExportSettings::class,
        ExportRecordEntity::class,
        LyricsSegmentEntity::class,
        WordTimestampEntity::class
    ],
    version = 3,
    exportSchema = false
)
abstract class VantaWaveDatabase : RoomDatabase() {
    abstract fun playlistDao(): PlaylistDao
    abstract fun favoriteDao(): FavoriteDao
    abstract fun visualizerProjectDao(): VisualizerProjectDao
    abstract fun exportSettingsDao(): ExportSettingsDao
    abstract fun exportRecordDao(): ExportRecordDao
    abstract fun lyricsDao(): LyricsDao

    companion object {
        @Volatile
        private var INSTANCE: VantaWaveDatabase? = null

        fun getInstance(context: Context): VantaWaveDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    VantaWaveDatabase::class.java,
                    "vantawave.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
