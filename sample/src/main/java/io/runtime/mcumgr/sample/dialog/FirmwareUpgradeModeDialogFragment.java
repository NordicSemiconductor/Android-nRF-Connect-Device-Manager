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
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.runtime.mcumgr.dfu.mcuboot.FirmwareUpgradeManager;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.fragment.mcumgr.ImageUpgradeFragment;

public class FirmwareUpgradeModeDialogFragment extends DialogFragment {
    private static final String SIS_ITEM = "item";

    private int selectedItem;

    public static DialogFragment getInstance() {
        return new FirmwareUpgradeModeDialogFragment();
    }

    @SuppressWarnings("ConstantConditions")
    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
        if (savedInstanceState != null) {
            selectedItem = savedInstanceState.getInt(SIS_ITEM);
        } else {
            selectedItem = 2; // CONFIRM_ONLY
        }

        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle(R.string.image_upgrade_mode)
                .setSingleChoiceItems(R.array.image_upgrade_mode_options, selectedItem,
                        (dialog, which) -> selectedItem = which)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(R.string.image_upgrade_action_start, (dialog, which) -> {
                    final ImageUpgradeFragment parent = (ImageUpgradeFragment) getParentFragment();
                    parent.start(getMode());
                })
                .create();
    }

    @Override
    public void onSaveInstanceState(@NonNull final Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt(SIS_ITEM, selectedItem);
    }

    private FirmwareUpgradeManager.Mode getMode() {
        return switch (selectedItem) {
            case 3 -> FirmwareUpgradeManager.Mode.NONE;
            case 2 -> FirmwareUpgradeManager.Mode.CONFIRM_ONLY;
            case 1 -> FirmwareUpgradeManager.Mode.TEST_ONLY;
            default -> FirmwareUpgradeManager.Mode.TEST_AND_CONFIRM;
        };
    }
}
