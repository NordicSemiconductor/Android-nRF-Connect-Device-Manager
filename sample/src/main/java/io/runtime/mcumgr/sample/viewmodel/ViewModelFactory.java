/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.viewmodel;

import android.app.Application;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.Callable;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;
import io.runtime.mcumgr.sample.di.component.ViewModelSubComponent;

public class ViewModelFactory extends ViewModelProvider.AndroidViewModelFactory {
    private final Map<Class<? extends ViewModel>, Callable<? extends ViewModel>> creators;

    /**
     * Creates a {@code AndroidViewModelFactory}.
     *
     * @param application an application to pass in {@link androidx.lifecycle.AndroidViewModel}.
     */
    @Inject
    public ViewModelFactory(@NonNull final Application application,
                            @NonNull final ViewModelSubComponent viewModelSubComponent) {
        super(application);

        creators = new HashMap<>();
        // we cannot inject view models directly because they won't be bound to the owner's
        // view model scope.
        creators.put(ScannerViewModel.class, viewModelSubComponent::scannerViewModel);
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
