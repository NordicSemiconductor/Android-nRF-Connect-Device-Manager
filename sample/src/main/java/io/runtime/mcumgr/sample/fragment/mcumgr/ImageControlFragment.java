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
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageControlBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.dialog.SelectImageDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageControlViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageControlFragment extends Fragment implements Injectable,
        Toolbar.OnMenuItemClickListener, SelectImageDialogFragment.OnImageSelectedListener {
    private static final int REQUEST_TEST    = 1;
    private static final int REQUEST_CONFIRM = 2;
    private static final int REQUEST_ERASE   = 3;

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    
    private FragmentCardImageControlBinding binding;

    private ImageControlViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ImageControlViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardImageControlBinding.inflate(inflater, container, false);
        binding.toolbar.inflateMenu(R.menu.help);
        binding.toolbar.setOnMenuItemClickListener(this);
        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // This makes the layout animate when the TextView value changes.
        // By default it animates only on hiding./showing views.
        // The view must have android:animateLayoutChanges(true) attribute set in the XML.
        ((ViewGroup) view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        viewModel.getResponse().observe(getViewLifecycleOwner(), this::printImageSlotInfo);
        viewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        viewModel.getTestOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> binding.actionTest.setEnabled(enabled));
        viewModel.getConfirmOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> binding.actionConfirm.setEnabled(enabled));
        viewModel.getEraseOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> binding.actionErase.setEnabled(enabled));
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            if (busy) {
                binding.actionRead.setEnabled(false);
                binding.actionTest.setEnabled(false);
                binding.actionConfirm.setEnabled(false);
                binding.actionErase.setEnabled(false);
            } else {
                binding.actionRead.setEnabled(true);
                // Other actions will be optionally enabled by other observers
            }
        });
        binding.actionRead.setOnClickListener(v -> viewModel.read());
        binding.actionTest.setOnClickListener(v -> onActionClick(REQUEST_TEST));
        binding.actionConfirm.setOnClickListener(v -> onActionClick(REQUEST_CONFIRM));
        binding.actionErase.setOnClickListener(v -> onActionClick(REQUEST_ERASE));
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
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

    @Override
    public void onImageSelected(final int requestId, final int image) {
        switch (requestId) {
            case REQUEST_TEST:
                viewModel.test(image);
                break;
            case REQUEST_CONFIRM:
                viewModel.confirm(image);
                break;
            case REQUEST_ERASE:
                viewModel.erase(image);
                break;
        }
    }

    private void onActionClick(final int requestId) {
        final int[] images = viewModel.getValidImages();
        if (images.length > 1) {
            final DialogFragment dialog = SelectImageDialogFragment.getInstance(requestId);
            dialog.show(getChildFragmentManager(), null);
        } else {
            onImageSelected(requestId, images[0]);
        }
    }

    private void printImageSlotInfo(@Nullable final McuMgrImageStateResponse response) {
        if (response != null) {
            final SpannableStringBuilder builder = new SpannableStringBuilder();
            builder.append(getString(R.string.image_control_split_status, response.splitStatus));
            builder.setSpan(new StyleSpan(Typeface.BOLD),
                    0, builder.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            for (final McuMgrImageStateResponse.ImageSlot slot : response.images) {
                builder.append("\n");
                final int index = builder.length();
                final String imageSlot = getString(R.string.image_control_image_slot,
                        slot.image, slot.slot);
                builder.append(imageSlot);
                builder.setSpan(new StyleSpan(Typeface.BOLD),
                        index, index + imageSlot.length(),
                        Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                builder.append("\n");
                builder.append(getString(R.string.image_control_flags,
                        slot.version,
                        StringUtils.toHex(slot.hash),
                        slot.bootable, slot.pending, slot.confirmed,
                        slot.active, slot.permanent));
            }
            binding.imageControlValue.setText(builder);
            binding.imageControlError.setVisibility(View.GONE);
        } else {
            binding.imageControlValue.setText(null);
        }
    }

    private void printError(@Nullable final McuMgrException error) {
        String message = StringUtils.toString(requireContext(), error);
        if (error instanceof McuMgrErrorException) {
            final McuMgrErrorCode code = ((McuMgrErrorException) error).getCode();
            if (code == McuMgrErrorCode.UNKNOWN) {
                // User tried to test a firmware with hash equal to the hash of the
                // active firmware. This would result in changing the permanent flag
                // of the slot 0 to false, which is not possible.
                message = getString(R.string.image_control_already_flashed);
            }
        }
        if (message == null) {
            binding.imageControlError.setText(null);
            return;
        }
        final SpannableString spannable = new SpannableString(message);
        spannable.setSpan(new ForegroundColorSpan(
                        ContextCompat.getColor(requireContext(), R.color.colorError)),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        spannable.setSpan(new StyleSpan(Typeface.BOLD),
                0, message.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
        binding.imageControlError.setText(spannable);
        binding.imageControlError.setVisibility(View.VISIBLE);
    }
}
