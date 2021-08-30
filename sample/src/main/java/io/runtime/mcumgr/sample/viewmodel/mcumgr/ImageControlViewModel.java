/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;

public class ImageControlViewModel extends McuMgrViewModel {
    private final ImageManager mManager;

    private final MutableLiveData<McuMgrImageStateResponse> mResponseLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mTestAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mConfirmAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> mEraseAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> mErrorLiveData = new MutableLiveData<>();

    private byte[] mHash;

    @Inject
    ImageControlViewModel(final ImageManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
    }

    @NonNull
    public LiveData<McuMgrImageStateResponse> getResponse() {
        return mResponseLiveData;
    }

    @NonNull
    public LiveData<Boolean> getTestOperationAvailability() {
        return mTestAvailableLiveData;
    }

    @NonNull
    public LiveData<Boolean> getConfirmOperationAvailability() {
        return mConfirmAvailableLiveData;
    }

    @NonNull
    public LiveData<Boolean> getEraseOperationAvailability() {
        return mEraseAvailableLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return mErrorLiveData;
    }

    public void read() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                // Save the hash of the image flashed to slot 1.
                final boolean hasSlot1 = response.images != null && response.images.length > 1;
                if (hasSlot1) {
                    if (response.images[0].confirmed) {
                        // New image is in slot 1.
                        mHash = response.images[1].hash;
                    } else {
                        // It's a test mode. The new image temporarily is in slot 0.
                        mHash = response.images[0].hash;
                    }
                }
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    public void test() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.test(mHash, new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    public void confirm() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.confirm(mHash, new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    public void erase() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.erase(new McuMgrCallback<McuMgrResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrResponse response) {
                read();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                mErrorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    private void postReady(@Nullable final McuMgrImageStateResponse response) {
        final boolean hasSlot1 = response != null
                && response.images != null && response.images.length > 1;
        final boolean slot1NotPending = hasSlot1 && !response.images[1].pending;
        final boolean slot1NotPermanent = hasSlot1 && !response.images[1].permanent;
        final boolean slot1NotConfirmed = hasSlot1 && !response.images[1].confirmed;
        mResponseLiveData.postValue(response);
        mTestAvailableLiveData.postValue(slot1NotPending);
        mConfirmAvailableLiveData.postValue(slot1NotPermanent);
        mEraseAvailableLiveData.postValue(slot1NotConfirmed);
        postReady();
    }
}
