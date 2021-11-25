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
    private final ImageManager manager;

    private final MutableLiveData<McuMgrImageStateResponse> responseLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> testAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> confirmAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> eraseAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();

    @NonNull
    private final byte[][] hashes;

    @Inject
    ImageControlViewModel(final ImageManager manager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
        // The current version supports 2 images.
        this.hashes = new byte[2][];
    }

    @NonNull
    public LiveData<McuMgrImageStateResponse> getResponse() {
        return responseLiveData;
    }

    @NonNull
    public LiveData<Boolean> getTestOperationAvailability() {
        return testAvailableLiveData;
    }

    @NonNull
    public LiveData<Boolean> getConfirmOperationAvailability() {
        return confirmAvailableLiveData;
    }

    @NonNull
    public LiveData<Boolean> getEraseOperationAvailability() {
        return eraseAvailableLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    public int[] getValidImages() {
        if (hashes[0] != null && hashes[1] != null)
            return new int[] { 0, 1 };
        if (hashes[0] != null)
            return new int[] { 0 };
        return new int[] { 1 };
    }

    public void read() {
        // This is called also from BLE thread after erase(), therefore postValue, not setValue.
        postBusy();
        errorLiveData.postValue(null);
        manager.list(new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                hashes[0] = hashes[1] = null;
                // Save the hash of the unconfirmed images. They are required for sending test
                // and confirm messages.
                if (response.images != null) {
                    for (McuMgrImageStateResponse.ImageSlot slot : response.images) {
                        if (!slot.confirmed) {
                            hashes[slot.image] = slot.hash;
                        }
                    }
                }
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    public void test(final int image) {
        if (image < 0 || hashes.length < image || hashes[image] == null)
            return;

        setBusy();
        errorLiveData.setValue(null);
        manager.test(hashes[image], new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    public void confirm(final int image) {
        if (image < 0 || hashes.length < image || hashes[image] == null)
            return;

        setBusy();
        errorLiveData.setValue(null);
        manager.confirm(hashes[image], new McuMgrCallback<McuMgrImageStateResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
                postReady(null);
            }
        });
    }

    public void erase(final int image) {
        if (image < 0 || hashes.length < image || hashes[image] == null)
            return;

        setBusy();
        errorLiveData.setValue(null);
        manager.erase(image, new McuMgrCallback<McuMgrResponse>() {
            @Override
            public void onResponse(@NonNull final McuMgrResponse response) {
                read();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                errorLiveData.postValue(error);
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

        responseLiveData.postValue(response);
        testAvailableLiveData.postValue(testEnabled);
        confirmAvailableLiveData.postValue(confirmEnabled);
        eraseAvailableLiveData.postValue(eraseEnabled);
        postReady();
    }
}
