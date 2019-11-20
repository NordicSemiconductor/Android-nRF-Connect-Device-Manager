/*
 * Copyright (c) Intellinium SAS, 2014-present
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.response.log;


import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.nio.charset.Charset;

import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.CBOR;

public class McuMgrLogResponse extends McuMgrResponse {
    @JsonProperty("next_index")
    public long next_index;
    @JsonProperty("logs")
    public LogResult[] logs;

    @JsonCreator
    public McuMgrLogResponse() {}

    public static class LogResult {

        public static final int LOG_TYPE_STREAM = 0;
        public static final int LOG_TYPE_MEMORY = 1;
        public static final int LOG_TYPE_STORAGE = 2;

        /**
         * Name of the log.
         */
        @JsonProperty("name")
        public String name;

        /**
         * Type of the log. {@link #LOG_TYPE_STREAM}, {@link #LOG_TYPE_MEMORY}, or
         * {@link #LOG_TYPE_STORAGE}.
         */
        @JsonProperty("type")
        public int type;

        /**
         * Array of {@link Entry}s collected from this log.
         */
        @JsonProperty("entries")
        public Entry[] entries;

        @JsonCreator
        public LogResult() {}
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Entry {

        public static final int LOG_LEVEL_DEBUG = 0;
        public static final int LOG_LEVEL_INFO = 1;
        public static final int LOG_LEVEL_WARN = 2;
        public static final int LOG_LEVEL_ERROR = 3;
        public static final int LOG_LEVEL_CRITICAL = 4;

        public static final String LOG_ENTRY_TYPE_STRING = "str";
        public static final String LOG_ENTRY_TYPE_CBOR = "cbor";
        public static final String LOG_ENTRY_TYPE_BINARY = "bin";

        /**
         * Log entry message. Binary may be encoded as defined in the {@link #type}.
         */
        @JsonProperty("msg")
        public byte[] msg;

        /**
         * Log entry time stamp.
         */
        @JsonProperty("ts")
        public long ts;

        /**
         * Log entry level.
         */
        @JsonProperty("level")
        public int level;

        /**
         * Log entry index. The index should be unique to this entry.
         */
        @JsonProperty("index")
        public long index;

        /**
         * Module which logged the entry.
         */
        @JsonProperty("module")
        public int module;

        /**
         * Log message type. {@link #LOG_ENTRY_TYPE_STRING}, {@link #LOG_ENTRY_TYPE_CBOR}, or
         * {@link #LOG_ENTRY_TYPE_BINARY}.
         */
        @JsonProperty("type")
        public String type;

        /**
         * The first 4 bytes of the build ID (image hash) which was running when this log entry was
         * written by the device.
         */
        @Nullable
        @JsonProperty("imghash")
        public byte[] imghash;

        @JsonCreator
        public Entry() {}

        /**
         * Get a string representation of the {@link #msg} based on the message {@link #type}.
         * <p>
         * If the type is {@link #LOG_ENTRY_TYPE_BINARY}, null, or unknown, this method will return
         * the hex encoding of the {@link #msg}.
         *
         * @return the type decoded string of the log message or null if the msg is null or decoding
         * failed
         */
        @Nullable
        public String getMessageString() {
            if (msg == null) {
                return null;
            }
            if (type == null) {
                return ByteUtil.byteArrayToHex(msg);
            }
            switch (type) {
                case LOG_ENTRY_TYPE_STRING:
                    return new String(msg, Charset.forName("UTF-8"));
                case LOG_ENTRY_TYPE_CBOR:
                    try {
                        return CBOR.toString(msg);
                    } catch (IOException e) {
                        e.printStackTrace();
                        return null;
                    }
                case LOG_ENTRY_TYPE_BINARY:
                    ByteUtil.byteArrayToHex(msg);

            }
            return ByteUtil.byteArrayToHex(msg);

        }
    }
}
