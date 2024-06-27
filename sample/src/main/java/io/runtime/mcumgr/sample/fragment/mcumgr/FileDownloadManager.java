package io.runtime.mcumgr.sample.fragment.mcumgr;

import static android.os.Environment.DIRECTORY_DOWNLOADS;

import android.content.Context;
import android.os.Environment;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

public class FileDownloadManager {
    private final static Logger LOG = LoggerFactory.getLogger(FileDownloadManager.class);

    public boolean save(byte[] data, String fileName) {
        File file = new File(Environment.getExternalStoragePublicDirectory(DIRECTORY_DOWNLOADS), fileName);
        String path = file.getAbsolutePath();
        LOG.debug("Saving file to {}", path);

        try {
            FileOutputStream fos = new FileOutputStream(file);
            fos.write(data);
            fos.close();
            LOG.debug("Saved file to {}", path);
            return true;
        } catch (IOException e) {
            LOG.error(String.format("Failed to save file to %s; size=%d", path, data.length), e);
            return false;
        }
    }
}
