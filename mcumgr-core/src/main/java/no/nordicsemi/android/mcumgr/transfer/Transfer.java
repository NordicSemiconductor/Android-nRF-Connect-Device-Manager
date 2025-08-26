package no.nordicsemi.android.mcumgr.transfer;

import org.jetbrains.annotations.Nullable;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.McuMgrResponse;

@SuppressWarnings({"WeakerAccess", "unused"})
public abstract class Transfer implements TransferCallback {

    byte @Nullable [] mData;
    int mOffset;

    int mDataLength;

    Transfer() {
        this(null, 0);
    }

    Transfer(int offset) {
        this(null, offset);
    }

    Transfer(byte @Nullable [] data) {
        this(data, 0);
    }

    Transfer(byte @Nullable [] data, int offset) {
        mData = data;
        mDataLength = data == null ? -1 : data.length;
        mOffset = offset;
    }

    /**
     * Resets the transfer status parameters.
     */
    public abstract void reset();

    /**
     * Synchronously sends or requests the part of data from given offset.
     *
     * @param offset the offset, from which data will be transferred.
     * @return the response received.
     * @throws McuMgrException a reason of a failure.
     */
    public abstract McuMgrResponse send(int offset) throws McuMgrException;

    /**
     * Synchronously sends or requests the next part of data.
     *
     * @return the response received.
     * @throws McuMgrException a reason of a failure.
     */
    @SuppressWarnings("UnusedReturnValue")
    public McuMgrResponse sendNext() throws McuMgrException {
        return send(mOffset);
    }

    /**
     * Returns the data associated with this object. For incoming transfers the data are available
     * only when the transfer is complete.
     *
     * @return the data.
     */
    public byte @Nullable [] getData() {
        return mData;
    }

    /**
     * Returns the current offset.
     *
     * @return the offset.
     */
    public int getOffset() {
        return mOffset;
    }

    /**
     * Returns true if transfer is complete.
     */
    public boolean isFinished() {
        return mData != null && mOffset == mData.length;
    }
}
