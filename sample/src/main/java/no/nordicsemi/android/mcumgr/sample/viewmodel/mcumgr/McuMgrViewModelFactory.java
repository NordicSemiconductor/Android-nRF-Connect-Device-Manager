/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.di.component.McuMgrViewModelSubComponent;
import no.nordicsemi.android.mcumgr.sample.viewmodel.FileBrowserViewModel;

public class McuMgrViewModelFactory implements ViewModelProvider.Factory {
    private final Map<Class<? extends ViewModel>, Callable<? extends ViewModel>> creators;

    @Inject
    public McuMgrViewModelFactory(@NonNull final McuMgrViewModelSubComponent viewModelSubComponent) {
        creators = new HashMap<>();
        // we cannot inject view models directly because they won't be bound to the owner's
        // view model scope.
        creators.put(DeviceStatusViewModel.class, viewModelSubComponent::deviceStatusViewModel);
        creators.put(EchoViewModel.class, viewModelSubComponent::echoViewModel);
        creators.put(ResetViewModel.class, viewModelSubComponent::resetViewModel);
        creators.put(ObservabilityViewModel.class, viewModelSubComponent::observabilityViewModel);
        creators.put(StatsViewModel.class, viewModelSubComponent::statsViewModel);
        creators.put(McuMgrViewModel.class, viewModelSubComponent::mcuMgrViewModel);
        creators.put(FileBrowserViewModel.class, viewModelSubComponent::fileBrowserViewModel);
        creators.put(ImageUpgradeViewModel.class, viewModelSubComponent::imageUpgradeViewModel);
        creators.put(ImageUploadViewModel.class, viewModelSubComponent::imageUploadViewModel);
        creators.put(ImageControlViewModel.class, viewModelSubComponent::imageControlViewModel);
        creators.put(ImageSettingsViewModel.class, viewModelSubComponent::imageSettingsViewModel);
        creators.put(FilesDownloadViewModel.class, viewModelSubComponent::filesDownloadViewModel);
        creators.put(FilesUploadViewModel.class, viewModelSubComponent::filesUploadViewModel);
        creators.put(ExecViewModel.class, viewModelSubComponent::execViewModel);
    }

    @SuppressWarnings("unchecked")
    @NonNull
    @Override
    public <T extends ViewModel> T create(@NonNull final Class<T> modelClass) {
        Callable<? extends ViewModel> creator = creators.get(modelClass);
        if (creator == null) {
            for (Map.Entry<Class<? extends ViewModel>, Callable<? extends ViewModel>> entry : creators.entrySet()) {
                if (modelClass.isAssignableFrom(entry.getKey())) {
                    creator = entry.getValue();
                    break;
                }
            }
        }
        if (creator == null) {
            throw new IllegalArgumentException("unknown model class " + modelClass);
        }
        try {
            return (T) creator.call();
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }
}
