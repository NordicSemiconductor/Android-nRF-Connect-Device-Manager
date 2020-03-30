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
import android.widget.Button;
import android.widget.EditText;
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
import io.runtime.mcumgr.sample.viewmodel.mcumgr.EchoViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class EchoFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory mViewModelFactory;

    @BindView(R.id.action_send)
    Button mSendAction;
    @BindView(R.id.echo_value)
    EditText mValue;
    @BindView(R.id.echo_content)
    View mContent;
    @BindView(R.id.echo_request)
    TextView mRequest;
    @BindView(R.id.echo_response)
    TextView mResponse;

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
        return inflater.inflate(R.layout.fragment_card_echo, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        ButterKnife.bind(this, view);

        mValue.setSelection(mValue.getText().length());
    }

    @Override
    public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

        mViewModel.getBusyState().observe(getViewLifecycleOwner(), busy -> mSendAction.setEnabled(!busy));
        mViewModel.getRequest().observe(getViewLifecycleOwner(), text -> {
            mContent.setVisibility(View.VISIBLE);
            print(mRequest, text);
            if (mResponse.getVisibility() == View.VISIBLE) {
                mResponse.setVisibility(View.INVISIBLE);
            }
        });
        mViewModel.getResponse().observe(getViewLifecycleOwner(), response -> {
            mResponse.setBackgroundResource(R.drawable.echo_response);
            print(mResponse, response);
        });
        mViewModel.getError().observe(getViewLifecycleOwner(), error -> {
            mResponse.setVisibility(View.VISIBLE);
            mResponse.setBackgroundResource(R.drawable.echo_error);
            mResponse.setText(error);
        });
        mSendAction.setOnClickListener(v -> {
            mRequest.setText(null);
            mResponse.setText(null);

            hideKeyboard();

            final String text = mValue.getText().toString();
            mValue.setText(null);
            mViewModel.echo(text);
        });
    }

    private void hideKeyboard() {
        mImm.hideSoftInputFromWindow(mValue.getWindowToken(), 0);
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
