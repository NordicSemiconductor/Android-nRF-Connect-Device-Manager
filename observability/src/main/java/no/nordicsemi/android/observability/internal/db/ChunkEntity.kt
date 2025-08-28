/*
 * Copyright (c) 2022, Nordic Semiconductor
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modification, are
 * permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this list of
 * conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice, this list
 * of conditions and the following disclaimer in the documentation and/or other materials
 * provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its contributors may be
 * used to endorse or promote products derived from this software without specific prior
 * written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED
 * TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A
 * PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT
 * HOLDER OR CONTRIBUTORS BE LIABLE FOR ANY DIRECT, INDIRECT, INCIDENTAL,
 * SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLUDING, BUT NOT
 * LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS OF USE, DATA,
 * OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON ANY THEORY
 * OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT (INCLUDING
 * NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS SOFTWARE,
 * EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package no.nordicsemi.android.observability.internal.db

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import no.nordicsemi.android.observability.data.Chunk

/**
 * Represents a chunk of data that is stored in the database.
 */
@Entity(tableName = "chunks")
internal data class ChunkEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    @ColumnInfo(name = "chunk_number")
    val chunkNumber: Int,
    @ColumnInfo(name = "data", typeAffinity = ColumnInfo.BLOB)
    val data: ByteArray,
    @ColumnInfo(name = "device_id")
    val deviceId: String,
    @ColumnInfo(name = "is_uploaded")
    val isUploaded: Boolean
)

/**
 * Converts the received byte array to a [ChunkEntity].
 *
 * @param deviceId The device ID associated with the chunk.
 * @return A [ChunkEntity] object that can be stored in the database.
 */
internal fun ByteArray.toEntity(deviceId: String) = ChunkEntity(
    chunkNumber = this[0].toInt(),
    data = this.copyOfRange(1, this.size),
    isUploaded = false,
    deviceId = deviceId,
)

/**
 * Converts a [ChunkEntity] to a [Chunk].
 *
 * @return A [Chunk] object that is exposed to the application.
 */
internal fun ChunkEntity.toChunk(): Chunk = Chunk(chunkNumber, data, deviceId, isUploaded)