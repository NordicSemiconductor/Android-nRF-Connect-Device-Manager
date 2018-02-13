package io.runtime.mcumgr.util;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.cbor.CBORFactory;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

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
                new TypeReference<HashMap<String, Object>>() {};
        ByteArrayInputStream inputStream = new ByteArrayInputStream(data);
        return mapper.readValue(inputStream, typeRef);
    }
}
