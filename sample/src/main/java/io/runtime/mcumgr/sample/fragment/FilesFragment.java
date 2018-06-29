/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment;

import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.dialog.PartitionDialogFragment;

public class FilesFragment extends Fragment {

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setHasOptionsMenu(true);
	}

	@Override
	public void onCreateOptionsMenu(final Menu menu, final MenuInflater inflater) {
		inflater.inflate(R.menu.settings, menu);
	}

	@Override
	public boolean onOptionsItemSelected(final MenuItem item) {
		switch (item.getItemId()) {
			case R.id.action_settings:
				final DialogFragment dialog = PartitionDialogFragment.getInstance();
				dialog.show(getChildFragmentManager(), null);
				return true;
		}
		return false;
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_fs, container, false);
	}
}
