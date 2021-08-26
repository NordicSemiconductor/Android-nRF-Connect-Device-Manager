/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.animation.LayoutTransition;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageControlBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageControlViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageControlFragment extends Fragment implements Injectable, Toolbar.OnMenuItemClickListener {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;
    
    private FragmentCardImageControlBinding mBinding;

    private ImageControlViewModel mViewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(ImageControlViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mBinding = FragmentCardImageControlBinding.inflate(inflater, container, false);
        return  mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.help);
        toolbar.setOnMenuItemClickListener(this);

        // This makes the layout animate when the TextView value changes.
        // By default it animates only on hiding./showing views.
        // The view must have android:animateLayoutChanges(true) attribute set in the XML.
        ((ViewGroup) view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        mViewModel.getResponse().observe(getViewLifecycleOwner(), this::printImageSlotInfo);
        mViewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        mViewModel.getTestOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> mBinding.actionTest.setEnabled(enabled));
        mViewModel.getConfirmOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> mBinding.actionConfirm.setEnabled(enabled));
        mViewModel.getEraseOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> mBinding.actionErase.setEnabled(enabled));
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            if (busy) {
                mBinding.actionRead.setEnabled(false);
                mBinding.actionTest.setEnabled(false);
                mBinding.actionConfirm.setEnabled(false);
                mBinding.actionErase.setEnabled(false);
            } else {
                mBinding.actionRead.setEnabled(true);
                // Other actions will be optionally enabled by other observers
            }
        });
        mBinding.actionRead.setOnClickListener(v -> mViewModel.read());
        mBinding.actionTest.setOnClickListener(v -> mViewModel.test());
        mBinding.actionConfirm.setOnClickListener(v -> mViewModel.confirm());
        mBinding.actionErase.setOnClickListener(v -> mViewModel.erase());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    @Override
    public boolean onMenuItemClick(final MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_help:
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.image_control_dialog_help_title,
                        R.string.image_control_dialog_help_message);
                dialog.show(getChildFragmentManager(), null);
                return true;
        }
        return false;
    }

    private void printImageSlotInfo(@Nullable final McuMgrImageStateResponse response) {
        if (response != null) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(getString(R.string.image_control_split_status, response.splitStatus));
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    0, builder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            for (final McuMgrImageStateResponse.ImageSlot slot : response.images) {
                final int index = builder.length();
                builder.append("\n");
                builder.append(getString(R.string.image_control_slot,
                        slot.slot, slot.version, StringUtils.toHex(slot.hash),
                        slot.bootable, slot.pending, slot.confirmed,
                        slot.active, slot.permanent));
                builder.setSpan(new StyleSpan(Typeface.BOLD),
                        index, index + 8, Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            }
            mBinding.imageControlValue.setText(builder);
            mBinding.imageControlError.setVisibility(View.GONE);
        } else {
            mBinding.imageControlValue.setText(null);
        }
    }

    private void printError(@Nullable final McuMgrException error) {
        String message = error != null ? error.getMessage() : null;
        if (error instanceof McuMgrErrorException) {
            final McuMgrErrorCode code = ((McuMgrErrorException) error).getCode();
            if (code == McuMgrErrorCode.UNKNOWN) {
                // User tried to test a firmware with hash equal to the hash of the
                // active firmware. This would result in changing the permanent flag
                // of the slot 0 to false, which is not possible.
                message = getString(R.string.image_control_already_flashed);
            }
        }
        if (error instanceof McuMgrTimeoutException) {
            message = getString(R.string.status_connection_timeout);
        }
        if (message == null) {
            mBinding.imageControlError.setText(null);
            return;
        }
        final SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        mBinding.imageControlError.setText(spannable);
        mBinding.imageControlError.setVisibility(View.VISIBLE);
    }
}
