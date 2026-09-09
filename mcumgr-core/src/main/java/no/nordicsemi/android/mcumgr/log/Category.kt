package no.nordicsemi.android.mcumgr.log

import no.nordicsemi.android.mcumgr.McuManager
import no.nordicsemi.android.mcumgr.McuMgrTransport
import no.nordicsemi.android.mcumgr.dfu.mcuboot.FirmwareUpgradeManager
import no.nordicsemi.android.mcumgr.dfu.suit.SUITUpgradeManager
import no.nordicsemi.android.mcumgr.transfer.Uploader
import no.nordicsemi.kotlin.log.Log

/**
 * A layer of the Mcu Manager library.
 *
 * Each layer logs using a separate category, allowing log output to be filtered by the area of
 * the library involved. The layers are declared bottom-up, from the raw transport to the firmware
 * upgrade process built on top of it:
 *
 * ```
 * DFU        the firmware upgrade process
 * TRANSFER   chunked upload and download
 * COMMAND    a single Mcu Manager command and its response
 * PROTOCOL   SMP framing: sequence numbers, matching responses to requests
 * TRANSPORT  the connection carrying the SMP packets
 * ```
 *
 * #### Example
 * ```kotlin
 * val sink = Log.Sink.Default { category, level ->
 *     // Follow the upgrade, but only complain about the layers below it.
 *     category == Category.DFU || level >= Log.Level.WARN
 * }
 * ```
 */
enum class Category : Log.Category {

    /**
     * The transport layer, that is implementations of [McuMgrTransport].
     *
     * This layer covers setting up and tearing down the connection to the device and sending
     * the SMP packets over it. For a Bluetooth LE transport that includes connecting, service
     * discovery, MTU negotiation and writing to and receiving notifications from the SMP
     * characteristic.
     */
    TRANSPORT,

    /**
     * The SMP framing layer.
     *
     * This layer covers assigning sequence numbers to outgoing requests, matching incoming
     * responses back to them, and the transaction timeouts and overwrites that result when
     * a response does not arrive.
     */
    PROTOCOL,

    /**
     * A single Mcu Manager command and its response, as sent by a [McuManager].
     *
     * All managers report under this category, including managers implemented outside of this
     * library for user-defined SMP groups.
     */
    COMMAND,

    /**
     * Chunked upload and download, as performed by [Uploader] and its counterparts.
     *
     * This layer covers splitting the data into chunks, the offsets acknowledged by the device,
     * recovering from lost chunks and notifications, and the resulting throughput.
     */
    TRANSFER,

    /**
     * The firmware upgrade process, handled by [FirmwareUpgradeManager] and [SUITUpgradeManager].
     *
     * This layer covers the upgrade state machine and its tasks. The commands and the data
     * transfer an upgrade performs are reported under [COMMAND] and [TRANSFER] respectively.
     */
    DFU,
}
