package io.runtime.mcumgr.transfer;

import android.os.ConditionVariable;

import org.jetbrains.annotations.NotNull;

import java.util.concurrent.Callable;

import io.runtime.mcumgr.exception.InsufficientMtuException;
import io.runtime.mcumgr.exception.McuMgrException;

public class TransferCallable implements Callable<Transfer>, TransferController {

    public enum State {
        NONE, TRANSFER, PAUSED, CLOSED
    }

    private final Transfer mTransfer;
    private State mState;
    private final ConditionVariable mPauseLock = new ConditionVariable(true);

    public TransferCallable(@NotNull Transfer transfer) {
        mTransfer = transfer;
        mState = State.NONE;
    }

    public Transfer getTransfer() {
        return mTransfer;
    }

    public TransferCallable.State getState() {
        return mState;
    }

    @Override
    public synchronized void pause() {
        if (mState == State.TRANSFER) {
            mState = State.PAUSED;
            mPauseLock.close();
        }
    }

    @Override
    public synchronized void resume() {
        if (mState == State.PAUSED) {
            mState = State.TRANSFER;
            mPauseLock.open();
        }
    }

    @Override
    public synchronized void cancel() {
        mState = State.CLOSED;
        mPauseLock.open();
        mTransfer.onCanceled();
    }

    private synchronized void failTransfer(McuMgrException e) {
        mState = State.CLOSED;
        mTransfer.onFailed(e);
    }

    private synchronized void completeTransfer() {
        mState = State.CLOSED;
        mTransfer.onCompleted();
    }

    @Override
    public Transfer call() throws InsufficientMtuException {
        if (mState == State.CLOSED) {
            return mTransfer;
        }
        while (!mTransfer.isFinished()) {
            // Block if the transfer has been paused
            mPauseLock.block();

            // Check if transfer hasn't been cancelled while paused
            if (mState == State.CLOSED) {
                return mTransfer;
            }

            // Send the next packet
            mState = State.TRANSFER;
            try {
                mTransfer.sendNext();
            } catch (McuMgrException e) {
                if (e instanceof InsufficientMtuException) {
                    throw (InsufficientMtuException) e;
                }
                failTransfer(e);
                return mTransfer;
            }

            synchronized (this) {
                // Check if transfer hasn't been cancelled.
                if (mState == State.CLOSED) {
                    return mTransfer;
                }

                // Call the progress callback.
                mTransfer.onProgressChanged(mTransfer.getOffset(), mTransfer.mDataLength,
                        System.currentTimeMillis());
            }
        }
        completeTransfer();
        return mTransfer;
    }
}
