package com.example.data.local

import androidx.room.*
import com.example.data.model.*
import kotlinx.coroutines.flow.Flow

@Dao
interface SongDao {
    @Query("SELECT * FROM songs WHERE isInTrash = 0 ORDER BY dateAdded DESC")
    fun getAllSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE isInTrash = 1 ORDER BY dateAdded DESC")
    fun getTrashSongs(): Flow<List<SongEntity>>

    @Query("SELECT * FROM songs WHERE id = :id")
    suspend fun getSongById(id: Long): SongEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSong(song: SongEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSongs(songs: List<SongEntity>)

    @Update
    suspend fun updateSong(song: SongEntity)

    @Query("UPDATE songs SET isInTrash = 1 WHERE id = :id")
    suspend fun moveSongToTrash(id: Long)

    @Query("UPDATE songs SET isInTrash = 0 WHERE id = :id")
    suspend fun restoreSong(id: Long)

    @Query("DELETE FROM songs WHERE id = :id")
    suspend fun deleteSongById(id: Long)

    @Query("DELETE FROM songs WHERE isInTrash = 1")
    suspend fun emptySongTrash()

    @Query("UPDATE songs SET isFavorite = :isFav WHERE id = :id")
    suspend fun updateFavorite(id: Long, isFav: Boolean)

    @Query("SELECT COUNT(*) FROM songs WHERE isInTrash = 0")
    suspend fun getSongCount(): Int

    @Query("SELECT * FROM songs WHERE isInTrash = 0")
    suspend fun getAllSongsSync(): List<SongEntity>

    @Query("DELETE FROM songs WHERE id IN (:ids)")
    suspend fun deleteSongsByIds(ids: List<Long>)

    @Query("UPDATE songs SET scrollSpeed = :speed WHERE id = :id")
    suspend fun updateScrollSpeed(id: Long, speed: Int)

    @Query("UPDATE songs SET lyrics = :lyrics, lyricsRichJson = :richJson WHERE id = :id")
    suspend fun updateLyrics(id: Long, lyrics: String, richJson: String)
}

@Dao
interface SetlistDao {
    @Query("SELECT * FROM setlists ORDER BY dateUpdated DESC")
    fun getAllSetlists(): Flow<List<SetlistEntity>>

    @Query("SELECT * FROM setlists WHERE id = :id")
    suspend fun getSetlistById(id: Long): SetlistEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlist(setlist: SetlistEntity): Long

    @Update
    suspend fun updateSetlist(setlist: SetlistEntity)

    @Query("DELETE FROM setlists WHERE id = :id")
    suspend fun deleteSetlist(id: Long)

    @Query("DELETE FROM setlist_items WHERE setlistId = :setlistId")
    suspend fun deleteItemsForSetlist(setlistId: Long)

    @Query("SELECT * FROM setlist_items WHERE setlistId = :setlistId AND isInTrash = 0 ORDER BY orderIndex ASC")
    fun getItemsForSetlist(setlistId: Long): Flow<List<SetlistItemEntity>>

    @Query("SELECT * FROM setlist_items WHERE setlistId = :setlistId AND isInTrash = 0 ORDER BY orderIndex ASC")
    suspend fun getItemsForSetlistSync(setlistId: Long): List<SetlistItemEntity>

    @Query("SELECT * FROM setlist_items WHERE isInTrash = 1 ORDER BY id DESC")
    fun getTrashSetlistItems(): Flow<List<SetlistItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlistItems(items: List<SetlistItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSetlistItem(item: SetlistItemEntity): Long

    @Query("UPDATE setlist_items SET isInTrash = 1 WHERE id = :itemId")
    suspend fun moveItemToTrash(itemId: Long)

    @Query("UPDATE setlist_items SET isInTrash = 0 WHERE id = :itemId")
    suspend fun restoreItem(itemId: Long)

    @Query("DELETE FROM setlist_items WHERE id = :itemId")
    suspend fun deleteSetlistItem(itemId: Long)

    @Query("DELETE FROM setlist_items WHERE isInTrash = 1")
    suspend fun emptySetlistItemTrash()
}

@Dao
interface ShowHistoryDao {
    @Query("SELECT * FROM show_history ORDER BY dateTimestamp DESC")
    fun getAllHistory(): Flow<List<ShowHistoryEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: ShowHistoryEntity): Long

    @Query("DELETE FROM show_history WHERE id = :id")
    suspend fun deleteHistory(id: Long)

    @Query("DELETE FROM show_history")
    suspend fun clearHistory()
}

@Dao
interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = 1")
    fun getSettingsFlow(): Flow<UserSettingsEntity?>

    @Query("SELECT * FROM user_settings WHERE id = 1")
    suspend fun getSettings(): UserSettingsEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSettings(settings: UserSettingsEntity)
}
