package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.sqlite.db.SupportSQLiteDatabase
import com.example.data.model.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        SongEntity::class,
        SetlistEntity::class,
        SetlistItemEntity::class,
        ShowHistoryEntity::class,
        UserSettingsEntity::class
    ],
    version = 7,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun songDao(): SongDao
    abstract fun setlistDao(): SetlistDao
    abstract fun showHistoryDao(): ShowHistoryDao
    abstract fun userSettingsDao(): UserSettingsDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        private val MIGRATION_6_7 = object : androidx.room.migration.Migration(6, 7) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE user_settings ADD COLUMN songTitleFontSize INTEGER NOT NULL DEFAULT 1")
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "vstracks_pro_db"
                )
                .addMigrations(MIGRATION_6_7)
                .fallbackToDestructiveMigration(true)
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        CoroutineScope(Dispatchers.IO).launch {
                            populateInitialData(getDatabase(context))
                        }
                    }
                })
                .build()
                INSTANCE = instance
                instance
            }
        }

        private suspend fun populateInitialData(db: AppDatabase) {
            // Seed settings
            db.userSettingsDao().saveSettings(UserSettingsEntity())

            // Seed initial repertoire songs
            val song1Id = db.songDao().insertSong(
                SongEntity(
                    title = "Ousado Amor (Reckless Love)",
                    artist = "Isaías Saad",
                    album = "Ousado Amor Ao Vivo",
                    genre = "Worship / Adoração",
                    musicalKey = "Gb",
                    bpm = 132,
                    durationMs = 285000,
                    lyrics = """
[Verso 1]
Antes de eu falar, Tu cantavas sobre mim
Tu tens sido tão, tão bom pra mim
Antes de eu respirar, sopraste Tua vida em mim
Tu tens sido tão, tão bondoso pra mim

[Refrão]
Oh, impressionante, infinito e ousado amor de Deus
Oh, que deixa as noventa e nove só pra me encontrar
Não posso comprá-lo, nem merecê-lo, mesmo assim Se entregou
Oh, impressionante, infinito e ousado amor de Deus

[Ponte]
Traz luz para as sombras
Escala montanhas
Pra me encontrar
Derruba muralhas
Destrói as mentiras
Pra me encontrar
                    """.trimIndent(),
                    chords = """
[Tom: Gb] [BPM: 132] [4/4]

[Intro]
| Ebm7 | Db4 | B9 | B9 |

[Verso 1]
      Ebm7            Db4            B9
Antes de eu falar, Tu cantavas sobre mim
             Ebm7     Db4         B9
Tu tens sido tão, tão bom pra mim
      Ebm7                Db4             B9
Antes de eu respirar, sopraste Tua vida em mim
             Ebm7     Db4             B9
Tu tens sido tão, tão bondoso pra mim

[Refrão]
    Ebm7            Db4                   B9       Gb
Oh, impressionante, infinito e ousado amor de Deus
       Ebm7               Db4               B9       Gb
Oh, que deixa as noventa e nove só pra me encontrar
           Ebm7          Db4                  B9        Gb
Não posso comprá-lo, nem merecê-lo, mesmo assim Se entregou
    Ebm7            Db4                   B9       Gb
Oh, impressionante, infinito e ousado amor de Deus
                    """.trimIndent(),
                    tags = "Abertura, Worship, Animada, In-Ear Click",
                    notes = "Guia no fone com contagem 1-2-3-4 na entrada do refrão. Split estéreo ativado.",
                    origin = "GoogleDrive",
                    fileSize = 8450000,
                    isOfflineAvailable = true,
                    scrollSpeed = 4,
                    lyricsRichJson = """[{"slideNumber":1,"title":"Slide 1","paragraphs":[{"alignment":"center","runs":[{"text":"ANTES DE EU FALAR,\nTU CANTAVAS SOBRE MIM","fontSizeSp":28.0,"colorHex":"#FFD700","fontFamily":"Montserrat","isBold":true,"isItalic":false}]},{"alignment":"center","runs":[{"text":"Tu tens sido tão, tão bom pra mim","fontSizeSp":22.0,"colorHex":"#FFFFFF","fontFamily":"Arial","isBold":false,"isItalic":false}]}]},{"slideNumber":2,"title":"Slide 2","paragraphs":[{"alignment":"center","runs":[{"text":"ANTES DE EU RESPIRAR,\nSOPRASTE TUA VIDA EM MIM","fontSizeSp":28.0,"colorHex":"#4ECDC4","fontFamily":"Montserrat","isBold":true,"isItalic":false}]},{"alignment":"center","runs":[{"text":"Tu tens sido tão, tão bondoso pra mim","fontSizeSp":22.0,"colorHex":"#FFFFFF","fontFamily":"Arial","isBold":false,"isItalic":false}]}]},{"slideNumber":3,"title":"Slide 3","paragraphs":[{"alignment":"center","runs":[{"text":"OH, IMPRESSIONANTE, INFINITO\nE OUSADO AMOR DE DEUS","fontSizeSp":32.0,"colorHex":"#FFD700","fontFamily":"Impact","isBold":true,"isItalic":false}]},{"alignment":"center","runs":[{"text":"Oh, que deixa as noventa e nove só pra me encontrar","fontSizeSp":24.0,"colorHex":"#FFEEAD","fontFamily":"Arial","isBold":true,"isItalic":true}]}]}]"""
                )
            )

            val song2Id = db.songDao().insertSong(
                SongEntity(
                    title = "A Bênção (The Blessing)",
                    artist = "Gabriel Guedes & Julia Vitória",
                    album = "A Bênção Live",
                    genre = "Worship / Adoração",
                    musicalKey = "B",
                    bpm = 70,
                    durationMs = 320000,
                    scrollSpeed = 2,
                    lyricsRichJson = """[{"slideNumber":1,"title":"Slide 1","paragraphs":[{"alignment":"center","runs":[{"text":"O SENHOR TE ABENÇOE E TE GUARDE","fontSizeSp":30.0,"colorHex":"#45B7D1","fontFamily":"Georgia","isBold":true,"isItalic":false}]},{"alignment":"center","runs":[{"text":"Que resplandeça Seu rosto sobre ti\nE tenha misericórdia de ti","fontSizeSp":24.0,"colorHex":"#FFFFFF","fontFamily":"Arial","isBold":false,"isItalic":false}]}]},{"slideNumber":2,"title":"Slide 2","paragraphs":[{"alignment":"center","runs":[{"text":"AMÉM, AMÉM, AMÉM\nAMÉM, AMÉM, AMÉM","fontSizeSp":36.0,"colorHex":"#FFD700","fontFamily":"Montserrat","isBold":true,"isItalic":false}]}]}]""",
                    lyrics = """
[Verso]
O Senhor te abençoe e te guarde
Que resplandeça Seu rosto sobre ti
E tenha misericórdia de ti
O Senhor sobre ti levante Seu rosto
E te dê a paz

[Refrão]
Amém, amém, amém
Amém, amém, amém
                    """.trimIndent(),
                    chords = """
[Tom: B] [BPM: 70] [4/4]

[Intro]
| B | Bsus | B | Bsus |

[Verso]
  B                 E2
O Senhor te abençoe e te guarde
    B/D#                F#sus  F#
Que resplandeça Seu rosto sobre ti
  G#m7             E2
E tenha misericórdia de ti
  B                     F#sus
O Senhor sobre ti levante Seu rosto
     B
E te dê a paz
                    """.trimIndent(),
                    tags = "Ministração, Lenta, Clima, Pad Celestial",
                    notes = "Entrar com Pad contínuo em B no final para transição suave.",
                    origin = "Local",
                    fileSize = 7800000,
                    isOfflineAvailable = true
                )
            )

            val song3Id = db.songDao().insertSong(
                SongEntity(
                    title = "Ruja o Leão / Que Se Abram os Céus",
                    artist = "Talita Catanzaro",
                    album = "Ao Vivo Palco 2026",
                    genre = "Louvor Celebrativo",
                    musicalKey = "D",
                    bpm = 126,
                    durationMs = 290000,
                    lyrics = """
[Refrão]
Ruja o Leão de Judá
Que o som da Tua glória venha encher este lugar!
                    """.trimIndent(),
                    chords = """
[Tom: D] [BPM: 126] [4/4]

[Intro]
| Bm7 | G | D | A |
                    """.trimIndent(),
                    tags = "Celebração, Bateria Forte, Metais, Stems",
                    notes = "Pista multipista com stems separadas disponíveis no Mixer.",
                    origin = "Local",
                    fileSize = 9200000,
                    isOfflineAvailable = true
                )
            )

            val song4Id = db.songDao().insertSong(
                SongEntity(
                    title = "Me Atraiu",
                    artist = "Gabriela Rocha",
                    album = "Presença",
                    genre = "Worship",
                    musicalKey = "E",
                    bpm = 68,
                    durationMs = 345000,
                    lyrics = "[Verso]\nEu poderia estar em qualquer outro lugar\nMas Tua presença me atraiu\n[Refrão]\nMe atraiu, me atraiu...",
                    chords = "[Tom: E] [BPM: 68]\n| E | B/D# | C#m7 | A9 |",
                    tags = "Clima, Intimidade, Fechamento",
                    notes = "Segurar Pad Shimmer em E durante a oração.",
                    origin = "GoogleDrive",
                    fileSize = 8900000,
                    isOfflineAvailable = true
                )
            )

            // Seed main setlist
            val setlistId = db.setlistDao().insertSetlist(
                SetlistEntity(
                    name = "Show Principal - Turnê Palco 2026",
                    description = "Repertório completo para apresentações ao vivo com transições, blocos e pausas."
                )
            )

            // Seed items for this setlist
            db.setlistDao().insertSetlistItems(
                listOf(
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 0,
                        type = "BLOCK",
                        customTitle = "BLOCO 1: ABERTURA CELEBRATIVA",
                        notes = "Subir cortina com iluminação roxa e disparo do VS 1"
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 1,
                        type = "SONG",
                        songId = song3Id,
                        customTitle = "Ruja o Leão / Que Se Abram os Céus",
                        durationSeconds = 290,
                        notes = "Click e Guia ativos no fone L. Bateria forte no PA R."
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 2,
                        type = "SONG",
                        songId = song1Id,
                        customTitle = "Ousado Amor (Reckless Love)",
                        durationSeconds = 285,
                        notes = "Transição contínua em 3 segundos."
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 3,
                        type = "PAUSE",
                        customTitle = "MOMENTO DE FALA & BOAS VINDAS",
                        durationSeconds = 180,
                        notes = "Manter Pad Warm em Gb no fundo durante a fala pastoral/ministração."
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 4,
                        type = "BLOCK",
                        customTitle = "BLOCO 2: ADORAÇÃO & MINISTRAÇÃO",
                        notes = "Luz baixa e foco no vocal central"
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 5,
                        type = "SONG",
                        songId = song2Id,
                        customTitle = "A Bênção (The Blessing)",
                        durationSeconds = 320,
                        notes = "Modo 1x1: pausar ao término da faixa."
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 6,
                        type = "SONG",
                        songId = song4Id,
                        customTitle = "Me Atraiu",
                        durationSeconds = 345,
                        notes = "Encerramento com fade-out gradual e oração final."
                    ),
                    SetlistItemEntity(
                        setlistId = setlistId,
                        orderIndex = 7,
                        type = "PAUSE",
                        customTitle = "INTERVALO / TROCA DE FIGURINO - 10 MIN",
                        durationSeconds = 600,
                        notes = "Som ambiente no PA. Músicos em standby."
                    )
                )
            )

            // Seed historical performance
            db.showHistoryDao().insertHistory(
                ShowHistoryEntity(
                    showName = "Festival Praise Arena - São Paulo",
                    setlistId = setlistId,
                    setlistName = "Show Principal - Turnê Palco 2026",
                    dateTimestamp = System.currentTimeMillis() - 86400000L * 2,
                    songsPlayedCount = 4,
                    totalDurationSeconds = 1420,
                    notes = "Show executado com sucesso. Todos os playbacks e pedais responderam perfeitamente sem falhas."
                )
            )
        }
    }
}
