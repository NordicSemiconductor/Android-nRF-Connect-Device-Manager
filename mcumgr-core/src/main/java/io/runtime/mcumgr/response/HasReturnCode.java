package io.runtime.mcumgr.response;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.NotNull;

import io.runtime.mcumgr.McuMgrErrorCode;

public interface HasReturnCode {

    class GroupReturnCode {

        /** The group ID from which the response was received. */
        @JsonProperty("group")
        public int group = 0;

        /** The return code from the group. */
        @JsonProperty("rc")
        public int rc;

        @JsonCreator
        public GroupReturnCode() {}

        @Override
        @NotNull
        public String toString() {
            return rc + " (group: " + group + ")";
        }
    }

    /**
     * Return the Mcu Manager return code as an int.
     *
     * @return Mcu Manager return code.
     */
    int getReturnCodeValue();

    /**
     * Get the return code as an enum.
     *
     * @return The return code enum.
     */
    McuMgrErrorCode getReturnCode();

    /**
     * Since nRF Connect SDK version 2.4 the groups return more granular return codes than before.
     * This change was added in SMP protocol version 2.
     * <p>
     * The standard return code is reserved for the Mcu Manager errors, e.g. parsing error, unsupported
     * group, etc, while the group return code is used for the group specific errors.
     * <p>
     * Instead of using this method, consider designated methods in specific response classes.
     * @return The group specific return code.
     */
    GroupReturnCode getGroupReturnCode();
}
