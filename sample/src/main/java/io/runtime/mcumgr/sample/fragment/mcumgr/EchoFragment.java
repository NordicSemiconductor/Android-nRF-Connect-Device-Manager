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

import io.runtime.mcumgr.exception.McuMgrTimeoutException;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.FragmentCardEchoBinding;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.EchoViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class EchoFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    private FragmentCardEchoBinding mBinding;

    private EchoViewModel mViewModel;
    private InputMethodManager mImm;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mViewModel = new ViewModelProvider(this, mViewModelFactory)
                .get(EchoViewModel.class);
        mImm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        mBinding = FragmentCardEchoBinding.inflate(inflater, container, false);
        return mBinding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        mBinding.echoValue.setSelection(mBinding.echoValue.getText().length());

        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> mBinding.actionSend.setEnabled(!busy));
        mViewModel.getRequest().observe(getViewLifecycleOwner(), text -> {
            mBinding.echoContent.setVisibility(View.VISIBLE);
            print(mBinding.echoRequest, text);
            if (mBinding.echoResponse.getVisibility() == View.VISIBLE) {
                mBinding.echoResponse.setVisibility(View.INVISIBLE);
            }
        });
        mViewModel.getResponse().observe(getViewLifecycleOwner(), response -> {
            mBinding.echoResponse.setBackgroundResource(R.drawable.echo_response);
            print(mBinding.echoResponse, response);
        });
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mBinding.echoResponse.setBackgroundResource(R.drawable.echo_error);
            if (error instanceof McuMgrTimeoutException) {
                print(mBinding.echoResponse, getString(R.string.status_connection_timeout));
            } else {
                print(mBinding.echoResponse, error.getMessage());
            }
        });
        mBinding.actionSend.setOnClickListener(v -> {
            mBinding.echoRequest.setText(null);
            mBinding.echoResponse.setText(null);

            hideKeyboard();

            final String text = mBinding.echoValue.getText().toString();
            mBinding.echoValue.setText(null);
            mViewModel.echo(text);
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        mBinding = null;
    }

    private void hideKeyboard() {
        mImm.hideSoftInputFromWindow(mBinding.echoValue.getWindowToken(), 0);
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
