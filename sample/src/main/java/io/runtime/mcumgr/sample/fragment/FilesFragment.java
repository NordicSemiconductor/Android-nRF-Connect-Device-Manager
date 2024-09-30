/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.dialog.PartitionDialogFragment;

public class FilesFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_fs, container, false);
    }

    @Override
    public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull final Menu menu, @NonNull final MenuInflater menuInflater) {
                menuInflater.inflate(R.menu.settings, menu);
            }

            @Override
            public void onPrepareMenu(@NonNull final Menu menu) {
                menu.findItem(R.id.action_settings).setVisible(isVisible());
            }

            @Override
            public boolean onMenuItemSelected(@NonNull final MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_settings) {
                    final DialogFragment dialog = PartitionDialogFragment.getInstance();
                    dialog.show(getChildFragmentManager(), null);
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}
