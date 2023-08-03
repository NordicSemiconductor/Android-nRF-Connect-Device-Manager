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
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import io.runtime.mcumgr.McuMgrErrorCode;
import io.runtime.mcumgr.exception.McuMgrErrorException;
import io.runtime.mcumgr.exception.McuMgrException;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardImageEraseSettingsBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageSettingsViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageSettingsFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;
    
    private FragmentCardImageEraseSettingsBinding binding;

    private ImageSettingsViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ImageSettingsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardImageEraseSettingsBinding.inflate(inflater, container, false);
        binding.toolbar.inflateMenu(R.menu.help);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_help) {
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.image_settings_dialog_help_title,
                        R.string.image_settings_dialog_help_message);
                dialog.show(getChildFragmentManager(), null);
                return true;
            }
            return false;
        });
        return  binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // This makes the layout animate when the TextView value changes.
        // By default it animates only on hiding./showing views.
        // The view must have android:animateLayoutChanges(true) attribute set in the XML.
        ((ViewGroup) view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        viewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            // Other actions will be optionally enabled by other observers
            binding.actionErase.setEnabled(!busy);
        });
        binding.actionErase.setOnClickListener(v -> viewModel.eraseSettings());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void printError(@Nullable final McuMgrException error) {
        String message = StringUtils.toString(requireContext(), error);
        if (error instanceof McuMgrErrorException e) {
            final McuMgrErrorCode code = e.getCode();
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
