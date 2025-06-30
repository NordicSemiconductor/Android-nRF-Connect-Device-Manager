package io.runtime.mcumgr.sample.dialog;

import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.widget.ArrayAdapter;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import io.runtime.mcumgr.sample.R;
import io.runtime.mcumgr.sample.databinding.DialogSelectBinaryBinding;

public class SelectBinaryDialogFragment extends DialogFragment {
	private static final String ARG_REQUEST_ID = "requestId";
	private int position = 0;

	public interface OnBinarySelectedListener {
		void onBinarySelected(final int requestId, final int index);
		void onSelectingBinaryCancelled();
	}

	public static DialogFragment getInstance(final int requestId) {
		final DialogFragment fragment = new SelectBinaryDialogFragment();

		final Bundle args = new Bundle();
		args.putInt(ARG_REQUEST_ID, requestId);
		fragment.setArguments(args);

		return fragment;
	}

	@NonNull
	@Override
	public Dialog onCreateDialog(@Nullable final Bundle savedInstanceState) {
		final Bundle args = requireArguments();
		final int requestId = args.getInt(ARG_REQUEST_ID);
		final String[] slots = getResources().getStringArray(R.array.image_upgrade_slot);
		final DialogSelectBinaryBinding binding = DialogSelectBinaryBinding.inflate(getLayoutInflater());
		binding.image.setAdapter(new ArrayAdapter<>(requireContext(), R.layout.drop_down_item, slots));
		binding.image.setOnItemClickListener((parent, view, position, id) -> this.position = position);

		return new MaterialAlertDialogBuilder(requireContext())
				.setTitle(R.string.image_upgrade_slot)
				.setView(binding.getRoot())
				// Setting the positive button listener here would cause the dialog to dismiss.
				// We have to validate the value before.
				.setPositiveButton(android.R.string.ok, (dialog, which) -> {
					final OnBinarySelectedListener listener = (OnBinarySelectedListener) getParentFragment();
					if (listener != null)
						listener.onBinarySelected(requestId, position);
				})
				.setNegativeButton(android.R.string.cancel, (dialog, button) -> onCancel(dialog))
				.create();
	}

	@Override
	public void onCancel(@NonNull DialogInterface dialog) {
		super.onCancel(dialog);
		final OnBinarySelectedListener listener = (OnBinarySelectedListener) getParentFragment();
		if (listener != null)
			listener.onSelectingBinaryCancelled();
	}
}
