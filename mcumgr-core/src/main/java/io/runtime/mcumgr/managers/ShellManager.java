package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.McuMgrGroupReturnCode;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.HasReturnCode;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.shell.McuMgrExecResponse;

@SuppressWarnings("unused")
public class ShellManager extends McuManager {
	public enum ReturnCode implements McuMgrGroupReturnCode {
		/** No error, this is implied if there is no ret value in the response */
		OK(0),

		/** Unknown error occurred. */
		UNKNOWN(1),

		/** The provided command to execute is too long. */
		COMMAND_TOO_LONG(2),

		/** No command to execute was provided. */
		EMPTY_COMMAND(3);

		private final int mCode;

		ReturnCode(int code) {
			mCode = code;
		}

		public int value() {
			return mCode;
		}

		public static @Nullable ShellManager.ReturnCode valueOf(@Nullable McuMgrResponse.GroupReturnCode returnCode) {
			if (returnCode == null || returnCode.group != GROUP_SHELL) {
				return null;
			}
			for (ShellManager.ReturnCode code : values()) {
				if (code.value() == returnCode.rc) {
					return code;
				}
			}
			return UNKNOWN;
		}
	}

	public interface Response extends HasReturnCode {

		@Nullable
		default BasicManager.ReturnCode getOsReturnCode() {
			McuMgrResponse.GroupReturnCode groupReturnCode = getGroupReturnCode();
			if (groupReturnCode == null) {
				if (getReturnCodeValue() == McuMgrErrorCode.OK.value()) {
					return BasicManager.ReturnCode.OK;
				}
				return BasicManager.ReturnCode.UNKNOWN;
			}
			return BasicManager.ReturnCode.valueOf(groupReturnCode);
		}
	}


	// Command IDs
	// https://github.com/zephyrproject-rtos/zephyr/blob/main/subsys/mgmt/mcumgr/lib/cmd/shell_mgmt/include/shell_mgmt/shell_mgmt.h
	private final static int ID_EXEC = 0;

	/**
	 * Construct a shell manager instance.
	 *
	 * @param transporter the transporter to use to send commands.
	 */
	public ShellManager(@NotNull final McuMgrTransport transporter) {
		super(GROUP_SHELL, transporter);
	}

	//******************************************************************
	// Default Commands
	//******************************************************************

	/**
	 * The command allows to execute command line in a similar way to typing
	 * it into a shell, but both a request and a response are transported with use of SMP
	 * (asynchronous).
	 *
	 * @param cmd      the command to be executed.
	 * @param argv     an array consisting arguments of the command.
	 * @param callback the asynchronous callback.
	 */
	public void exec(@NotNull String cmd, @Nullable String[] argv,
					 @NotNull McuMgrCallback<McuMgrExecResponse> callback) {
		exec(cmd, argv, DEFAULT_TIMEOUT, callback);
	}

	/**
	 * The command allows to execute command line in a similar way to typing
	 * it into a shell, but both a request and a response are transported with use of SMP
	 * (asynchronous).
	 *
	 * @param cmd      the command to be executed.
	 * @param argv     an array consisting arguments of the command.
	 * @param timeout  the operation timeout in milliseconds.
	 * @param callback the asynchronous callback.
	 */
	public void exec(@NotNull String cmd, @Nullable String[] argv,
					 long timeout,
					 @NotNull McuMgrCallback<McuMgrExecResponse> callback) {
		HashMap<String, Object> payloadMap = new HashMap<>();
		if (argv == null || argv.length == 0) {
			payloadMap.put("argv", new String[] { cmd });
		} else {
			String[] a = new String[argv.length + 1];
			a[0] = cmd;
			System.arraycopy(argv, 0, a, 1, argv.length);
			payloadMap.put("argv", a);
		}
		send(OP_WRITE, ID_EXEC, payloadMap, timeout, McuMgrExecResponse.class, callback);
	}

	/**
	 * The command allows to execute command line in a similar way to typing
	 * it into a shell, but both a request and a response are transported with use of SMP
	 * (synchronous).
	 *
	 * @param cmd  the command to be executed.
	 * @param argv an array consisting arguments of the command.
	 * @return The response.
	 * @throws McuMgrException Transport error. See cause.
	 */
	@NotNull
	public McuMgrExecResponse exec(@NotNull String cmd, @Nullable String[] argv) throws McuMgrException {
		return exec(cmd, argv, DEFAULT_TIMEOUT);
	}

	/**
	 * The command allows to execute command line in a similar way to typing
	 * it into a shell, but both a request and a response are transported with use of SMP
	 * (synchronous).
	 *
	 * @param cmd  the command to be executed.
	 * @param argv an array consisting arguments of the command.
	 * @return The response.
	 * @throws McuMgrException Transport error. See cause.
	 */
	@NotNull
	public McuMgrExecResponse exec(@NotNull String cmd, @Nullable String[] argv, long timeout) throws McuMgrException {
		HashMap<String, Object> payloadMap = new HashMap<>();
		if (argv == null || argv.length == 0) {
			payloadMap.put("argv", new String[] { cmd });
		} else {
			String[] a = new String[argv.length + 1];
			a[0] = cmd;
			System.arraycopy(argv, 0, a, 1, argv.length);
			payloadMap.put("argv", a);
		}
		return send(OP_WRITE, ID_EXEC, payloadMap, timeout, McuMgrExecResponse.class);
	}

}
