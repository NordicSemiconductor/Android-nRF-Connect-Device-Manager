/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.log;

import android.support.annotation.Nullable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.util.CBOR;

public class McuMgrLogResponse extends McuMgrResponse {
    public int next_index;
    public LogResult[] logs;

    public static class LogResult {
        public String name;
        public int type;
        public Entry[] entries;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {
        public byte[] msg;
        public long ts;
        public int level;
        public int index;
        public int module;
        public String type;

        /**
         * Get a string representation of the message based on the message type.
         * @return the type decoded string of the log message or null if the msg is null or decoding
         * failed
         */
        @Nullable
        public String getMessage() {
            if (msg == null) {
                return null;
            }
            if (type != null && type.equals("cbor")) {
                try {
                    return CBOR.toString(msg);
                } catch (IOException e) {
                    e.printStackTrace();
                    return null;
                }
            } else {
                return new String(msg, StandardCharsets.UTF_8);
            }
        }
    }
}
