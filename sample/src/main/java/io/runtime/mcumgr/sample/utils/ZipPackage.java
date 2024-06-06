package io.runtime.mcumgr.sample.utils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;

import com.google.gson.FieldNamingPolicy;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import io.runtime.mcumgr.dfu.mcuboot.model.ImageSet;
import io.runtime.mcumgr.dfu.mcuboot.model.TargetImage;
import io.runtime.mcumgr.exception.McuMgrException;
import timber.log.Timber;

public final class ZipPackage {
	private static final String MANIFEST = "manifest.json";

	@SuppressWarnings({"unused", "MismatchedReadAndWriteOfArray"})
	@Keep
	private static class Manifest {
		private int formatVersion;
		private File[] files;

		@Keep
		private static class File {
			/**
			 * The version number of the image. This is a string in the format "X.Y.Z-text".
			 */
			private String version;
			/**
			 * The name of the image file.
			 */
			private String file;
			/**
			 * The size of the image file in bytes. This is declared size and does not have to
			 * be equal to the actual file size.
			 */
			private int size;
			/**
			 * Image index is used for multi-core devices. Index 0 is the main core (app core),
			 * index 1 is secondary core (net core), etc.
			 * <p>
			 * For single-core devices this is not present in the manifest file and defaults to 0.
			 */
			private int imageIndex = 0;
			/**
			 * The slot number where the image is to be sent. By default images are sent to the
			 * secondary slot and then swapped to the primary slot after the image is confirmed
			 * and the device is reset.
			 * <p>
			 * However, if the device supports Direct XIP feature it is possible to run an app
			 * from a secondary slot. The image has to be compiled for this slot. A ZIP package
			 * can contain images for both slots. Only the one targeting the available one will
			 * be sent.
			 * @since NCS v 2.5, nRF Connect Device Manager 1.8.
			 */
			private int slot = TargetImage.SLOT_SECONDARY;
		}
	}

	private Manifest manifest;
	private final ImageSet binaries;

	public ZipPackage(@NonNull final byte[] data) throws IOException, McuMgrException {
		ZipEntry ze;
		Map<String, byte[]> entries = new HashMap<>();

		// Unzip the file and look for the manifest.json.
		final ZipInputStream zis = new ZipInputStream(new ByteArrayInputStream(data));
		while ((ze = zis.getNextEntry()) != null) {
			if (ze.isDirectory())
				throw new IOException("Invalid ZIP");

			final String name = validateFilename(ze.getName(), ".");

			if (name.equals(MANIFEST)) {
				final Gson gson = new GsonBuilder()
						.setFieldNamingPolicy(FieldNamingPolicy.LOWER_CASE_WITH_UNDERSCORES)
						.create();
				manifest = gson.fromJson(new InputStreamReader(zis), Manifest.class);
			} else if (name.endsWith(".bin")) {
				final byte[] content = getData(zis);
				entries.put(name, content);
			} else {
				Timber.w("Unsupported file found: %s", name);
			}
		}

		binaries = new ImageSet();

		// Search for images.
		for (final Manifest.File file: manifest.files) {
			final String name = file.file;
			final byte[] content = entries.get(name);
			if (content == null)
				throw new IOException("File not found: " + name);

			binaries.add(new TargetImage(file.imageIndex, file.slot, content));
		}
	}

	public ImageSet getBinaries() {
		return binaries;
	}

	private byte[] getData(@NonNull ZipInputStream zis) throws IOException {
		final byte[] buffer = new byte[1024];

		// Read file content to byte array
		final ByteArrayOutputStream os = new ByteArrayOutputStream();
		int count;
		while ((count = zis.read(buffer)) != -1) {
			os.write(buffer, 0, count);
		}
		return os.toByteArray();
	}

	/**
	 * Validates the path (not the content) of the zip file to prevent path traversal issues.
	 *
	 * <p> When unzipping an archive, always validate the compressed files' paths and reject any path
	 * that has a path traversal (such as ../..). Simply looking for .. characters in the compressed
	 * file's path may not be enough to prevent path traversal issues. The code validates the name of
	 * the entry before extracting the entry. If the name is invalid, the entire extraction is aborted.
	 * <p>
	 *
	 * @param filename The path to the file.
	 * @param intendedDir The intended directory where the zip should be.
	 * @return The validated path to the file.
	 * @throws java.io.IOException Thrown in case of path traversal issues.
	 */
	@SuppressWarnings("SameParameterValue")
	private String validateFilename(@NonNull final String filename,
									@NonNull final String intendedDir)
			throws IOException {
		File f = new File(filename);
		String canonicalPath = f.getCanonicalPath();

		File iD = new File(intendedDir);
		String canonicalID = iD.getCanonicalPath();

		if (canonicalPath.startsWith(canonicalID)) {
			return canonicalPath.substring(1); // remove leading "/"
		} else {
			throw new IllegalStateException("File is outside extraction target directory.");
		}
	}

}
