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
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.DeviceStatusViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class DeviceStatusFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    @BindView(R.id.connection_status)
    TextView mConnectionStatus;
    @BindView(R.id.bonding_status)
    TextView mBondingStatus;
    @BindView(R.id.work_indicator)
    ProgressBar mWorkIndicator;

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
        return inflater.inflate(R.layout.fragment_card_device_status, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel.getConnectionState().observe(getViewLifecycleOwner(),
                resId -> mConnectionStatus.setText(resId));
        mViewModel.getBondState().observe(getViewLifecycleOwner(), resId ->
                mBondingStatus.setText(resId));
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy ->
                mWorkIndicator.setVisibility(busy ? View.VISIBLE : View.GONE));
    }
}
