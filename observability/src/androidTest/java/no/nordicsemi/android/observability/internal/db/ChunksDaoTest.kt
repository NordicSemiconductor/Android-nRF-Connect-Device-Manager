package no.nordicsemi.android.observability.internal.db

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ChunksDaoTest {

    private lateinit var db: ChunksDatabase
    private lateinit var dao: ChunksDao

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        db = Room.inMemoryDatabaseBuilder(context, ChunksDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        dao = db.chunksDao()
    }

    @After
    fun tearDown() {
        db.close()
    }

    @Test
    fun markUploaded_onlyMarksRequestedBatchForRequestedDevice() {
        val deviceA = "deviceA"
        val deviceB = "deviceB"

        repeat(150) { i ->
            dao.insert(ChunkEntity(chunkNumber = i, data = byteArrayOf(1), deviceId = deviceA, isUploaded = false))
        }
        repeat(5) { i ->
            dao.insert(ChunkEntity(chunkNumber = i, data = byteArrayOf(1), deviceId = deviceB, isUploaded = false))
        }

        // Simulate ChunkSender having just sent and confirmed a 100-chunk batch for device A.
        dao.markUploaded(100, deviceA)

        val remainingA = dao.getNotUploaded(1000, deviceA)
        val remainingB = dao.getNotUploaded(1000, deviceB)

        // Only the 100 oldest chunks for device A should be marked uploaded.
        assertEquals(50, remainingA.size)
        // Device B's queue must be untouched.
        assertEquals(5, remainingB.size)
    }
}
