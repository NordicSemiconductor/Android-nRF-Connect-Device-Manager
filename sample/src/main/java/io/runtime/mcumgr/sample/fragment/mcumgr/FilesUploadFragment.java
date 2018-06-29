/*
 * Copyright (c) 2018, Nordic Semiconductor
 *
 * SPDX-License-Identifier: Apache-2.0
 */

package io.runtime.mcumgr.sample.fragment.mcumgr;

import android.arch.lifecycle.ViewModelProviders;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.support.v4.content.ContextCompat;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.ForegroundColorSpan;
import android.text.style.StyleSpan;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;

import javax.inject.Inject;

import butterknife.BindView;
import butterknife.ButterKnife;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.di.Injectable;
import io.runtime.mcumgr.sample.dialog.GenerateFileDialogFragment;
import io.runtime.mcumgr.sample.utils.FsUtils;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.FilesUploadViewModel;
import io.runtime.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;

public class FilesUploadFragment extends FileBrowserFragment implements Injectable {

	@Inject
	McuMgrViewModelFactory mViewModelFactory;
	@Inject
	FsUtils mFsUtils;

	@BindView(R.id.file_name)
	TextView mFileName;
	@BindView(R.id.file_path)
	TextView mFileDestination;
	@BindView(R.id.file_size)
	TextView mFileSize;
	@BindView(R.id.status)
	TextView mStatus;
	@BindView(R.id.progress)
	ProgressBar mProgress;
	@BindView(R.id.action_generate)
	Button mGenerateFileAction;
	@BindView(R.id.action_select_file)
	Button mSelectFileAction;
	@BindView(R.id.action_upload)
	Button mUploadAction;
	@BindView(R.id.action_cancel)
	Button mCancelAction;
	@BindView(R.id.action_pause_resume)
	Button mPauseResumeAction;

	private FilesUploadViewModel mViewModel;

	@Override
	public void onCreate(@Nullable final Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		mViewModel = ViewModelProviders.of(this, mViewModelFactory)
				.get(FilesUploadViewModel.class);
	}

	@Nullable
	@Override
	public View onCreateView(@NonNull final LayoutInflater inflater,
							 @Nullable final ViewGroup container,
							 @Nullable final Bundle savedInstanceState) {
		return inflater.inflate(R.layout.fragment_card_files_upload, container, false);
	}

	@Override
	public void onViewCreated(@NonNull final View view, @Nullable final Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);
		ButterKnife.bind(this, view);
	}

	@SuppressWarnings("ConstantConditions")
	@Override
	public void onActivityCreated(@Nullable final Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		mFsUtils.getPartition().observe(this, partition -> {
			if (isFileLoaded()) {
				final String fileName = mFileName.getText().toString();
				mFileDestination.setText(getString(R.string.files_file_path, partition, fileName));
			}
		});
		mViewModel.getState().observe(this, state -> {
			mUploadAction.setEnabled(isFileLoaded());
			mCancelAction.setEnabled(state.canCancel());
			mPauseResumeAction.setEnabled(state.canPauseOrResume());
			mPauseResumeAction.setText(state == FilesUploadViewModel.State.PAUSED ?
					R.string.image_action_resume : R.string.image_action_pause);

			mSelectFileAction.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
			mUploadAction.setVisibility(state.inProgress() ? View.GONE : View.VISIBLE);
			mCancelAction.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
			mPauseResumeAction.setVisibility(state.inProgress() ? View.VISIBLE : View.GONE);
			// Update status
			switch (state) {
				case UPLOADING:
					mStatus.setText(R.string.files_upload_status_uploading);
					break;
				case PAUSED:
					mStatus.setText(R.string.files_upload_status_paused);
					break;
				case COMPLETE:
					clearFileContent();
					mStatus.setText(R.string.image_upgrade_status_completed);
					break;
			}
		});
		mViewModel.getProgress().observe(this, progress -> mProgress.setProgress(progress));
		mViewModel.getError().observe(this, error -> {
			mGenerateFileAction.setVisibility(View.VISIBLE);
			mSelectFileAction.setVisibility(View.VISIBLE);
			mUploadAction.setVisibility(View.VISIBLE);
			mCancelAction.setVisibility(View.GONE);
			mPauseResumeAction.setVisibility(View.GONE);
			printError(error);
		});
		mViewModel.getCancelledEvent().observe(this, nothing -> {
			clearFileContent();
			mFileName.setText(null);
			mFileDestination.setText(null);
			mFileSize.setText(null);
			mStatus.setText(null);
			mGenerateFileAction.setVisibility(View.VISIBLE);
			mSelectFileAction.setVisibility(View.VISIBLE);
			mUploadAction.setVisibility(View.VISIBLE);
			mUploadAction.setEnabled(false);
			mCancelAction.setVisibility(View.GONE);
			mPauseResumeAction.setVisibility(View.GONE);
		});
		mViewModel.getBusyState().observe(this, busy -> {
			mGenerateFileAction.setEnabled(!busy);
			mSelectFileAction.setEnabled(!busy);
			mUploadAction.setEnabled(isFileLoaded() && !busy);
		});

		// Configure GENERATE FILE action
		mGenerateFileAction.setOnClickListener(v -> {
			final DialogFragment dialog = GenerateFileDialogFragment.getInstance();
			dialog.show(getChildFragmentManager(), null);
		});

		// Configure SELECT FILE action
		mSelectFileAction.setOnClickListener(v -> selectFile("*/*"));

		// Restore UPLOAD action state after rotation
		mUploadAction.setEnabled(isFileLoaded());
		mUploadAction.setOnClickListener(v -> {
			final String fileName = mFileName.getText().toString();
			mFsUtils.addRecent(fileName);
			final String filePath = mFileDestination.getText().toString();
			mViewModel.upload(filePath, getFileContent());
		});

		// Cancel and Pause/Resume buttons
		mCancelAction.setOnClickListener(v -> mViewModel.cancel());
		mPauseResumeAction.setOnClickListener(v -> {
			if (mViewModel.getState().getValue() == FilesUploadViewModel.State.UPLOADING) {
				mViewModel.pause();
			} else {
				mViewModel.resume();
			}
		});
	}

	public void onGenerateFileRequested(final int fileSize) {
		onFileSelected("Lorem_" + fileSize + ".txt", fileSize);
		setFileContent(FsUtils.generateLoremIpsum(fileSize));
	}

	@Override
	protected void onFileCleared() {
		mUploadAction.setEnabled(false);
	}

	@Override
	protected void onFileSelected(@NonNull final String fileName, final int fileSize) {
		final String partition = mFsUtils.getPartitionString();
		mFileName.setText(fileName);
		mFileDestination.setText(getString(R.string.files_file_path, partition, fileName));
		mFileSize.setText(getString(R.string.files_upload_size_value, fileSize));
	}

	@Override
	protected void onFileLoaded(@NonNull final byte[] data) {
		mUploadAction.setEnabled(true);
		mStatus.setText(R.string.files_upload_status_ready);
	}

	@Override
	protected void onFileLoadingFailed(final int error) {
		mStatus.setText(error);
	}

	private void printError(@NonNull final String error) {
		final SpannableString spannable = new SpannableString(error);
		spannable.setSpan(new ForegroundColorSpan(
						ContextCompat.getColor(requireContext(), R.color.error)),
				0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		spannable.setSpan(new StyleSpan(Typeface.BOLD),
				0, error.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
		mStatus.setText(spannable);
	}
}
