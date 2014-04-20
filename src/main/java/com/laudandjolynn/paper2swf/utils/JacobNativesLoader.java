/*******************************************************************************
 * Copyright (c) 2014 htd0324@gmail.com.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v3.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/gpl.html
 * 
 * Contributors:
 *     htd0324@gmail.com - initial API and implementation
 ******************************************************************************/
package com.laudandjolynn.paper2swf.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jacob.com.LibraryLoader;

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
			System.getProperty("java.io.tmpdir") + File.separator + "paper2swf"
					+ File.separator);

	private static boolean loadLibrary(String nativeFile32, String nativeFile64) {
		String path = extractLibrary(nativeFile32, nativeFile64);
		System.setProperty(LibraryLoader.JACOB_DLL_PATH, path);
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
	}
}
