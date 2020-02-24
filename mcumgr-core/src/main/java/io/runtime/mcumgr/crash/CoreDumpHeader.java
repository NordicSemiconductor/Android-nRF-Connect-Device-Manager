package io.runtime.mcumgr.crash;

import org.jetbrains.annotations.NotNull;

import java.io.IOException;

import io.runtime.mcumgr.util.ByteUtil;
import io.runtime.mcumgr.util.Endian;

/**
 * The header of the core dump file. Contains the magic number and the core dump size for
 * validation.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CoreDumpHeader {

    private static final int OFFSET = 0;

    private int mMagic;
    private int mSize;

    public CoreDumpHeader(int magic, int size) {
        mMagic = magic;
        mSize = size;
    }

    /**
     * Parse the core dump header from the default offset of 0.
     * @param data entire core dump in bytes
     * @return The core dump header.
     * @throws IOException If the magic number was invalid.
     */
    @NotNull
    public static CoreDumpHeader fromBytes(@NotNull byte[] data) throws IOException {
        return fromBytes(data, OFFSET);
    }

    /**
     * Parse the core dump header from a given offset.
     * @param data data containing the core dump
     * @param offset the offset to start parsing the header from.
     * @return The core dump header.
     * @throws IOException If the magic number was invalid.
     */
    @NotNull
    public static CoreDumpHeader fromBytes(@NotNull byte[] data, int offset) throws IOException {
        int magic = ByteUtil.byteArrayToUnsignedInt(data, offset, Endian.LITTLE, 4);
        if (magic != CoreDump.MAGIC) {
            throw new IOException("Illegal magic number: actual=" +
                    String.format("0x%x", magic) + ", expected=" +
                    String.format("0x%x", CoreDump.MAGIC));
        }
        int size = ByteUtil.byteArrayToUnsignedInt(data, offset + 4, Endian.LITTLE, 4);
        return new CoreDumpHeader(magic, size);
    }

    public int getMagic() {
        return mMagic;
    }

    public int getSize() {
        return mSize;
    }
}
