package no.nordicsemi.android.mcumgr.sample.dialog;

import android.app.Dialog;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.DialogSelectImageBinding;

public class SelectImageDialogFragment extends DialogFragment {
	private static final String ARG_REQUEST_ID = "requestId";
	private int position = 0;

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
		binding.image.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.drop_down_item, items));
		binding.image.setText(items[0], false);
		binding.image.setOnItemClickListener((parent, view, position, id) -> this.position = position);

		return new MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.image_select_image)
				.setView(binding.getRoot())
				// Setting the positive button listener here would cause the dialog to dismiss.
				// We have to validate the value before.
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					final Bundle args = requireArguments();
					final int requestId = args.getInt(ARG_REQUEST_ID);

					final OnImageSelectedListener listener = (OnImageSelectedListener) getParentFragment();
					if (listener != null)
						listener.onImageSelected(requestId, position);
				})
				.setNegativeButton(android.R.string.cancel, null)
				.create();
	}
}
