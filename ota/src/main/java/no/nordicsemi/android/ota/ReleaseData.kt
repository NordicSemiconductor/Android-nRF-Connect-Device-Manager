package no.nordicsemi.android.ota

/**
 * An OTA package, returned by nRF Cloud server.
 *
 * @property location The URL where the package can be downloaded.
 * @property releaseNotes Release notes for this package.
 * @property appVersion The version of the application in this package.
 * @property md5 The MD5 checksum of the package.
 * @property isForced Whether this update is forced.
 * @property size The size of the artifact at the [location].
 * @property isDelta Whether this package corresponds to a Delta or a Full Release.
 */
data class ReleaseData(
    val location: String,
    val releaseNotes: String,
    val appVersion: String,
    val md5: String,
    val isForced: Boolean?,
    val size: Long,
    val isDelta: Boolean,
)

/**
 * Information about the latest release for a device.
 */
sealed class ReleaseInformation {
    /**
     * An update is available.
     *
     * @property release The details about the available release.
     */
    data class UpdateAvailable(val release: ReleaseData) : ReleaseInformation()
    /**
     * The device is up to date.
     */
    object UpToDate : ReleaseInformation()
}

interface ReleaseCallback {
    fun onSuccess(releaseInformation: ReleaseInformation)
    fun onError(throwable: Throwable)
}

class ReleaseBinary(val name: String?, val bytes: ByteArray)

interface DownloadCallback {
    fun onSuccess(binary: ReleaseBinary)
    fun onNoContent()
    fun onError(throwable: Throwable)
}