package no.nordicsemi.android.mcumgr.crash;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Represents a collection of type length value entries for a core dump file.
 */
@SuppressWarnings({"unused", "WeakerAccess"})
public class CoreDumpTlv {

    private static final int OFFSET = 8;

    @NotNull
    private final List<CoreDumpTlvEntry> mEntries;

    public CoreDumpTlv(@NotNull List<CoreDumpTlvEntry> entries) {
        mEntries = entries;
    }

    /**
     * Parse the core dump TLV from core dump data from the default offset of 8.
     * @param data The entire core dump file.
     * @return The core dump TLV.
     * @throws IOException If parsing TLV entries encountered an error.
     */
    @NotNull
    public static CoreDumpTlv fromBytes(byte @NotNull [] data) throws IOException {
        return fromBytes(data, OFFSET);
    }

    /**
     * Parse the core dump TLV from core dump data from an offset.
     * @param data The core dump data.
     * @param offset The offset to start parsing the TLV from.
     * @return The core dump TLV.
     * @throws IOException If parsing TLV entries encountered an error.
     */
    @NotNull
    public static CoreDumpTlv fromBytes(byte @NotNull [] data, int offset) throws IOException {
        List<CoreDumpTlvEntry> entries = new ArrayList<>();
        while (offset < data.length) {
            CoreDumpTlvEntry entry = CoreDumpTlvEntry.fromBytes(data, offset);
            entries.add(entry);
            offset += entry.getSize();
        }
        return new CoreDumpTlv(entries);
    }

    /**
     * Get the first entry in the TLV which matches the given type.
     * @param type The type to match to the entry type.
     * @return The entry, or null if not found.
     */
    @Nullable
    public CoreDumpTlvEntry getEntryOfType(int type) {
        for (CoreDumpTlvEntry entry : mEntries) {
            if (entry.getType() == type) {
                return entry;
            }
        }
        return null;
    }

    /**
     * Get all entries in the TLV which match the given type.
     * @param type The type to match to the entry type.
     * @return A list of entries, empty if none match.
     */
    @NotNull
    public List<CoreDumpTlvEntry> getEntriesOfType(int type) {
        List<CoreDumpTlvEntry> entries = new ArrayList<>();
        for (CoreDumpTlvEntry entry : mEntries) {
            if (entry.getType() == type) {
                entries.add(entry);
            }
        }
        return entries;
    }

    /**
     * Get the list of core dump TLV entries.
     * @return the list of TLV entries
     */
    @NotNull
    public List<CoreDumpTlvEntry> getEntries() {
        return mEntries;
    }

    public int getSize() {
        int size = 0;
        for (CoreDumpTlvEntry entry : mEntries) {
            size += entry.getSize();
        }
        return size;
    }
}
