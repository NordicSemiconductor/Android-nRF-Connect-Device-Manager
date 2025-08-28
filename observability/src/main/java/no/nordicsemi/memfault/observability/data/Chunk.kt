package no.nordicsemi.memfault.observability.data

/**
 * Represents a chunk of Observability data received from the device
 * using Monitoring & Diagnostics Service.
 *
 * @property chunkNumber The number of the chunk, in range from 0-31
 * @property data The bytes received.
 * @property deviceId The device ID of the device from which the chunk was received.
 * @property isUploaded A flag indicating whether the chunk has been uploaded to the nRF Cloud.
 */
data class Chunk(
    val chunkNumber: Int,
    val data: ByteArray,
    val deviceId: String,
    var isUploaded: Boolean
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as Chunk

        if (chunkNumber != other.chunkNumber) return false
        if (isUploaded != other.isUploaded) return false
        if (!data.contentEquals(other.data)) return false
        if (deviceId != other.deviceId) return false

        return true
    }

    override fun hashCode(): Int {
        var result = chunkNumber
        result = 31 * result + isUploaded.hashCode()
        result = 31 * result + data.contentHashCode()
        result = 31 * result + deviceId.hashCode()
        return result
    }
}