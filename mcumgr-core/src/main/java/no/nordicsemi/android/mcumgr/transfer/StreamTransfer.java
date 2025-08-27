package no.nordicsemi.android.mcumgr.transfer;

import org.jetbrains.annotations.Nullable;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class StreamTransfer extends Transfer {

    StreamTransfer() {
        this(0);
    }

    StreamTransfer(int offset) {
        this(offset, -1);
    }

    StreamTransfer(int offset, int totalBytes) {
        super(offset);
        mDataLength = totalBytes;
    }

    @Override
    @Deprecated
    public byte @Nullable [] getData() {
        throw new UnsupportedOperationException("StreamTransfer has no retrievable data.");
    }

    /**
     * Returns true if transfer is complete.
     */
    public boolean isFinished() {
        return mOffset == mDataLength;
    }
}
