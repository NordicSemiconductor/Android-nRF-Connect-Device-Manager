/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.dialog;

import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.EditText;

import javax.inject.Inject;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.utils.FsUtils;

public class PartitionDialogFragment extends DialogFragment implements Injectable {

    @Inject
    FsUtils mFsUtils;

    private InputMethodManager mImm;

    public static DialogFragment getInstance() {
        return new PartitionDialogFragment();
    }

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mImm = (InputMethodManager) requireContext().getSystemService(Context.INPUT_METHOD_SERVICE);
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        final LayoutInflater inflater = requireActivity().getLayoutInflater();
        final View view = inflater.inflate(R.layout.dialog_files_settings, null);
        final EditText partition = view.findViewById(R.id.partition);
        partition.setText(mFsUtils.getPartitionString());
        partition.selectAll();

        final AlertDialog dialog = new AlertDialog.Builder(requireContext())
                .setTitle(R.string.files_settings_title)
                .setView(view)
                // Setting the positive button listener here would cause the dialog to dismiss.
                // We have to validate the value before.
                .setPositiveButton(R.string.files_settings_action_save, null)
                .setNegativeButton(android.R.string.cancel, null)
                // Setting the neutral button listener here would cause the dialog to dismiss.
                .setNeutralButton(R.string.files_settings_action_restore, null)
                .create();
        dialog.setOnShowListener(d -> mImm.showSoftInput(partition, InputMethodManager.SHOW_IMPLICIT));

        // The neutral button should not dismiss the dialog.
        // We have to overwrite the default OnClickListener.
        // This can be done only after the dialog was shown.
        dialog.show();
        dialog.getButton(DialogInterface.BUTTON_NEUTRAL).setOnClickListener(v -> {
            final String defaultPartition = mFsUtils.getDefaultPartition();
            partition.setText(defaultPartition);
            partition.setSelection(defaultPartition.length());
        });
        dialog.getButton(DialogInterface.BUTTON_POSITIVE).setOnClickListener(v -> {
            final String newPartition = partition.getText().toString().trim();
            if (!TextUtils.isEmpty(newPartition)) {
                mFsUtils.setPartition(newPartition);
                dismiss();
            } else {
                partition.setError(getString(R.string.files_settings_error));
            }
        });
        return dialog;
    }
}
