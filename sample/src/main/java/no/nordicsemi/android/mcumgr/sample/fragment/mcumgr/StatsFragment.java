/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.animation.LayoutTransition;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;
import java.util.Map;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.exception.McuMgrException;
import no.nordicsemi.android.mcumgr.response.stat.McuMgrStatResponse;
import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardStatsBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.utils.StringUtils;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.StatsViewModel;

public class StatsFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;

    private FragmentCardStatsBinding binding;

    private StatsViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(StatsViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardStatsBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // This makes the layout animate when the TextView value changes.
        // By default it animates only on hiding./showing views.
        // The view must have android:animateLayoutChanges(true) attribute set in the XML.
        ((ViewGroup) view).getLayoutTransition().enableTransitionType(LayoutTransition.CHANGING);

        viewModel.getResponse().observe(getViewLifecycleOwner(), this::printStats);
        viewModel.getError().observe(getViewLifecycleOwner(), this::printError);
        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> binding.actionRefresh.setEnabled(!busy));
        binding.actionRefresh.setOnClickListener(v -> viewModel.readStats());
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void printStats(@NonNull final List<McuMgrStatResponse> responses) {
        final SpannableStringBuilder builder = new SpannableStringBuilder();
        for (final McuMgrStatResponse response : responses) {
            final int start = builder.length();
            builder.append(getString(R.string.stats_module, response.name)).append("\n");
            builder.setSpan(new StyleSpan(Typeface.BOLD), start, start + response.name.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);

            for (final Map.Entry<String, Long> entry : response.fields.entrySet()) {
                builder.append(getString(R.string.stats_field,
                        entry.getKey(), entry.getValue())).append("\n");
            }
        }
        binding.statsValue.setText(builder);
    }

    private void printError(@Nullable final McuMgrException error) {
        final String message = StringUtils.toString(requireContext(), error);
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
