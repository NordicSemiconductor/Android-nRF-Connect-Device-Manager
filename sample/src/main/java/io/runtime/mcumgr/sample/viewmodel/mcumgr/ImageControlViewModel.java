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

    @NonNull
    private final byte[][] mHashes;

    @Inject
    ImageControlViewModel(final ImageManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        mManager = manager;
        // The current version supports 2 images.
        mHashes = new byte[2][];
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

    public int[] getValidImages() {
        if (mHashes[0] != null && mHashes[1] != null)
            return new int[] { 0, 1 };
        if (mHashes[0] != null)
            return new int[] { 0 };
        return new int[] { 1 };
    }

    public void read() {
        setBusy();
        mErrorLiveData.setValue(null);
        mManager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                mHashes[0] = mHashes[1] = null;
                // Save the hash of the unconfirmed images. They are required for sending test
                // and confirm messages.
                if (response.images != null) {
                    for (McuMgrImageStateResponse.ImageSlot slot : response.images) {
                        if (!slot.confirmed) {
                            mHashes[slot.image] = slot.hash;
                        }
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

    public void test(final int image) {
        if (image < 0 || mHashes.length < image || mHashes[image] == null)
            return;

        setBusy();
        mErrorLiveData.setValue(null);
        mManager.test(mHashes[image], new McuMgrCallback<McuMgrImageStateResponse>() {
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

    public void confirm(final int image) {
        if (image < 0 || mHashes.length < image || mHashes[image] == null)
            return;

        setBusy();
        mErrorLiveData.setValue(null);
        mManager.confirm(mHashes[image], new McuMgrCallback<McuMgrImageStateResponse>() {
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

    public void erase(final int image) {
        if (image < 0 || mHashes.length < image || mHashes[image] == null)
            return;

        setBusy();
        mErrorLiveData.setValue(null);
        mManager.erase(image, new McuMgrCallback<McuMgrResponse>() {
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
        boolean testEnabled = false;
        boolean confirmEnabled = false;
        boolean eraseEnabled = false;

        if (response != null && response.images != null) {
            for (McuMgrImageStateResponse.ImageSlot image: response.images) {
                // Skip slots with active fw.
                if (image.slot == 0)
                    continue;
                // Test should be enabled if at least one image has PENDING = false.
                if (!image.pending)
                    testEnabled = true;
                // Confirm should be enabled if at least one has PERMANENT = false.
                if (!image.permanent)
                    confirmEnabled = true;
                // Erasing a slot is possible if the image is not CONFIRMED.
                if (!image.confirmed)
                    eraseEnabled = true;
            }
        }

        mResponseLiveData.postValue(response);
        mTestAvailableLiveData.postValue(testEnabled);
        mConfirmAvailableLiveData.postValue(confirmEnabled);
        mEraseAvailableLiveData.postValue(eraseEnabled);
        postReady();
    }
}
