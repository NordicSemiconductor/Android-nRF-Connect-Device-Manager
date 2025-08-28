package no.nordicsemi.memfault.observability.data

import android.content.Context
import androidx.room.Room
import com.memfault.cloud.sdk.ChunkQueue
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import no.nordicsemi.memfault.observability.internal.db.ChunksDatabase
import no.nordicsemi.memfault.observability.internal.db.toChunk
import no.nordicsemi.memfault.observability.internal.db.toEntity

/**
 * An implementation of [ChunkQueue] that stores chunks in a persistent database.
 *
 * @param context The application context required to create a database.
 * @param deviceId The unique identifier for the device..
 */
class PersistentChunkQueue(
    context: Context,
    private val deviceId: String,
) : ChunkQueue {
    companion object {
        private const val DB_NAME = "chunks-database"
    }

    /** The database instance used to store chunks. */
    private val database = Room
        .databaseBuilder(
            context = context.applicationContext,
            klass = ChunksDatabase::class.java,
            name = DB_NAME
        )
        .build()
    /** The DAO used to access the chunks in the database. */
    private val chunkDao = database.chunksDao()

    /** A flow emitting all chunks stored in the database for the given device ID. */
    val chunks: Flow<List<Chunk>> = chunkDao.getAll(deviceId)
        .map { list -> list.map { entity -> entity.toChunk() } }

    override fun addChunks(chunks: List<ByteArray>): Boolean = chunks
        .filter { it.size > 1 }
        .forEach { chunkDao.insert(it.toEntity(deviceId)) }
        // Always return true, as the chunks are added asynchronously.
        .let { true }

    override fun peek(count: Int): List<ByteArray> = chunkDao
        .getNotUploaded(count, deviceId)
        .map { it.data }

    override fun drop(count: Int) = chunkDao
        .markUploaded(count, deviceId)

    /**
     * Deletes all chunks that have been marked as uploaded.
     */
    fun deleteUploaded() = chunkDao.clearUploaded()
}