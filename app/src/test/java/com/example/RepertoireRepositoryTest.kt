package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.SongEntity
import com.example.data.repository.VSTracksRepository
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class RepertoireRepositoryTest {

    private lateinit var db: AppDatabase
    private lateinit var repository: VSTracksRepository

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repository = VSTracksRepository(context, db)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `addSongsToSetlist preserves exact selection order`() = runBlocking {
        val idA = repository.insertSong(SongEntity(title = "Alpha", artist = "Artista 1"))
        val idB = repository.insertSong(SongEntity(title = "Beta", artist = "Artista 2"))
        val idC = repository.insertSong(SongEntity(title = "Gamma", artist = "Artista 3"))

        val songA = repository.getSongById(idA)!!
        val songB = repository.getSongById(idB)!!
        val songC = repository.getSongById(idC)!!

        val setlistId = repository.createSetlist("Show Ordem")
        // User selected Gamma, then Alpha, then Beta
        repository.addSongsToSetlist(setlistId, listOf(songC, songA, songB))

        val items = repository.getItemsForSetlistSync(setlistId)
        assertEquals(listOf("Gamma", "Alpha", "Beta"), items.map { it.customTitle })
        assertEquals(listOf(0, 1, 2), items.map { it.orderIndex })
    }

    @Test
    fun `blocks and intervals stay isolated per repertoire`() = runBlocking {
        val setlist1 = repository.createSetlist("Repertório 1")
        val setlist2 = repository.createSetlist("Repertório 2")

        repository.addBlockDivider(setlist1, "BLOCO 1: ABERTURA")
        repository.addPause(setlist1, "Intervalo", 0, "")

        // Repertoire 2 starts clean and gets its own block and interval
        assertEquals(0, repository.getItemsForSetlistSync(setlist2).size)

        repository.addBlockDivider(setlist2, "BLOCO 1: SHOW NOVO")
        repository.addPause(setlist2, "Intervalo", 0, "")

        val items1 = repository.getItemsForSetlistSync(setlist1)
        val items2 = repository.getItemsForSetlistSync(setlist2)

        assertEquals(2, items1.size)
        assertEquals(listOf("BLOCO 1: ABERTURA", "Intervalo"), items1.map { it.customTitle })

        assertEquals(2, items2.size)
        assertEquals(listOf("BLOCO 1: SHOW NOVO", "Intervalo"), items2.map { it.customTitle })
    }
}
