package no.nordicsemi.android.mcumgr.sample.utils;

import androidx.annotation.Keep;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

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

import no.nordicsemi.android.mcumgr.dfu.mcuboot.model.ImageSet;
import no.nordicsemi.android.mcumgr.dfu.mcuboot.model.TargetImage;
import no.nordicsemi.android.mcumgr.dfu.suit.model.CacheImageSet;
import no.nordicsemi.android.mcumgr.exception.McuMgrException;
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
			 * The file type. Expected vales are: "application", "bin", "suit-envelope", "cache", "mcuboot".
			 */
			private String type;
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
			private int slot = -1; // If not set in manifest TargetImage.SLOT_SECONDARY will be used (see getBinaries(type) below).
			/**
			 * The target partition ID. This parameter is valid for files with type `cache`.
			 */
			private int partition = 0;
		}
	}

	private Manifest manifest;
	private final Map<String, byte[]> entries = new HashMap<>();

	public ZipPackage(@NonNull final byte[] data) throws IOException {
		ZipEntry ze;

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
			} else if (name.endsWith(".bin") || name.endsWith(".suit")) {
				final byte[] content = getData(zis);
				entries.put(name, content);
			} else {
				Timber.w("Unsupported file found: %s", name);
			}
		}
	}

	@NonNull
	public ImageSet getBinaries() throws IOException, McuMgrException {
		final ImageSet binaries = getBinaries(null);
		if (binaries == null)
			return new ImageSet();
		return binaries;
	}

	@Nullable
	public ImageSet getMcuBootBinaries() throws IOException, McuMgrException {
		return getBinaries("mcuboot");
	}

	@Nullable
	private ImageSet getBinaries(final @Nullable String type) throws IOException, McuMgrException {
		ImageSet binaries = null;

		// Search for images.
		int i = 0;
		for (final Manifest.File file: manifest.files) {
			if (type == null || type.equals(file.type)) {
				final String name = file.file;
				final byte[] content = entries.get(name);
				if (content == null)
					throw new IOException("File not found: " + name);

				if (binaries == null)
					binaries = new ImageSet();

				int slot = file.slot;
				// If slot wasn't set from json, set it to the default value.
				if (slot < 0) {
					if ("mcuboot".equals(file.type)) {
						// Before nRF Connect SDK 3.0 slot was not given for mcuboot updates.
						// Instead, slots were assigned in order of appearance in the manifest.
						slot = i++;
					} else {
						// By default, send the image to the secondary slot, even if they are
						// later swapped to the primary slot.
						slot = TargetImage.SLOT_SECONDARY;
					}
				}
				binaries.add(new TargetImage(file.imageIndex, slot, content));
			}
		}
		return binaries;
	}

	/**
	 * Returns the SUIT envelope.
	 * <p>
	 * This is valid only for SUIT updates using SUIT manager.
	 * @return The SUIT envelope, or null if not present in the ZIP.
	 */
	@Nullable
	public byte[] getSuitEnvelope() {
		// First, search for an entry of type "suit-envelope".
		for (final Manifest.File file: manifest.files) {
			if (file.type.equals("suit-envelope")) {
                return entries.get(file.file);
			}
		}
		// If not found, search for a file with the ".suit" extension.
		for (final Manifest.File file: manifest.files) {
			if (file.file.endsWith(".suit")) {
				return entries.get(file.file);
			}
		}
		// Not found.
		return null;
	}

	/**
	 * Raw cache images are sent to the device together with the SUIT envelope before starting the
	 * update process. The cache images are stored in the cache partitions.
	 *
	 * @return The cache images, or null if not present in the ZIP.
	 * @throws IOException if at least one of the cache images is missing.
	 */
	@Nullable
	public CacheImageSet getCacheBinaries() throws IOException {
		CacheImageSet cache = null;

		// Search for images.
		for (final Manifest.File file: manifest.files) {
			if (file.type.equals("cache")) {
				final String name = file.file;
				final byte[] content = entries.get(name);
				if (content == null)
					throw new IOException("File not found: " + name);

				if (cache == null)
					cache = new CacheImageSet();
				cache.add(file.partition, content);
			}
		}
		return cache;
	}

	public byte[] getResource(@NonNull final String name) {
		return entries.get(name);
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
