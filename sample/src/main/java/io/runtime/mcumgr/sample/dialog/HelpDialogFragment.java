/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatDialogFragment;
import androidx.fragment.app.DialogFragment;
import io.runtime.mcumgr.sample.R;

public class HelpDialogFragment extends AppCompatDialogFragment {
    private static final String ARG_TITLE_RES_ID = "titleResId";
    private static final String ARG_MESSAGE_RES_ID = "messageResId";

    @NonNull
    public static DialogFragment getInstance(@StringRes final int titleResId, @StringRes final int messageResId) {
        final DialogFragment fragment = new HelpDialogFragment();

        final Bundle args = new Bundle();
        args.putInt(ARG_TITLE_RES_ID, titleResId);
        args.putInt(ARG_MESSAGE_RES_ID, messageResId);
        fragment.setArguments(args);

        return fragment;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final Bundle args = getArguments();
        if (args == null) {
            throw new UnsupportedOperationException("HelpDialogFragment created without arguments");
        }

        final int titleResId = getArguments().getInt(ARG_TITLE_RES_ID);
        final int messageResId = getArguments().getInt(ARG_MESSAGE_RES_ID);

        return new AlertDialog.Builder(requireContext())
                .setIcon(R.drawable.ic_help)
                .setTitle(titleResId)
                .setMessage(messageResId)
                .setPositiveButton(android.R.string.ok, null)
                .create();
    }
}
