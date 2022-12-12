/*
 * Copyright (c) 2017-2018 Runtime Inc.
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import org.jetbrains.annotations.NotNull;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@SuppressWarnings("unused")
public class CBOR {
    private final static CBORFactory sFactory = new CBORFactory();

    public static byte[] toBytes(Object obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.writeValueAsBytes(obj);
    }

    public static <T> T toObject(byte[] data, Class<T> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.readValue(data, type);
    }

    public static String toString(byte[] data) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.readTree(data).toString();
    }

    public static String toString(byte[] data, int offset) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.readTree(data, offset, data.length - offset).toString();
    }

    public static <T> String toString(T obj) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.valueToTree(obj).toString();
    }

    public static Map<String, String> toStringMap(byte[] data) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        TypeReference<HashMap<String, String>> typeRef =
                new TypeReference<HashMap<String, String>>() {};
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return mapper.readValue(inputStream, typeRef);
    }

    public static Map<String, Object> toObjectMap(byte[] data) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        TypeReference<HashMap<String, Object>> typeRef =
                new TypeReference<HashMap<String, Object>>() {
                };
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return mapper.readValue(inputStream, typeRef);
    }

    public static <T> T getObject(byte @NotNull [] data, @NotNull String key, @NotNull Class<T> type) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.convertValue(mapper.readTree(data).get(key), type);
    }

    @NotNull
    public static String getString(byte @NotNull [] data, @NotNull String key) throws IOException {
        ObjectMapper mapper = new ObjectMapper(sFactory);
        return mapper.readTree(data).get(key).asText();
    }

    /**
     * Calculates the size in bytes of a CBOR encoded unsigned integer.
     */
    public static int uintLength(int n) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0");
        if (n < 24) return 1;
        if (n < 256) return 2; // 2^8
        if (n < 65535) return 3; // 2^16
        return 5; // 2^32
        // For long values it could return 9, but this won't happen here.
    }

    /**
     * Calculates the size in bytes of a CBOR encoded string.
     */
    public static int stringLength(@NotNull String s) {
        return uintLength(s.length()) + s.length();
    }

}
