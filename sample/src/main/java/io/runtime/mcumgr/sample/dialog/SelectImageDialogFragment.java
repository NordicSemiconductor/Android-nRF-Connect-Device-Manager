package io.runtime.mcumgr.sample.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.DialogFragment;
import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.DialogSelectImageBinding;

public class SelectImageDialogFragment extends DialogFragment {
	private static final String ARG_REQUEST_ID = "requestId";
	private int mPosition = 0;

	public interface OnImageSelectedListener {
		void onImageSelected(final int requestId, final int image);
	}

	public static DialogFragment getInstance(final int requestId) {
		final DialogFragment fragment = new SelectImageDialogFragment();

		final Bundle args = new Bundle();
		args.putInt(ARG_REQUEST_ID, requestId);
		fragment.setArguments(args);

		return fragment;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		final DialogSelectImageBinding binding = DialogSelectImageBinding.inflate(getLayoutInflater());
		final CharSequence[] items = getResources().getTextArray(R.array.image_select_images);
		binding.image.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.dialog_select_image_item, items));
		binding.image.setText(items[0], false);
		binding.image.setOnItemClickListener((parent, view, position, id) -> {
			mPosition = position;
		});

		return new AlertDialog.Builder(requireContext())
				.setTitle(R.string.image_select_image)
				.setView(binding.getRoot())
				// Setting the positive button listener here would cause the dialog to dismiss.
				// We have to validate the value before.
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					final Bundle args = requireArguments();
					final int requestId = args.getInt(ARG_REQUEST_ID);

					final OnImageSelectedListener listener = (OnImageSelectedListener) getParentFragment();
					if (listener != null)
						listener.onImageSelected(requestId, mPosition);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
	}
}
