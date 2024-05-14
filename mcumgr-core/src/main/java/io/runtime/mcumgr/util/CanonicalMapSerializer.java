package io.runtime.mcumgr.util;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.BeanProperty;
import com.fasterxml.jackson.databind.JsonSerializer;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.ser.std.MapSerializer;
import com.fasterxml.jackson.databind.type.SimpleType;
import com.fasterxml.jackson.databind.util.ClassUtil;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;
import java.util.Set;

/**
 * This serializer does exactly the same as {@link MapSerializer},
 * but is using canonical form of encoding maps, instead of
 * indefinite length.
 * That is, a map is encoded using it's length instead of a STOP sign.
 * <ul>
 *     <li>A0 - map of size 0</li>
 *     <li>A3 - map of size 3</li>
 *     <li>An - where n is in { 0 - F }</li>
 *     <li>B0 - map of size 16</li>
 *     <li>B7 - max map size encoded in a single byte (23 pairs)</li>
 *     <li>B8 18 - map of size 24 pairs</li>
 * </ul>
 * Before, maps were using BF - map of indefinite length, ending with FF (stop sign).
 */
public class CanonicalMapSerializer extends MapSerializer {

    public CanonicalMapSerializer() {
        super(Collections.emptySet(), null, SimpleType.constructUnsafe(Object.class), SimpleType.constructUnsafe(Object.class), false, null, null, null);
    }

    @Override
    public void serialize(
            Map<?, ?> value, JsonGenerator gen, SerializerProvider provider)
      throws IOException, JsonProcessingException {
        // This is the only line that had to be modified:
        gen.writeStartObject(value, value.size());

        // Rest is as in super class:
        this.serializeWithoutTypeInfo(value, gen, provider);
        gen.writeEndObject();
    }

    // These methods are required to make the overriding class to work:

    public CanonicalMapSerializer(CanonicalMapSerializer canonicalMapSerializer, BeanProperty property, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer, Set<String> ignored, Set<String> included) {
        super(canonicalMapSerializer, property, keySerializer, valueSerializer, ignored, included);
    }

    public CanonicalMapSerializer(CanonicalMapSerializer ser, Object filterId, boolean sortKeys) {
        super(ser, filterId, sortKeys);
    }

    protected void _ensureOverride(String method) {
        ClassUtil.verifyMustOverride(CanonicalMapSerializer.class, this, method);
    }

    public CanonicalMapSerializer withResolved(BeanProperty property, JsonSerializer<?> keySerializer, JsonSerializer<?> valueSerializer, Set<String> ignored, Set<String> included, boolean sortKeys) {
        this._ensureOverride("withResolved");
        CanonicalMapSerializer ser = new CanonicalMapSerializer(this, property, keySerializer, valueSerializer, ignored, included);
        if (sortKeys != ser._sortKeys) {
            ser = new CanonicalMapSerializer(ser, this._filterId, sortKeys);
        }

        return ser;
    }
}