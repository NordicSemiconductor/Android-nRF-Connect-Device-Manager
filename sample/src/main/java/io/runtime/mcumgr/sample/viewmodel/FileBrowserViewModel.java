package io.runtime.mcumgr.sample.viewmodel;

import javax.inject.Inject;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class FileBrowserViewModel extends ViewModel {
	private final MutableLiveData<byte[]> fileContent = new MutableLiveData<>();

	@Inject
	public FileBrowserViewModel() {
		// Empty
	}

	public void setFileContent(final byte[] fileContent) {
		this.fileContent.setValue(fileContent);
	}

	public LiveData<byte[]> getFileContent() {
		return fileContent;
	}

	public boolean isFileLoaded() {
		return fileContent.getValue() != null;
	}
}
