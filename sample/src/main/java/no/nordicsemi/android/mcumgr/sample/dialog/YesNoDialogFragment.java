package no.nordicsemi.android.mcumgr.sample.dialog;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.StringRes;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class YesNoDialogFragment extends DialogFragment {
	private static final String ARG_REQUEST_ID = "requestId";
	private static final String ARG_TITLE_ID = "titleId";
	private static final String ARG_QUESTION_ID = "questionId";

	public interface Listener {
		void onAnswer(final int requestId, final boolean yes);
	}

	public static DialogFragment getInstance(final int requestId,
											 @StringRes final int titleId, @StringRes final int questionId) {
		final DialogFragment fragment = new YesNoDialogFragment();

		final Bundle args = new Bundle();
		args.putInt(ARG_REQUEST_ID, requestId);
		args.putInt(ARG_TITLE_ID, titleId);
		args.putInt(ARG_QUESTION_ID, questionId);
		fragment.setArguments(args);

		return fragment;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		final Bundle args = requireArguments();
		final int requestId = args.getInt(ARG_REQUEST_ID);

		return new MaterialAlertDialogBuilder(requireContext())
				.setTitle(args.getInt(ARG_TITLE_ID))
				.setMessage(args.getInt(ARG_QUESTION_ID))
				.setPositiveButton(android.R.string.yes, (dialog, which) -> {
					final Listener listener = (Listener) getParentFragment();
					if (listener != null)
						listener.onAnswer(requestId, true);
				})
				.setNegativeButton(android.R.string.cancel, (dialog, which) -> {
					final Listener listener = (Listener) getParentFragment();
					if (listener != null)
						listener.onAnswer(requestId, false);
				})
				.create();
	}
}
