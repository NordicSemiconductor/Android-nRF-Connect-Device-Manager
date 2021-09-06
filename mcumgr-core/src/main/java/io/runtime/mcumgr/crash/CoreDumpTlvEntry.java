package io.runtime.mcumgr.crash;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Arrays;

import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.Endian;

/**
 * Core dump type length value entry. These entries contain an additional 32-bit unsigned integer
 * value, off, which marks the memory offset of entries with type {@link CoreDump#TLV_TYPE_MEM}.
 * For other types this value should be set to zero and can be ignored.
 *
 * Member variables for type, length, and off use higher bit width primitives to ensure we don't
 * overflow Java's signed integer types.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CoreDumpTlvEntry {

    private static final int MIN_SIZE = 8;

    private final int mType;      // uint8_t
    private final int mLength;    // uint16_t
    private final long mOff;      // uint32_t
    private final byte @NotNull [] mValue;

    public CoreDumpTlvEntry(int type, int length, long off, byte @NotNull [] value) {
        mType = type;
        mLength = length;
        mOff = off;
        mValue = value;
    }

    public int getType() {
        return mType;
    }

    public int getLength() {
        return mLength;
    }

    public long getOff() {
        return mOff;
    }

    public byte @NotNull [] getValue() {
        return mValue;
    }

    public int getSize() {
        return MIN_SIZE + mLength;
    }

    @NotNull
    public static CoreDumpTlvEntry fromBytes(byte @NotNull [] data, int offset) throws IOException {
        if (offset + MIN_SIZE > data.length) {
            throw new IOException("Insufficient data. TLV entry requires at least 8 bytes. " +
                    "(length=" + data.length +
                    ", offset=" + offset + ").");
        }
        // Extract type, length, and offset
        int type = ByteUtil.byteToUnsignedInt(data[offset]);
        int length = ByteUtil.byteArrayToUnsignedInt(data, offset + 2, Endian.LITTLE, 2);
        long off = ByteUtil.byteArrayToUnsignedLong(data, offset + 4, Endian.LITTLE, 4);
        if (offset + MIN_SIZE + length > data.length) {
            throw new IOException("Insufficient data. TLV Value length out of bounds. " +
                    "(data length=" + data.length + ", offset=" + offset +
                    ", entry length=" + length + ").");
        }
        byte[] value = Arrays.copyOfRange(data, offset + MIN_SIZE, offset + MIN_SIZE + length);
        return new CoreDumpTlvEntry(type, length, off, value);
    }

    @NotNull
    @Override
    public String toString() {
        return String.format("{type=%s, length=%s, off=%s, value=%s}", mType, mLength, mOff,
                ByteUtil.byteArrayToHex(mValue));
    }
}
