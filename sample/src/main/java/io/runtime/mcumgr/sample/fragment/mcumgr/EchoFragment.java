/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardEchoBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.StringUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.EchoViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class EchoFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;

    private FragmentCardEchoBinding binding;

    private EchoViewModel viewModel;
    private InputMethodManager imm;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(EchoViewModel.class);
        imm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardEchoBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        binding.echoValue.setSelection(binding.echoValue.getText().length());

        viewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> binding.actionSend.setEnabled(!busy));
        viewModel.getRequest().observe(getViewLifecycleOwner(), text -> {
            binding.echoContent.setVisibility(View.VISIBLE);
            print(binding.echoRequest, text);
            if (binding.echoResponse.getVisibility() == View.VISIBLE) {
                binding.echoResponse.setVisibility(View.INVISIBLE);
            }
        });
        viewModel.getResponse().observe(getViewLifecycleOwner(), response -> {
            binding.echoResponse.setBackgroundResource(R.drawable.echo_response);
            print(binding.echoResponse, response);
        });
        viewModel.getError().observe(getViewLifecycleOwner(), error -> {
            binding.echoResponse.setBackgroundResource(R.drawable.echo_error);
            print(binding.echoResponse, StringUtils.toString(requireContext(), error));
        });
        binding.actionSend.setOnClickListener(v -> {
            binding.echoRequest.setText(null);
            binding.echoResponse.setText(null);

            hideKeyboard();

            final String text = binding.echoValue.getText().toString();
            binding.echoValue.setText(null);
            viewModel.echo(text);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    private void hideKeyboard() {
        imm.hideSoftInputFromWindow(binding.echoValue.getWindowToken(), 0);
    }

    private void print(final TextView view, final String text) {
        view.setVisibility(View.VISIBLE);
        if (TextUtils.isEmpty(text)) {
            view.setText(R.string.echo_empty);
            view.setTypeface(null, Typeface.ITALIC);
        } else {
            view.setText(text);
            view.setTypeface(null, Typeface.NORMAL);
        }
    }
}
