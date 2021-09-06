package io.runtime.mcumgr.crash;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;

/**
 * A core dump collected from a device. Use the {@link #fromBytes(byte[])} method to parse the file
 * into this object.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CoreDump {

    protected static final int MAGIC = 0x690c47c3;

    protected static final int TLV_TYPE_IMAGE = 1;
    protected static final int TLV_TYPE_MEM   = 2;
    protected static final int TLV_TYPE_REG   = 3;

    private final CoreDumpHeader mHeader;
    private final CoreDumpTlv mTlv;

    public CoreDump(@NotNull CoreDumpHeader header, @NotNull CoreDumpTlv tlv) {
        mHeader = header;
        mTlv = tlv;
    }

    /**
     * Parse a core dump file from a byte array.
     *
     * @param data The core dump.
     * @return the parsed core dump.
     * @throws IOException if the core dump is invalid.
     */
    @NotNull
    public static CoreDump fromBytes(byte @NotNull [] data) throws IOException {
        CoreDumpHeader header = CoreDumpHeader.fromBytes(data);
        CoreDumpTlv tlv = CoreDumpTlv.fromBytes(data);
        return new CoreDump(header, tlv);
    }

    /**
     * Get the image hash from the core dump TLV.
     *
     * @return the image hash, or null if not found in the TLV.
     */
    public byte @Nullable [] getImageHash() {
        CoreDumpTlvEntry entry = mTlv.getEntryOfType(TLV_TYPE_IMAGE);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    /**
     * Get the registers from the core dump TLV.
     *
     * @return the registers, or null if not found in the TLV.
     */
    public byte @Nullable [] getRegisters() {
        CoreDumpTlvEntry entry = mTlv.getEntryOfType(TLV_TYPE_REG);
        if (entry == null) {
            return null;
        }
        return entry.getValue();
    }

    @NotNull
    public CoreDumpHeader getHeader() {
        return mHeader;
    }

    @NotNull
    public CoreDumpTlv getTlv() {
        return mTlv;
    }
}
