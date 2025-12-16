package no.nordicsemi.android.ota.mcumgr;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.McuManager;
import no.nordicsemi.android.mcumgr.McuMgrCallback;
import no.nordicsemi.android.mcumgr.McuMgrErrorCode;
import no.nordicsemi.android.mcumgr.McuMgrGroupReturnCode;
import no.nordicsemi.android.mcumgr.McuMgrTransport;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.HasReturnCode;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

/**
 * A manager used to request device information required for Memfault OTA updates.
 * <p>
 * Read more: <a href="https://docs.memfault.com/docs/platform/ota">Memfault Docs</a>.
 */
@SuppressWarnings("unused")
public class MemfaultManager extends McuManager {
    // Memfault group allows to read:
    // - Software Type,
    // - HW Version,
    // - Firmware Version,
    // - Device Serial Number
    // and
    // - Project Key
    // for the Memfault OTA updates.
    private final static int GROUP_MEMFAULT = 128;

	public enum ReturnCode implements McuMgrGroupReturnCode {
		/** No error, this is implied if there is no ret value in the response */
		OK(0),

		/** Unknown error occurred. */
		UNKNOWN(1),

		/** Project key not configured. */
		NO_PROJECT_KEY(2);

		private final int mCode;

		ReturnCode(int code) {
			mCode = code;
		}

		public int value() {
			return mCode;
		}

		public static @Nullable MemfaultManager.ReturnCode valueOf(@Nullable McuMgrResponse.GroupReturnCode returnCode) {
			if (returnCode == null || returnCode.group != GROUP_MEMFAULT) {
				return null;
			}
			for (MemfaultManager.ReturnCode code : values()) {
				if (code.value() == returnCode.rc) {
					return code;
				}
			}
			return UNKNOWN;
		}
	}

	public interface Response extends HasReturnCode {

		@Nullable
		default MemfaultManager.ReturnCode getMemfaultReturnCode() {
			GroupReturnCode groupReturnCode = getGroupReturnCode();
			if (groupReturnCode == null) {
				if (getReturnCodeValue() == McuMgrErrorCode.OK.value()) {
					return MemfaultManager.ReturnCode.OK;
				}
				return MemfaultManager.ReturnCode.UNKNOWN;
			}
			return MemfaultManager.ReturnCode.valueOf(groupReturnCode);
		}
	}


	// Command IDs
	private final static int ID_DEVICE_INFO = 0;
	private final static int ID_PROJECT_KEY = 1;

	/**
	 * Construct a Memfault manager instance.
	 *
	 * @param transporter the transporter to use to send commands.
	 */
	public MemfaultManager(@NotNull final McuMgrTransport transporter) {
		super(GROUP_MEMFAULT, transporter);
	}

	//******************************************************************
	// Commands
	//******************************************************************

	/**
	 * The command allows to get the device information required for Memfault OTA (asynchronous):
     * <ul>
     *     <li>Software type</li>
     *     <li>Hardware version</li>
     *     <li>Current firmware version</li>
     *     <li>Device Serial Number</li>
     * </ul>
	 *
	 * @param callback the asynchronous callback.
	 */
	public void info(@NotNull McuMgrCallback<MemfaultDeviceInfoResponse> callback) {
        send(OP_READ, ID_DEVICE_INFO, null, DEFAULT_TIMEOUT, MemfaultDeviceInfoResponse.class, callback);
	}

	/**
	 * The command allows to get the device information required for Memfault OTA (synchronous):
     * <ul>
     *     <li>Software type</li>
     *     <li>Hardware version</li>
     *     <li>Current firmware version</li>
     *     <li>Device Serial Number</li>
     * </ul>
	 *
	 * @return The response.
	 * @throws McuMgrException Transport error. See cause.
	 */
	@NotNull
	public MemfaultDeviceInfoResponse info() throws McuMgrException {
		return send(OP_READ, ID_DEVICE_INFO, null, DEFAULT_TIMEOUT, MemfaultDeviceInfoResponse.class);
	}

    /**
     * The command allows to get the Project Key required for Memfault OTA (asynchronous).
     *
     * @param callback the asynchronous callback.
     */
    public void projectKey(@NotNull McuMgrCallback<MemfaultProjectKeyResponse> callback) {
        send(OP_READ, ID_PROJECT_KEY, null, DEFAULT_TIMEOUT, MemfaultProjectKeyResponse.class, callback);
    }

    /**
     * The command allows to get the Project Key required for Memfault OTA (synchronous).
     *
     * @return The response.
     * @throws McuMgrException Transport error. See cause.
     */
    @NotNull
    public MemfaultProjectKeyResponse projectKey() throws McuMgrException {
        return send(OP_READ, ID_PROJECT_KEY, null, DEFAULT_TIMEOUT, MemfaultProjectKeyResponse.class);
    }

}
