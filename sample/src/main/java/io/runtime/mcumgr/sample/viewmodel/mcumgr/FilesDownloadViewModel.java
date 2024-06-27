/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel.mcumgr;

import javax.inject.Inject;
import javax.inject.Named;

import androidx.annotation.NonNull;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import io.runtime.mcumgr.McuMgrTransport;
import io.runtime.mcumgr.ble.McuMgrBleTransport;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.managers.FsManager;
import io.runtime.mcumgr.sample.fragment.mcumgr.FileDownloadManager;
import io.runtime.mcumgr.sample.viewmodel.SingleLiveEvent;
import io.runtime.mcumgr.transfer.DownloadCallback;
import io.runtime.mcumgr.transfer.TransferController;
import no.nordicsemi.android.ble.ConnectionPriorityRequest;

@SuppressWarnings("unused")
public class FilesDownloadViewModel extends McuMgrViewModel implements DownloadCallback {

    private final static Logger LOG = LoggerFactory.getLogger(FilesDownloadViewModel.class);

    String fileName = "";

    private final FsManager manager;
    private FileDownloadManager fileDownloadManager = new FileDownloadManager();

    private TransferController controller;

    private final MutableLiveData<Integer> progressLiveData = new MutableLiveData<>();
    private final MutableLiveData<byte[]> responseLiveData = new MutableLiveData<>();
    private final MutableLiveData<McuMgrException> errorLiveData = new MutableLiveData<>();
    private final SingleLiveEvent<Void> cancelledEvent = new SingleLiveEvent<>();

    @Inject
    FilesDownloadViewModel(final FsManager manager,
                           @Named("busy") final MutableLiveData<Boolean> state) {
        super(state);
        this.manager = manager;
    }

    @NonNull
    public LiveData<Integer> getProgress() {
        return progressLiveData;
    }

    @NonNull
    public LiveData<byte[]> getResponse() {
        return responseLiveData;
    }

    @NonNull
    public LiveData<McuMgrException> getError() {
        return errorLiveData;
    }

    @NonNull
    public LiveData<Void> getCancelledEvent() {
        return cancelledEvent;
    }

    public void download(final String path) {
        if (controller != null) {
            return;
        }
        setBusy();
        requestHighConnectionPriority();
        setLoggingEnabled(false);
        controller = manager.fileDownload(path, this);
    }

    public void pause() {
        final TransferController controller = this.controller;
        if (controller != null) {
            setLoggingEnabled(true);
            controller.pause();
            setReady();
        }
    }

    public void resume() {
        final TransferController controller = this.controller;
        if (controller != null) {
            setBusy();
            setLoggingEnabled(false);
            controller.resume();
        }
    }

    public void cancel() {
        final TransferController controller = this.controller;
        if (controller != null) {
            controller.cancel();
        }
    }

    @Override
    public void onDownloadProgressChanged(final int current, final int total, final long timestamp) {
        // Convert to percent
        progressLiveData.postValue((int) (current * 100.f / total));
    }

    @Override
    public void onDownloadFailed(@NonNull final McuMgrException error) {
        controller = null;
        progressLiveData.postValue(0);
        errorLiveData.postValue(error);
        setLoggingEnabled(true);
        postReady();
    }

    @Override
    public void onDownloadCanceled() {
        controller = null;
        progressLiveData.postValue(0);
        cancelledEvent.post();
        setLoggingEnabled(true);
        postReady();
    }

    private String buildTargetFileName() {
        SimpleDateFormat formatter = new SimpleDateFormat("MM-dd-HH.mm");
        String time = formatter.format(new Date());
        return this.fileName.isEmpty() ? String.format("nrf_%s.log", time)
        : String.format("%s_%s", time, this.fileName); // preserve file extension from original fileName
    }

    @Override
    public void onDownloadCompleted(@NonNull final byte[] data) {
        controller = null;
        LOG.debug("File download from peripheral successful. Will save to phone; size={}", data.length);
        fileDownloadManager.save(data, buildTargetFileName());
        progressLiveData.postValue(0);
        responseLiveData.postValue(data);
        setLoggingEnabled(true);
        postReady();
    }

    private void requestHighConnectionPriority() {
        final McuMgrTransport transporter = manager.getTransporter();
        if (transporter instanceof McuMgrBleTransport) {
            final McuMgrBleTransport bleTransporter = (McuMgrBleTransport) transporter;
            bleTransporter.requestConnPriority(ConnectionPriorityRequest.CONNECTION_PRIORITY_HIGH);
        }
    }

    private void setLoggingEnabled(final boolean enabled) {
        final McuMgrTransport transporter = manager.getTransporter();
        if (transporter instanceof McuMgrBleTransport) {
            final McuMgrBleTransport bleTransporter = (McuMgrBleTransport) transporter;
            bleTransporter.setLoggingEnabled(enabled);
        }
    }

    public void setFileName(final String fileName) {
        this.fileName = fileName;
    }
}
