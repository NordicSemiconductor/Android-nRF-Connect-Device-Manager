package com.juul.mcumgr

import com.juul.mcumgr.command.CommandResult
import com.juul.mcumgr.command.Image
import com.juul.mcumgr.command.mapResponse
import com.juul.mcumgr.command.toUnitResult
import com.juul.mcumgr.transfer.CoreDownloader
import com.juul.mcumgr.transfer.ImageUploader

class ImageManager internal constructor(val transport: Transport) {

    suspend fun list(): CommandResult<ImageStateResponse> {
        val request = Image.ReadStateRequest
        return transport.send(
            request,
            Image.StateResponse::class
        ).mapResponse { response ->
            response.toImageStateResponse()
        }
    }

    suspend fun test(hash: ByteArray): CommandResult<ImageStateResponse> {
        val request = Image.WriteStateRequest(hash, false)
        return transport.send(
            request,
            Image.StateResponse::class
        ).mapResponse { response ->
            response.toImageStateResponse()
        }
    }

    suspend fun confirm(hash: ByteArray? = null): CommandResult<ImageStateResponse> {
        val request = Image.WriteStateRequest(hash, true)
        return transport.send(
            request,
            Image.StateResponse::class
        ).mapResponse { response ->
            response.toImageStateResponse()
        }
    }

    suspend fun erase(): CommandResult<Unit> {
        val request = Image.EraseRequest
        return transport.send(
            request,
            Image.EraseResponse::class
        ).toUnitResult()
    }

    suspend fun eraseState(): CommandResult<Unit> {
        val request = Image.EraseStateRequest
        return transport.send(
            request,
            Image.EraseStateResponse::class
        ).toUnitResult()
    }

    suspend fun imageWrite(
        data: ByteArray,
        offset: Int,
        length: Int? = null,
        hash: ByteArray? = null
    ): CommandResult<ImageWriteResponse> {
        val request = Image.ImageWriteRequest(data, offset, length, hash)
        return transport.send(
            request,
            Image.ImageWriteResponse::class
        ).mapResponse { response ->
            ImageWriteResponse(response.offset)
        }
    }

    suspend fun coreList(): CommandResult<Unit> {
        val request = Image.CoreListRequest
        return transport.send(
            request,
            Image.CoreListResponse::class
        ).toUnitResult()
    }

    suspend fun coreRead(offset: Int): CommandResult<CoreReadResponse> {
        val request = Image.CoreReadRequest(offset)
        return transport.send(
            request,
            Image.CoreReadResponse::class
        ).mapResponse { response ->
            CoreReadResponse(response.data, response.offset, response.length)
        }
    }

    suspend fun coreErase(): CommandResult<Unit> {
        val request = Image.CoreEraseRequest
        return transport.send(
            request,
            Image.CoreEraseResponse::class
        ).toUnitResult()
    }

    fun imageUploader(
        data: ByteArray,
        windowCapacity: Int = 1
    ) = ImageUploader(data, transport, windowCapacity)

    fun coreDownloader(
        windowCapacity: Int = 1
    ) = CoreDownloader(transport, windowCapacity)
}

data class ImageWriteResponse(
    val offset: Int
)

data class CoreReadResponse(
    val data: ByteArray,
    val offset: Int,
    val length: Int? = null
)

data class ImageStateResponse(
    val images: List<ImageState>
) {

    // Note: this implementation does not support multi-image boot.

    /**
     * The primary image slot always runs the active image.
     */
    val primary: ImageState = images[0]

    /**
     * Secondary image slot to upload a new image to.
     */
    val secondary: ImageState? = images.getOrNull(1)

    data class ImageState(
        /** The slot index, 0 (primary) or 1 (secondary). */
        val slot: Int,
        /** The image version string. */
        val version: String,
        /** The sha256 hash of the image. Also known as the build ID. */
        val hash: ByteArray,
        /** Used for split image apps. */
        val bootable: Boolean,
        /**
         * True if the image is pending. A pending image will be swapped from
         * the secondary slot to the primary slot on device reset. Only images
         * in the secondary slot can be pending.
         */
        val pending: Boolean,
        /**
         * True if the image is confirmed as the primary boot image. A confirmed
         * image will be booted as long as the secondary image is not pending.
         */
        val confirmed: Boolean,
        /** True if the image is active. The primary image is always active. */
        val active: Boolean,
        /** An image is permanent after being confirmed. */
        val permanent: Boolean
    )
}

private fun Image.StateResponse.toImageStateResponse(): ImageStateResponse {
    val images = images.map { imageState ->
        ImageStateResponse.ImageState(
            imageState.slot,
            imageState.version,
            imageState.hash,
            imageState.bootable,
            imageState.pending,
            imageState.confirmed,
            imageState.active,
            imageState.permanent
        )
    }
    return ImageStateResponse(images)
}
