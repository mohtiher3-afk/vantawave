package com.example.data

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Entity
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Transaction
import com.example.transcription.LyricsSegment
import com.example.transcription.WordTimestamp
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "lyrics_segments")
data class LyricsSegmentEntity(
    @PrimaryKey
    val id: String,
    val trackId: String,
    val lineIndex: Int,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val text: String,
    val isRtl: Boolean,
    val confidence: Float,
    val createdAt: Long = System.currentTimeMillis()
)

@Entity(tableName = "word_timestamps")
data class WordTimestampEntity(
    @PrimaryKey
    val id: String,
    val segmentId: String,
    val wordIndex: Int,
    val word: String,
    val startTimeMs: Long,
    val endTimeMs: Long,
    val confidence: Float
)

@Dao
interface LyricsDao {
    @Query("SELECT * FROM lyrics_segments WHERE trackId = :trackId ORDER BY lineIndex ASC")
    fun getSegmentsForTrack(trackId: String): Flow<List<LyricsSegmentEntity>>

    @Query("SELECT * FROM word_timestamps WHERE segmentId = :segmentId ORDER BY wordIndex ASC")
    fun getWordsForSegment(segmentId: String): Flow<List<WordTimestampEntity>>

    @Query("SELECT * FROM word_timestamps WHERE segmentId IN (SELECT id FROM lyrics_segments WHERE trackId = :trackId) ORDER BY startTimeMs ASC")
    fun getAllWordsForTrack(trackId: String): Flow<List<WordTimestampEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSegments(segments: List<LyricsSegmentEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWords(words: List<WordTimestampEntity>)

    @Query("DELETE FROM lyrics_segments WHERE trackId = :trackId")
    suspend fun deleteSegmentsForTrack(trackId: String)

    @Query("DELETE FROM word_timestamps WHERE segmentId IN (SELECT id FROM lyrics_segments WHERE trackId = :trackId)")
    suspend fun deleteWordsForTrack(trackId: String)

    @Transaction
    suspend fun saveFullLyrics(trackId: String, segments: List<LyricsSegment>) {
        deleteWordsForTrack(trackId)
        deleteSegmentsForTrack(trackId)

        val segmentEntities = segments.map { seg ->
            LyricsSegmentEntity(
                id = seg.id,
                trackId = trackId,
                lineIndex = seg.lineIndex,
                startTimeMs = seg.startTimeMs,
                endTimeMs = seg.endTimeMs,
                text = seg.text,
                isRtl = seg.isRtl,
                confidence = seg.confidence
            )
        }
        insertSegments(segmentEntities)

        val wordEntities = mutableListOf<WordTimestampEntity>()
        segments.forEach { seg ->
            seg.words.forEachIndexed { idx, w ->
                wordEntities.add(
                    WordTimestampEntity(
                        id = "${seg.id}_w_$idx",
                        segmentId = seg.id,
                        wordIndex = idx,
                        word = w.word,
                        startTimeMs = w.startTimeMs,
                        endTimeMs = w.endTimeMs,
                        confidence = w.confidence
                    )
                )
            }
        }
        insertWords(wordEntities)
    }
}
