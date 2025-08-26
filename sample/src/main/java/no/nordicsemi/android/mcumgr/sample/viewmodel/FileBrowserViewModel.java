package no.nordicsemi.android.mcumgr.sample.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import javax.inject.Inject;

public class FileBrowserViewModel extends ViewModel {
	private final MutableLiveData<byte[]> fileContent = new MutableLiveData<>();

	@Inject
	public FileBrowserViewModel() {
		// Empty
	}

	public void setFileContent(final byte[] fileContent) {
		this.fileContent.postValue(fileContent);
	}

	public LiveData<byte[]> getFileContent() {
		return fileContent;
	}

	public boolean isFileLoaded() {
		return fileContent.getValue() != null;
	}
}
