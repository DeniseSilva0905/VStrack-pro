package com.example.data.repository

import android.content.Context
import android.media.MediaMetadataRetriever
import android.net.Uri
import com.example.data.local.AppDatabase
import com.example.data.model.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

class VSTracksRepository(
    private val context: Context,
    private val database: AppDatabase = AppDatabase.getDatabase(context)
) {
    private val songDao = database.songDao()
    private val setlistDao = database.setlistDao()
    private val historyDao = database.showHistoryDao()
    private val settingsDao = database.userSettingsDao()

    // Songs
    val allSongs: Flow<List<SongEntity>> = songDao.getAllSongs()
    val trashSongs: Flow<List<SongEntity>> = songDao.getTrashSongs()

    suspend fun getSongById(id: Long): SongEntity? = withContext(Dispatchers.IO) {
        songDao.getSongById(id)
    }

    suspend fun insertSong(song: SongEntity): Long = withContext(Dispatchers.IO) {
        songDao.insertSong(song)
    }

    suspend fun updateSong(song: SongEntity) = withContext(Dispatchers.IO) {
        songDao.updateSong(song)
    }

    suspend fun moveSongToTrash(id: Long) = withContext(Dispatchers.IO) {
        songDao.moveSongToTrash(id)
    }

    suspend fun restoreSong(id: Long) = withContext(Dispatchers.IO) {
        songDao.restoreSong(id)
    }

    suspend fun deleteSong(id: Long) = withContext(Dispatchers.IO) {
        songDao.deleteSongById(id)
    }

    suspend fun emptySongTrash() = withContext(Dispatchers.IO) {
        songDao.emptySongTrash()
    }

    suspend fun toggleFavorite(id: Long, isFav: Boolean) = withContext(Dispatchers.IO) {
        songDao.updateFavorite(id, isFav)
    }

    suspend fun updateSongScrollSpeed(songId: Long, speed: Int) = withContext(Dispatchers.IO) {
        songDao.updateScrollSpeed(songId, speed)
    }

    suspend fun updateSongLyrics(songId: Long, lyrics: String, lyricsRichJson: String) = withContext(Dispatchers.IO) {
        songDao.updateLyrics(songId, lyrics, lyricsRichJson)
    }

    // Setlists
    val allSetlists: Flow<List<SetlistEntity>> = setlistDao.getAllSetlists()

    suspend fun getSetlistById(id: Long): SetlistEntity? = withContext(Dispatchers.IO) {
        setlistDao.getSetlistById(id)
    }

    suspend fun createSetlist(name: String, description: String = ""): Long = withContext(Dispatchers.IO) {
        setlistDao.insertSetlist(
            SetlistEntity(
                name = name,
                description = description,
                dateCreated = System.currentTimeMillis(),
                dateUpdated = System.currentTimeMillis()
            )
        )
    }

    suspend fun updateSetlist(setlist: SetlistEntity) = withContext(Dispatchers.IO) {
        setlistDao.updateSetlist(setlist.copy(dateUpdated = System.currentTimeMillis()))
    }

    suspend fun duplicateSetlist(setlistId: Long): Long = withContext(Dispatchers.IO) {
        val original = setlistDao.getSetlistById(setlistId) ?: return@withContext -1L
        val newId = setlistDao.insertSetlist(
            SetlistEntity(
                name = "${original.name} (Cópia)",
                description = original.description,
                dateCreated = System.currentTimeMillis(),
                dateUpdated = System.currentTimeMillis()
            )
        )
        val items = setlistDao.getItemsForSetlistSync(setlistId)
        val newItems = items.map { it.copy(id = 0, setlistId = newId) }
        setlistDao.insertSetlistItems(newItems)
        newId
    }

    suspend fun deleteSetlist(id: Long) = withContext(Dispatchers.IO) {
        setlistDao.deleteItemsForSetlist(id)
        setlistDao.deleteSetlist(id)
    }

    fun getItemsForSetlist(setlistId: Long): Flow<List<SetlistItemEntity>> =
        setlistDao.getItemsForSetlist(setlistId)

    suspend fun getItemsForSetlistSync(setlistId: Long): List<SetlistItemEntity> = withContext(Dispatchers.IO) {
        setlistDao.getItemsForSetlistSync(setlistId)
    }

    suspend fun addSongToSetlist(setlistId: Long, song: SongEntity) = withContext(Dispatchers.IO) {
        val currentItems = setlistDao.getItemsForSetlistSync(setlistId)
        val nextOrder = (currentItems.maxOfOrNull { it.orderIndex } ?: -1) + 1
        setlistDao.insertSetlistItem(
            SetlistItemEntity(
                setlistId = setlistId,
                orderIndex = nextOrder,
                type = "SONG",
                songId = song.id,
                customTitle = song.title,
                durationSeconds = (song.durationMs / 1000).toInt(),
                notes = song.notes
            )
        )
    }

    suspend fun addSongsToSetlist(setlistId: Long, songsInOrder: List<SongEntity>) = withContext(Dispatchers.IO) {
        if (songsInOrder.isEmpty()) return@withContext
        val currentItems = setlistDao.getItemsForSetlistSync(setlistId)
        val startOrder = (currentItems.maxOfOrNull { it.orderIndex } ?: -1) + 1
        val newEntities = songsInOrder.mapIndexed { idx, song ->
            SetlistItemEntity(
                setlistId = setlistId,
                orderIndex = startOrder + idx,
                type = "SONG",
                songId = song.id,
                customTitle = song.title,
                durationSeconds = (song.durationMs / 1000).toInt(),
                notes = song.notes
            )
        }
        setlistDao.insertSetlistItems(newEntities)
    }

    suspend fun addBlockDivider(setlistId: Long, title: String) = withContext(Dispatchers.IO) {
        val currentItems = setlistDao.getItemsForSetlistSync(setlistId)
        val nextOrder = (currentItems.maxOfOrNull { it.orderIndex } ?: -1) + 1
        setlistDao.insertSetlistItem(
            SetlistItemEntity(
                setlistId = setlistId,
                orderIndex = nextOrder,
                type = "BLOCK",
                customTitle = title.ifBlank { "NOVO BLOCO" },
                durationSeconds = 0,
                notes = ""
            )
        )
    }

    suspend fun addPause(setlistId: Long, title: String, durationMinutes: Int, notes: String) = withContext(Dispatchers.IO) {
        val currentItems = setlistDao.getItemsForSetlistSync(setlistId)
        val nextOrder = (currentItems.maxOfOrNull { it.orderIndex } ?: -1) + 1
        setlistDao.insertSetlistItem(
            SetlistItemEntity(
                setlistId = setlistId,
                orderIndex = nextOrder,
                type = "PAUSE",
                customTitle = title.ifBlank { "PAUSA / INTERVALO" },
                durationSeconds = durationMinutes * 60,
                notes = notes
            )
        )
    }

    suspend fun moveItem(setlistId: Long, fromIndex: Int, toIndex: Int) = withContext(Dispatchers.IO) {
        val items = setlistDao.getItemsForSetlistSync(setlistId).toMutableList()
        if (fromIndex in items.indices && toIndex in items.indices) {
            val item = items.removeAt(fromIndex)
            items.add(toIndex, item)
            // Re-index
            val reindexed = items.mapIndexed { index, setlistItemEntity ->
                setlistItemEntity.copy(orderIndex = index)
            }
            setlistDao.insertSetlistItems(reindexed)
        }
    }

    val trashSetlistItems: Flow<List<SetlistItemEntity>> = setlistDao.getTrashSetlistItems()

    suspend fun moveItemToTrash(itemId: Long, setlistId: Long) = withContext(Dispatchers.IO) {
        setlistDao.moveItemToTrash(itemId)
        val items = setlistDao.getItemsForSetlistSync(setlistId)
        val reindexed = items.mapIndexed { index, item -> item.copy(orderIndex = index) }
        setlistDao.insertSetlistItems(reindexed)
    }

    suspend fun restoreItem(itemId: Long) = withContext(Dispatchers.IO) {
        setlistDao.restoreItem(itemId)
    }

    suspend fun deleteSetlistItem(itemId: Long, setlistId: Long) = withContext(Dispatchers.IO) {
        setlistDao.deleteSetlistItem(itemId)
        // Normalize indices
        val items = setlistDao.getItemsForSetlistSync(setlistId)
        val reindexed = items.mapIndexed { index, item -> item.copy(orderIndex = index) }
        setlistDao.insertSetlistItems(reindexed)
    }

    suspend fun deleteSetlistItemPermanently(itemId: Long) = withContext(Dispatchers.IO) {
        setlistDao.deleteSetlistItem(itemId)
    }

    suspend fun emptySetlistItemTrash() = withContext(Dispatchers.IO) {
        setlistDao.emptySetlistItemTrash()
    }

    // Show History
    val allHistory: Flow<List<ShowHistoryEntity>> = historyDao.getAllHistory()

    suspend fun recordShowHistory(history: ShowHistoryEntity): Long = withContext(Dispatchers.IO) {
        historyDao.insertHistory(history)
    }

    suspend fun deleteHistory(id: Long) = withContext(Dispatchers.IO) {
        historyDao.deleteHistory(id)
    }

    // Settings
    val settingsFlow: Flow<UserSettingsEntity?> = settingsDao.getSettingsFlow()

    suspend fun getSettings(): UserSettingsEntity = withContext(Dispatchers.IO) {
        settingsDao.getSettings() ?: UserSettingsEntity().also { settingsDao.saveSettings(it) }
    }

    suspend fun saveSettings(settings: UserSettingsEntity) = withContext(Dispatchers.IO) {
        settingsDao.saveSettings(settings)
    }

    // Audio File Import from SAF Uri with actual file name preservation
    suspend fun importAudioFromUri(uri: Uri, origin: String = "Local"): SongEntity = withContext(Dispatchers.IO) {
        val actualFileName = getActualFileName(uri)
        var title = actualFileName.substringBeforeLast('.').replace('_', ' ').replace('-', ' ').trim()
        var artist = "Artista Desconhecido"
        var durationMs = 0L
        var album = ""
        var genre = "Gospel / Worship"

        try {
            val retriever = MediaMetadataRetriever()
            retriever.setDataSource(context, uri)
            val metaTitle = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE)
            if (!metaTitle.isNullOrBlank()) {
                title = metaTitle.trim()
            }
            artist = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST)?.trim() ?: "Artista Desconhecido"
            album = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ALBUM)?.trim() ?: ""
            genre = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_GENRE)?.trim() ?: "Worship"
            durationMs = retriever.extractMetadata(MediaMetadataRetriever.METADATA_KEY_DURATION)?.toLongOrNull() ?: 180000L
            retriever.release()
        } catch (_: Exception) {
            // Keep derived file name title
        }

        val song = SongEntity(
            title = title.ifBlank { "Música VS" },
            artist = if (artist.isBlank() || artist == "Artista Desconhecido") "Artista Desconhecido" else artist,
            album = album,
            genre = genre,
            musicalKey = "C",
            bpm = 120,
            durationMs = durationMs,
            lyrics = "[Letra ainda não cadastrada para $title]",
            chords = "[Tom: C] [BPM: 120]\n| C | G | Am7 | F |",
            audioUri = uri.toString(),
            audioFileName = actualFileName,
            origin = origin,
            isOfflineAvailable = true
        )

        val newId = songDao.insertSong(song)
        song.copy(id = newId)
    }

    fun getActualFileName(uri: Uri): String {
        var name: String? = null
        if (uri.scheme == "content") {
            try {
                context.contentResolver.query(uri, arrayOf(android.provider.OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
                    if (cursor.moveToFirst()) {
                        val colIdx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                        if (colIdx >= 0) {
                            name = cursor.getString(colIdx)
                        }
                    }
                }
            } catch (_: Exception) {}
        }
        if (name.isNullOrBlank()) {
            name = uri.lastPathSegment?.substringAfterLast('/')
        }
        return name ?: "audio_vs.mp3"
    }

    fun normalizeAudioKey(name: String): String {
        return name.trim().lowercase()
            .substringBeforeLast('.')
            .replace(Regex("^(\\d{1,3}[_\\-\\.\\s]+)+"), "")
            .replace(Regex("[_\\-\\.\\(\\)\\[\\]]+"), " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    suspend fun cleanDuplicateSongs(): Int = withContext(Dispatchers.IO) {
        val songs = songDao.getAllSongsSync()
        if (songs.size <= 1) return@withContext 0

        val seenUris = mutableSetOf<String>()
        val seenFileNames = mutableSetOf<String>()
        val seenNormalizedTitles = mutableSetOf<String>()

        val duplicateIds = mutableListOf<Long>()

        // Prioritize keeping favorites, songs with non-empty fileName, and original oldest ID
        val sorted = songs.sortedWith(
            compareByDescending<SongEntity> { it.isFavorite }
                .thenByDescending { it.audioFileName.isNotBlank() }
                .thenBy { it.id }
        )

        for (song in sorted) {
            val normUri = song.audioUri.trim().lowercase()
            val normFile = normalizeAudioKey(song.audioFileName)
            val normTitle = normalizeAudioKey(song.title)

            val isUriDup = normUri.isNotBlank() && seenUris.contains(normUri)
            val isFileDup = normFile.isNotBlank() && seenFileNames.contains(normFile)
            val isTitleDup = normTitle.isNotBlank() && seenNormalizedTitles.contains(normTitle)

            if (isUriDup || isFileDup || isTitleDup) {
                duplicateIds.add(song.id)
            } else {
                if (normUri.isNotBlank()) seenUris.add(normUri)
                if (normFile.isNotBlank()) seenFileNames.add(normFile)
                if (normTitle.isNotBlank()) seenNormalizedTitles.add(normTitle)
            }
        }

        if (duplicateIds.isNotEmpty()) {
            songDao.deleteSongsByIds(duplicateIds)
        }

        duplicateIds.size
    }

    // JSON Setlist Export / Import
    suspend fun exportSetlistToJson(setlistId: Long): String = withContext(Dispatchers.IO) {
        val setlist = setlistDao.getSetlistById(setlistId) ?: return@withContext "{}"
        val items = setlistDao.getItemsForSetlistSync(setlistId)

        val root = JSONObject().apply {
            put("app", "VSTracks Pro")
            put("version", "1.0")
            put("exportDate", SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date()))
            put("setlistName", setlist.name)
            put("setlistDescription", setlist.description)

            val itemsArray = JSONArray()
            items.forEach { item ->
                val itemObj = JSONObject().apply {
                    put("type", item.type)
                    put("customTitle", item.customTitle)
                    put("orderIndex", item.orderIndex)
                    put("durationSeconds", item.durationSeconds)
                    put("notes", item.notes)
                }
                itemsArray.put(itemObj)
            }
            put("items", itemsArray)
        }
        root.toString(2)
    }

    suspend fun importSetlistFromJson(jsonString: String): Long = withContext(Dispatchers.IO) {
        val obj = JSONObject(jsonString)
        val name = obj.optString("setlistName", "Repertório Importado")
        val desc = obj.optString("setlistDescription", "Importado via JSON")

        val newId = setlistDao.insertSetlist(
            SetlistEntity(
                name = name,
                description = desc
            )
        )

        val itemsArray = obj.optJSONArray("items")
        if (itemsArray != null) {
            val list = mutableListOf<SetlistItemEntity>()
            for (i in 0 until itemsArray.length()) {
                val itemObj = itemsArray.getJSONObject(i)
                list.add(
                    SetlistItemEntity(
                        setlistId = newId,
                        orderIndex = itemObj.optInt("orderIndex", i),
                        type = itemObj.optString("type", "SONG"),
                        customTitle = itemObj.optString("customTitle", "Música"),
                        durationSeconds = itemObj.optInt("durationSeconds", 180),
                        notes = itemObj.optString("notes", "")
                    )
                )
            }
            setlistDao.insertSetlistItems(list)
        }
        newId
    }
}
