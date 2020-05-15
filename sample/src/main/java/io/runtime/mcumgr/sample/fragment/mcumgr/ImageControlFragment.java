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
import android.widget.Button;
import android.widget.TextView;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.response.img.McuMgrImageStateResponse;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.HelpDialogFragment;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.ImageControlViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class ImageControlFragment extends Fragment implements Injectable, Toolbar.OnMenuItemClickListener {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    @BindView(R.id.image_control_value)
    TextView mValue;
    @BindView(R.id.image_control_error)
    TextView mError;
    @BindView(R.id.action_read)
    Button mReadAction;
    @BindView(R.id.action_test)
    Button mTestAction;
    @BindView(R.id.action_confirm)
    Button mConfirmAction;
    @BindView(R.id.action_erase)
    Button mEraseAction;

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
        return inflater.inflate(R.layout.fragment_card_image_control, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        final Toolbar toolbar = view.findViewById(R.id.toolbar);
        toolbar.inflateMenu(R.menu.help);
        toolbar.setOnMenuItemClickListener(this);

        // This makes the layout animate when the TextView value changes.
        // By default it animates only on hiding./showing views.
        // The view must have android:animateLayoutChanges(true) attribute set in the XML.
        ((ViewGroup) view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);
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
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel.getResponse().observe(getViewLifecycleOwner(), this::printImageSlotInfo);
        mViewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        mViewModel.getTestOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> mTestAction.setEnabled(enabled));
        mViewModel.getConfirmOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> mConfirmAction.setEnabled(enabled));
        mViewModel.getEraseOperationAvailability().observe(getViewLifecycleOwner(),
                enabled -> mEraseAction.setEnabled(enabled));
        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> {
            if (busy) {
                mReadAction.setEnabled(false);
                mTestAction.setEnabled(false);
                mConfirmAction.setEnabled(false);
                mEraseAction.setEnabled(false);
            } else {
                mReadAction.setEnabled(true);
                // Other actions will be optionally enabled by other observers
            }
        });
        mReadAction.setOnClickListener(v -> mViewModel.read());
        mTestAction.setOnClickListener(v -> mViewModel.test());
        mConfirmAction.setOnClickListener(v -> mViewModel.confirm());
        mEraseAction.setOnClickListener(v -> mViewModel.erase());
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
            mValue.setText(builder);
            mError.setVisibility(View.GONE);
        } else {
            mValue.setText(null);
        }
    }

    private void printError(@Nullable final String error) {
        if (error != null) {
            final SpannableString spannable = new SpannableString(error);
            spannable.setSpan(new ForegroundColorSpan(
                            ContextCompat.getColor(requireContext(), R.color.colorError)),
                    0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            spannable.setSpan(new StyleSpan(Typeface.BOLD),
                    0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
            mError.setText(spannable);
            mError.setVisibility(View.VISIBLE);
        } else {
            mError.setText(null);
        }
    }
}
