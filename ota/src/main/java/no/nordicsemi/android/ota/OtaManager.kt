package no.nordicsemi.android.ota

import com.memfault.cloud.sdk.GetLatestReleaseCallback
import com.memfault.cloud.sdk.MemfaultCloud
import com.memfault.cloud.sdk.MemfaultOtaPackage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import no.nordicsemi.android.mcumgr.McuMgrCallback
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.exception.McuMgrException
import no.nordicsemi.android.ota.mcumgr.MemfaultDeviceInfoResponse
import no.nordicsemi.android.ota.mcumgr.MemfaultManager
import no.nordicsemi.android.ota.mcumgr.MemfaultProjectKeyResponse
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * The nRF Cloud OTA manager can be used to get the latest firmware releases for supported devices
 * from nRF Cloud Services.
 */
class OtaManager {

    /**
     * Gets the information about the latest release for the given device from nRF Cloud Services
     * by reading required data using the given transport.
     *
     * This method is using Memfault group (id = 128) to get the:
     * - device Serial Number
     * - device Hardware Version
     * - device Software Type
     * - current firmware version
     * - the project key
     *
     * and then requesting the release information form nRF Cloud.
     * @param transport The transport to use to send commands.
     * @throws McuMgrException if reading the device information fails.
     */
    suspend fun getLatestRelease(
        transport: McuMgrTransport,
    ): ReleaseInformation {
        val memfaultManager = MemfaultManager(transport)

        val deviceInfo = suspendCancellableCoroutine { continuation ->
            memfaultManager.info(object : McuMgrCallback<MemfaultDeviceInfoResponse> {
                override fun onResponse(response: MemfaultDeviceInfoResponse) {
                    continuation.resume(response.toDeviceInfo())
                }

                override fun onError(error: McuMgrException) {
                    continuation.resumeWithException(error)
                }
            })
        }
        val projectKey = suspendCancellableCoroutine { continuation ->
            memfaultManager.projectKey(object : McuMgrCallback<MemfaultProjectKeyResponse> {
                override fun onResponse(response: MemfaultProjectKeyResponse) {
                    continuation.resume(response.projectKey)
                }

                override fun onError(error: McuMgrException) {
                    continuation.resumeWithException(error)
                }
            })
        }
        return getLatestRelease(deviceInfo, projectKey)
    }

    /**
     * Gets the information about the latest release for the given device from nRF Cloud Services
     * by reading required data using the given transport.
     *
     * This method is using Memfault group (id = 128) to get the:
     * - device Serial Number
     * - device Hardware Version
     * - device Software Type
     * - current firmware version
     * - the project key
     *
     * and then requesting the release information form nRF Cloud.
     * @param transport The transport to use to send commands.
     * @param callback A callback with the [ReleaseInformation] object containing details about the
     * latest release or an error.
     */
    fun getLatestRelease(
        transport: McuMgrTransport,
        callback: ReleaseCallback,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = getLatestRelease(transport)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    /**
     * Gets the latest release for the given device from nRF Cloud Services.
     *
     * @param deviceInfo Information about the device, such as hardware version, software type and
     * current firmware version.
     * @param projectKey The project API key from nRF Cloud. You can find it in the project settings
     * on the nRF Cloud web portal.
     * @return A [ReleaseInformation] object containing details about the latest release or an error.
     */
    suspend fun getLatestRelease(
        deviceInfo: DeviceInfo,
        projectKey: String,
    ): ReleaseInformation = suspendCancellableCoroutine { continuation ->
        val memfaultCloud = MemfaultCloud.Builder()
            .setApiKey(projectKey)
            .build()

        memfaultCloud.getLatestRelease(deviceInfo.map(), object : GetLatestReleaseCallback {
            override fun onUpToDate() {
                continuation.resume(ReleaseInformation.UpToDate)
            }

            override fun onUpdateAvailable(otaPackage: MemfaultOtaPackage) {
                val releaseData = ReleaseData(
                    location = otaPackage.location,
                    releaseNotes = otaPackage.releaseNotes,
                    appVersion = otaPackage.appVersion,
                    md5 = otaPackage.md5,
                    isForced = otaPackage.isForced,
                    size = otaPackage.size,
                    isDelta = otaPackage.isDelta,
                )
                continuation.resume(ReleaseInformation.UpdateAvailable(releaseData))
            }

            override fun onError(e: Exception) {
                continuation.resumeWithException(e)
            }
        })
    }

    /**
     * Gets the latest release for the given device from nRF Cloud Services.
     *
     * @param deviceInfo Information about the device, such as hardware version, software type and
     * current firmware version.
     * @param projectKey The project API key from nRF Cloud. You can find it in the project settings
     * on the nRF Cloud web portal.
     * @param callback A callback with the [ReleaseInformation] object containing details about the
     * latest release or an error.
     */
    fun getLatestRelease(
        deviceInfo: DeviceInfo,
        projectKey: String,
        callback: ReleaseCallback,
    ) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val result = getLatestRelease(deviceInfo, projectKey)
                withContext(Dispatchers.Main) {
                    callback.onSuccess(result)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }

    /**
     * Downloads the release binary from the given location.
     *
     * @param location The URL of the release binary. It can be obtained using [getLatestRelease]
     * from [ReleaseInformation.UpdateAvailable].
     * @return A [ReleaseBinary] object containing the name of the downloaded file and its content,
     * or null if the received data has no content.
     */
    suspend fun download(location: String): ReleaseBinary? = withContext(Dispatchers.IO) {
        val url = URL(location)
        var connection: HttpURLConnection? = null
        var inputStream: InputStream? = null
        val byteArrayOutputStream = ByteArrayOutputStream()

        try {
            connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            // Set timeouts (optional, but good practice)
            connection.connectTimeout = 15000 // 15 seconds
            connection.readTimeout = 60000 // 60 seconds

            // Start the connection
            connection.connect()

            // Check for successful response
            val responseCode = connection.responseCode
            when (responseCode) {
                HttpURLConnection.HTTP_OK -> {
                    // Read the input stream and write to ByteArrayOutputStream.
                    inputStream = connection.inputStream
                    val buffer = ByteArray(4096) // 4KB buffer
                    var bytesRead: Int
                    while (inputStream.read(buffer).also { bytesRead = it } != -1) {
                        byteArrayOutputStream.write(buffer, 0, bytesRead)
                    }
                    val content = byteArrayOutputStream.toByteArray()

                    // Read file name from Content-Disposition header if available.
                    val contentDisposition = connection.getHeaderField("Content-Disposition")
                    val fileName = contentDisposition?.let {
                        val fileNameKey = "filename="
                        val startIndex = it.indexOf(fileNameKey)
                        if (startIndex != -1) {
                            it.substring(startIndex + fileNameKey.length).trim('"')
                        } else {
                            null
                        }
                    }

                    return@withContext ReleaseBinary(fileName, content)
                }

                HttpURLConnection.HTTP_NO_CONTENT -> {
                    return@withContext null
                }

                else -> {
                    // Handle HTTP error codes
                    throw Exception("Download failed: HTTP error code $responseCode - ${connection.responseMessage}")
                }
            }
        } finally {
            // Ensure resources are closed
            inputStream?.close()
            byteArrayOutputStream.close()
            connection?.disconnect()
        }
    }

    fun download(location: String, callback: DownloadCallback) {
        CoroutineScope(Dispatchers.IO).launch {
            try {
                val binary = download(location)
                withContext(Dispatchers.Main) {
                    if (binary != null) {
                        callback.onSuccess(binary)
                    } else {
                        callback.onNoContent()
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    callback.onError(e)
                }
            }
        }
    }
}