package com.laudandjolynn.paper2swf.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.zip.CRC32;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午9:21:35
 * @copyright: www.laudandjolynn.com
 */
public class JacobNativesLoader {
	private final static Logger logger = LoggerFactory
			.getLogger(JacobNativesLoader.class);
	private static boolean nativesLoaded = false;
	public final static boolean isWindows = System.getProperty("os.name")
			.contains("Windows");
	public final static boolean isLinux = System.getProperty("os.name")
			.contains("Linux");
	public final static boolean isMac = System.getProperty("os.name").contains(
			"Mac");
	public final static boolean is64Bit = System.getProperty("os.arch").equals(
			"amd64");
	public final static File nativesDir = new File(
			System.getProperty("java.io.tmpdir") + "/paper2swf/"
					+ crc("jacob.dll"));

	private static String crc(String nativeFile) {
		InputStream input = JacobNativesLoader.class.getResourceAsStream("/"
				+ nativeFile);
		if (input == null)
			return JacobNativesLoader.class.getName(); // fallback
		CRC32 crc = new CRC32();
		byte[] buffer = new byte[4096];
		try {
			while (true) {
				int length = input.read(buffer);
				if (length == -1)
					break;
				crc.update(buffer, 0, length);
			}
		} catch (Exception ex) {
			try {
				input.close();
			} catch (Exception ignored) {
				logger.error("crc fail.", ignored);
			}
		}
		return Long.toString(crc.getValue());
	}

	private static boolean loadLibrary(String nativeFile32, String nativeFile64) {
		String path = extractLibrary(nativeFile32, nativeFile64);
		if (path != null) {
			System.load(path);
		}

		return path != null;
	}

	private static String extractLibrary(String native32, String native64) {
		String nativeFileName = is64Bit ? native64 : native32;
		File nativeFile = new File(nativesDir, nativeFileName);
		try {
			// Extract native from classpath to temp dir.
			InputStream input = JacobNativesLoader.class
					.getResourceAsStream("/" + nativeFileName);
			if (input == null) {
				return null;
			}
			nativesDir.mkdirs();
			FileOutputStream output = new FileOutputStream(nativeFile);
			byte[] buffer = new byte[4096];
			while (true) {
				int length = input.read(buffer);
				if (length == -1)
					break;
				output.write(buffer, 0, length);
			}
			input.close();
			output.close();
		} catch (IOException ex) {
			logger.error("extract library to temp file fail.", ex);
		}
		return nativeFile.exists() ? nativeFile.getAbsolutePath() : null;
	}

	/**
	 * Loads the jacob native libraries.
	 */
	public static void load() {
		if (nativesLoaded) {
			return;
		}

		if (isWindows) {
			nativesLoaded = loadLibrary("jacob_x86.dll", "jacob_x64.dll");
		}
		nativesLoaded = true;
	}
}
