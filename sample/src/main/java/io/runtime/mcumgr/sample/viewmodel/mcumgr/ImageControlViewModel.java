/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;

import io.runtime.mcumgr.McuMgrCallback;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.managers.DefaultManager;
import io.runtime.mcumgr.managers.ImageManager;
import io.runtime.mcumgr.managers.SUITManager;
import io.runtime.mcumgr.response.McuMgrResponse;
import io.runtime.mcumgr.response.dflt.McuMgrBootloaderInfoResponse;
import io.runtime.mcumgr.response.img.McuMgrImageResponse;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestListResponse;
import io.runtime.mcumgr.response.suit.McuMgrManifestStateResponse;
import timber.log.Timber;

public class ImageControlViewModel extends McuMgrViewModel {
    @NonNull
    private final DefaultManager osManager;
    @NonNull
    private final ImageManager manager;
    @NonNull
    private final SUITManager suitManager;

    private final MutableLiveData<McuMgrImageStateResponse> responseLiveData = new MutableLiveData<>();
    private final MutableLiveData<List<McuMgrManifestStateResponse>> manifestsLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> testAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> confirmAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<Boolean> eraseAvailableLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();

    @NonNull
    private final byte[][] hashes;

    private interface OnBootloaderReceived {
        void run(@NonNull BootloaderType type);
    }

    private enum BootloaderType {
        MCUBOOT,
        SUIT
    }
    private BootloaderType bootloaderType = null;

    @Inject
    ImageControlViewModel(@NonNull final DefaultManager osManager,
                          @NonNull final ImageManager manager,
                          @NonNull final SUITManager suitManager,
                          @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.osManager = osManager;
        this.manager = manager;
        this.suitManager = suitManager;
        // The current version supports 2 images.
        this.hashes = new byte[2][];
    }

    @NonNull
    public LiveData<McuMgrImageStateResponse> getResponse() {
        return responseLiveData;
    }

    @NonNull
    public LiveData<List<McuMgrManifestStateResponse>> getManifests() {
        return manifestsLiveData;
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

    private void withBootloader(@NonNull OnBootloaderReceived callback) {
        if (bootloaderType != null) {
            callback.run(bootloaderType);
            return;
        }

        setBusy();
        errorLiveData.setValue(null);
        osManager.bootloaderInfo(DefaultManager.BOOTLOADER_INFO_QUERY_BOOTLOADER, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NotNull McuMgrBootloaderInfoResponse response) {
                if (response.bootloader != null && response.bootloader.equals("SUIT")) {
                    bootloaderType = BootloaderType.SUIT;
                } else  {
                    bootloaderType = BootloaderType.MCUBOOT;
                }
                postReady();
                callback.run(bootloaderType);
            }

            @Override
            public void onError(@NotNull McuMgrException error) {
                if (error instanceof McuMgrErrorException) {
                    bootloaderType = BootloaderType.MCUBOOT;
                    callback.run(bootloaderType);
                } else {
                    postError(error);
                }
            }
        });
    }

    public void read() {
        // This is called also from BLE thread after erase(), therefore postValue, not setValue.
        postBusy();
        errorLiveData.postValue(null);

        withBootloader(type -> {
            if (type == BootloaderType.SUIT) {
                readSuit();
            } else {
                readMcuboot();
            }
        });
    }

    private void readMcuboot() {
        manager.list(new McuMgrCallback<>() {
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
                postError(error);
            }
        });
    }

    private void readSuit() {
        // Hashes are not used in SUIT mode.
        hashes[0] = hashes[1] = null;

        suitManager.listManifests(new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrManifestListResponse list) {
                final List<McuMgrManifestStateResponse> manifests = new ArrayList<>();
                if (list.manifests == null || list.manifests.isEmpty()) {
                    postReady(manifests);
                    return;
                }
                // Oh, I wish this class was in Kotlin and I could just use coroutines...
                final Object lock = new Object();
                new Thread(() -> {
                    for (McuMgrManifestListResponse.Manifest manifest : list.manifests) {
                        suitManager.getManifestState(manifest.role, new McuMgrCallback<>() {
                            @Override
                            public void onResponse(@NotNull McuMgrManifestStateResponse response) {
                                manifests.add(response);
                                synchronized (lock) {
                                    lock.notifyAll();
                                }
                            }

                            @Override
                            public void onError(@NotNull McuMgrException error) {
                                Timber.e(error, "Error for %d", manifest.role);
                                synchronized (lock) {
                                    lock.notifyAll();
                                }
                            }
                        });
                        try {
                            synchronized (lock) {
                                lock.wait(1000);
                            }
                        } catch (final InterruptedException e) {
                            Timber.w("Response not received within 10 seconds");
                            postError(new McuMgrTimeoutException(e));
                            return;
                        }
                    }
                    postReady(manifests);
                }).start();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                Timber.e(error, "Error when listing SUIT manifests");
                postError(error);
            }
        });
    }

    public void test(final int image) {
        if (image < 0 || hashes.length < image || hashes[image] == null)
            return;

        setBusy();
        errorLiveData.setValue(null);
        manager.test(hashes[image], new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                postError(error);
            }
        });
    }

    public void confirm(final int image) {
        if (image < 0 || hashes.length < image || hashes[image] == null)
            return;

        setBusy();
        errorLiveData.setValue(null);
        manager.confirm(hashes[image], new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageStateResponse response) {
                postReady(response);
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                postError(error);
            }
        });
    }

    public void erase(final int image) {
        if (image < 0 || hashes.length < image || hashes[image] == null) {
            // In SUIT hashes aren't used, but it's possible to send CleanUp command.
            if (image == 1 && hashes.length == 2 && hashes[0] == null && hashes[1] == null) {
                setBusy();
                errorLiveData.setValue(null);
                suitManager.cleanup(new McuMgrCallback<>() {
                    @Override
                    public void onResponse(@NotNull McuMgrResponse response) {
                        eraseAvailableLiveData.postValue(true);
                        postReady();
                    }

                    @Override
                    public void onError(@NotNull McuMgrException error) {
                        postError(error);
                    }
                });
            }
            return;
        }

        setBusy();
        errorLiveData.setValue(null);
        manager.erase(image, new McuMgrCallback<>() {
            @Override
            public void onResponse(@NonNull final McuMgrImageResponse response) {
                read();
            }

            @Override
            public void onError(@NonNull final McuMgrException error) {
                postError(error);
            }
        });
    }

    private void postReady(@Nullable final List<McuMgrManifestStateResponse> manifests) {
        responseLiveData.postValue(null);
        manifestsLiveData.postValue(manifests);
        testAvailableLiveData.postValue(false);
        confirmAvailableLiveData.postValue(false);
        eraseAvailableLiveData.postValue(manifests != null && !manifests.isEmpty());
        postReady();
    }

    private void postReady(@NonNull final McuMgrImageStateResponse response) {
        boolean testEnabled = false;
        boolean confirmEnabled = false;
        boolean eraseEnabled = false;

        if (response.images != null) {
            for (McuMgrImageStateResponse.ImageSlot image: response.images) {
                // Skip slots with active fw.
                if (image.active)
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

    private void postError(McuMgrException error) {
        errorLiveData.postValue(error);
        responseLiveData.postValue(null);
        testAvailableLiveData.postValue(false);
        confirmAvailableLiveData.postValue(false);
        eraseAvailableLiveData.postValue(false);
        postReady();
    }
}
