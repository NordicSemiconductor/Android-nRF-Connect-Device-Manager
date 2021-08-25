/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentManager;
import androidx.lifecycle.ViewModelProvider;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageFragment extends Fragment implements Injectable {
    private static final String SIS_MODE_ADVANCED = "advanced";

    @SuppressWarnings("WeakerAccess")
    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    private McuMgrViewModel mViewModel;

    // Basic
    private Fragment mImageUpgradeFragment;
    // Advanced
    private Fragment mImageUploadFragment;
    private Fragment mImageControlFragment;
    private Fragment mResetFragment;

    private boolean mModeAdvanced;
    private boolean mOperationInProgress;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);

        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(McuMgrViewModel.class);

        mModeAdvanced = savedInstanceState != null &&
                savedInstanceState.getBoolean(SIS_MODE_ADVANCED);
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean(SIS_MODE_ADVANCED, mModeAdvanced);
    }

    @Override
    public void onCreateOptionsMenu(@NonNull final Menu menu, @NonNull final MenuInflater inflater) {
        inflater.inflate(R.menu.image_mode, menu);

        menu.findItem(R.id.action_switch_to_advanced)
                .setVisible(!mModeAdvanced)
                .setEnabled(!mOperationInProgress);
        menu.findItem(R.id.action_switch_to_basic)
                .setVisible(mModeAdvanced)
                .setEnabled(!mOperationInProgress);
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_switch_to_advanced:
                mModeAdvanced = true;
                getChildFragmentManager().beginTransaction()
                        .show(mImageUploadFragment)
                        .show(mImageControlFragment)
                        .show(mResetFragment)
                        .hide(mImageUpgradeFragment)
                        .commit();
                requireActivity().invalidateOptionsMenu();
                return true;
            case R.id.action_switch_to_basic:
                mModeAdvanced = false;
                getChildFragmentManager().beginTransaction()
                        .show(mImageUpgradeFragment)
                        .hide(mImageUploadFragment)
                        .hide(mImageControlFragment)
                        .hide(mResetFragment)
                        .commit();
                requireActivity().invalidateOptionsMenu();
                return true;
        }
        return false;
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_image, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final FragmentManager fm = getChildFragmentManager();
        mImageUpgradeFragment = fm.findFragmentById(R.id.fragment_image_upgrade);
        mImageUploadFragment = fm.findFragmentById(R.id.fragment_image_upload);
        mImageControlFragment = fm.findFragmentById(R.id.fragment_image_control);
        mResetFragment = fm.findFragmentById(R.id.fragment_reset);

        // Initially, show only the basic Image Upgrade fragment
        if (savedInstanceState == null) {
            getChildFragmentManager().beginTransaction()
                    .hide(mImageUploadFragment)
                    .hide(mImageControlFragment)
                    .hide(mResetFragment)
                    .commit();
        }

        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            mOperationInProgress = busy;
            requireActivity().invalidateOptionsMenu();
        });
    }
}
