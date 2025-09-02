package no.nordicsemi.android.mcumgr.sample.fragment.mcumgr;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import javax.inject.Inject;

import no.nordicsemi.android.mcumgr.sample.R;
import no.nordicsemi.android.mcumgr.sample.databinding.FragmentCardObservabilityBinding;
import no.nordicsemi.android.mcumgr.sample.di.Injectable;
import no.nordicsemi.android.mcumgr.sample.dialog.HelpDialogFragment;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.McuMgrViewModelFactory;
import no.nordicsemi.android.mcumgr.sample.viewmodel.mcumgr.ObservabilityViewModel;
import no.nordicsemi.android.observability.bluetooth.DeviceState;
import no.nordicsemi.android.observability.internet.ChunkManager;

public class ObservabilityFragment extends Fragment implements Injectable {

    @Inject
    McuMgrViewModelFactory viewModelFactory;

    FragmentCardObservabilityBinding binding;

    ObservabilityViewModel viewModel;

    @Override
    public void onCreate(@Nullable final Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        viewModel = new ViewModelProvider(this, viewModelFactory)
                .get(ObservabilityViewModel.class);
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull final LayoutInflater inflater,
                             @Nullable final ViewGroup container,
                             @Nullable final Bundle savedInstanceState) {
        binding = FragmentCardObservabilityBinding.inflate(inflater, container, false);
        binding.toolbar.inflateMenu(R.menu.help);
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_help) {
                final DialogFragment dialog = HelpDialogFragment.getInstance(
                        R.string.observability_dialog_help_title,
                        R.string.observability_dialog_help_message,
                        "https://mflt.io/nrf-app-discover-cloud-services");
                dialog.show(getChildFragmentManager(), null);
                return true;
            }
            return false;
        });
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        viewModel.getChunksState().observe(getViewLifecycleOwner(), state -> {
            if (state != null) {
                switch (state.getDeviceStatus()) {
                    case DeviceState.Disconnected disconnected -> {
                        switch (disconnected.getReason()) {
                            case NOT_SUPPORTED ->
                                    binding.mds.setText(R.string.observability_not_supported);
                            case null ->
                                    binding.mds.setText(R.string.status_unknown);
                            default ->
                                    binding.mds.setText(R.string.observability_disconnected);
                        }
                    }
                    case DeviceState.Connecting ignored ->
                            binding.mds.setText(R.string.observability_connecting);
                    case DeviceState.Initializing ignored ->
                            binding.mds.setText(R.string.observability_connecting);
                    case DeviceState.Connected ignored1 -> {
                        switch (state.getUploadingStatus()) {
                            case ChunkManager.Status.Idle ignored -> {
                                binding.mds.setText(R.string.observability_connected);
                                binding.mdsSent.setText(getResources().getQuantityString(R.plurals.observability_value, state.getChunksUploaded(), state.getChunksUploaded(), state.getBytesUploaded()));
                                binding.mdsPending.setText(getResources().getQuantityString(R.plurals.observability_value, state.getPendingChunks(), state.getPendingChunks(), state.getPendingBytes()));
                            }
                            case ChunkManager.Status.InProgress ignored -> {
                                binding.mds.setText(R.string.observability_uploading);
                                binding.mdsSent.setText(getResources().getQuantityString(R.plurals.observability_value, state.getChunksUploaded(), state.getChunksUploaded(), state.getBytesUploaded()));
                                binding.mdsPending.setText(getResources().getQuantityString(R.plurals.observability_value, state.getPendingChunks(), state.getPendingChunks(), state.getPendingBytes()));
                            }
                            case ChunkManager.Status.Suspended suspended ->{
                                binding.mds.setText(getString(R.string.observability_suspended, suspended.getDelayInSeconds()));
                                binding.mdsSent.setText(getResources().getQuantityString(R.plurals.observability_value, state.getChunksUploaded(), state.getChunksUploaded(), state.getBytesUploaded()));
                                binding.mdsPending.setText(getResources().getQuantityString(R.plurals.observability_value, state.getPendingChunks(), state.getPendingChunks(), state.getPendingBytes()));
                            }
                            default -> {
                                binding.mds.setText(R.string.status_unknown);
                                binding.mdsSent.setText(R.string.observability_not_applicable);
                                binding.mdsPending.setText(R.string.observability_not_applicable);
                            }
                        }
                    }
                    default -> {}
                }
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
