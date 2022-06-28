package io.runtime.mcumgr.managers;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;

import io.runtime.mcumgr.McuManager;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.response.dflt.McuMgrExecResponse;

@SuppressWarnings("unused")
public class ShellManager extends McuManager {

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
