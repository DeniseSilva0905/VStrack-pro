package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "songs")
data class SongEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val title: String,
    val artist: String,
    val album: String = "",
    val genre: String = "Gospel / Worship",
    val musicalKey: String = "C",
    val bpm: Int = 120,
    val durationMs: Long = 0,
    val lyrics: String = "",
    val chords: String = "",
    val tags: String = "",
    val notes: String = "",
    val audioUri: String = "", // content:// or local path
    val audioFileName: String = "",
    val origin: String = "Local", // "Local", "GoogleDrive", "Demo"
    val dateAdded: Long = System.currentTimeMillis(),
    val fileSize: Long = 0,
    val isOfflineAvailable: Boolean = true,
    val isFavorite: Boolean = false,
    val isInTrash: Boolean = false,
    val scrollSpeed: Int = 3, // Custom per-song scroll speed (1 to 10)
    val lyricsRichJson: String = "" // Formatted PowerPoint lyrics (slides, fonts, colors, sizes)
)

@Entity(tableName = "setlists")
data class SetlistEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val name: String,
    val description: String = "",
    val dateCreated: Long = System.currentTimeMillis(),
    val dateUpdated: Long = System.currentTimeMillis()
)

@Entity(tableName = "setlist_items")
data class SetlistItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val setlistId: Long,
    val orderIndex: Int,
    val type: String, // "SONG", "BLOCK", "PAUSE"
    val songId: Long? = null,
    val customTitle: String = "", // e.g., "Abertura", "Intervalo - 15 min"
    val durationSeconds: Int = 0,
    val notes: String = "",
    val isInTrash: Boolean = false
)

@Entity(tableName = "show_history")
data class ShowHistoryEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val showName: String,
    val setlistId: Long? = null,
    val setlistName: String,
    val dateTimestamp: Long = System.currentTimeMillis(),
    val songsPlayedCount: Int = 0,
    val totalDurationSeconds: Long = 0,
    val notes: String = ""
)

@Entity(tableName = "user_settings")
data class UserSettingsEntity(
    @PrimaryKey
    val id: Int = 1,
    val userName: String = "Diretor Musical",
    val userEmail: String = "musico@vstrackspro.com",
    val stageRole: String = "Líder de Louvor / Teclado",
    val bandName: String = "Banda VSTracks Pro",
    val stagePin: String = "1234",
    val isStageLockEnabled: Boolean = false,
    val requirePlayConfirmation: Boolean = false,
    val isContinuousMode: Boolean = false, // false = 1x1 mode, true = continuous
    val intervalSeconds: Int = 3,
    val isStereoSplitEnabled: Boolean = true, // L: Click/Guia, R: VS Playback
    val isLRSwapped: Boolean = false, // L/R <-> R/L
    val leftVolume: Float = 0.9f,
    val rightVolume: Float = 1.0f,
    val isGoogleDriveConnected: Boolean = true,
    val googleDriveAccount: String = "drive.musico@gmail.com",
    val googleDriveFolder: String = "Meus VS / Playbacks Shows 2026",
    val isWakeLockEnabled: Boolean = true,
    val showArtistInLibrary: Boolean = true,
    val songTitleFontSize: Int = 1 // 0 = Pequena (14sp), 1 = Média (16sp), 2 = Grande (19sp)
)

enum class SongTitleFontSizeOption(val level: Int, val label: String, val sizeSp: Int) {
    SMALL(0, "Pequena", 14),
    MEDIUM(1, "Média", 16),
    LARGE(2, "Grande", 19);

    companion object {
        fun fromLevel(level: Int): SongTitleFontSizeOption =
            entries.firstOrNull { it.level == level } ?: MEDIUM
    }
}
