/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardDeviceStatusBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.DeviceStatusViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class DeviceStatusFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    private FragmentCardDeviceStatusBinding mBinding;

    private DeviceStatusViewModel mViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(DeviceStatusViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mBinding = FragmentCardDeviceStatusBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mViewModel.getConnectionState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case CONNECTING:
                    mBinding.connectionStatus.setText(R.string.status_connecting);
                    break;
                case INITIALIZING:
                    mBinding.connectionStatus.setText(R.string.status_initializing);
                    break;
                case READY:
                    mBinding.connectionStatus.setText(R.string.status_connected);
                    break;
                case DISCONNECTING:
                    mBinding.connectionStatus.setText(R.string.status_disconnecting);
                    break;
                case DISCONNECTED:
                    mBinding.connectionStatus.setText(R.string.status_disconnected);
                    break;
                case TIMEOUT:
                    mBinding.connectionStatus.setText(R.string.status_connection_timeout);
                    break;
                case NOT_SUPPORTED:
                    mBinding.connectionStatus.setText(R.string.status_not_supported);
                    break;
            }
        });
        mViewModel.getBondState().observe(getViewLifecycleOwner(), state -> {
            switch (state) {
                case NOT_BONDED:
                    mBinding.bondingStatus.setText(R.string.status_not_bonded);
                    break;
                case BONDING:
                    mBinding.bondingStatus.setText(R.string.status_bonding);
                    break;
                case BONDED:
                    mBinding.bondingStatus.setText(R.string.status_bonded);
                    break;
            }
        });
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy ->
                mBinding.workIndicator.setVisibility(busy ? View.VISIBLE : View.GONE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }
}
