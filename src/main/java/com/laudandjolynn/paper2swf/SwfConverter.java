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
package com.laudandjolynn.paper2swf;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.log4j.Logger;

/**
 * swf转换器
 * 
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014-4-2
 * @copyright: www.laudandjolynn.com
 */
public class SwfConverter {
	private static final Logger log = Logger.getLogger(SwfConverter.class);
	private static final String SWF_TOOLS_RESPONSE_REGEX = "^NOTICE\\s+processing\\s+PDF\\s+page\\s+(\\d+)\\s+\\(.+\\)$";
	private String swftoolsFilePath = null;
	private String languageDir = null;

	/**
	 * 取得SWF转换器实例
	 * 
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @return
	 */
	public SwfConverter(String swftoolsFilePath, String languageDir) {
		if (!swftoolsFilePath.endsWith(File.separator)) {
			swftoolsFilePath += File.separator;
		}
		this.swftoolsFilePath = swftoolsFilePath;
		this.languageDir = languageDir;
	}

	/**
	 * PDF转swf
	 * 
	 * @param pdfFilePath
	 *            源PDF文件路径，包括文件名
	 * @param swfDir
	 *            目的swf存储目录
	 * @param swfFileName
	 *            swf文件名
	 * @param pageing
	 *            是否分页
	 * @return 返回生成的swf的页数，-1表示转换失败
	 */
	public int pdf2Swf(String pdfFilePath, String swfDir, String swfFileName,
			boolean paging) {
		File swftoolsFile = new File(this.swftoolsFilePath);
		File languageDir = new File(this.languageDir);
		File pdfFile = new File(pdfFilePath);

		if (!swftoolsFile.exists()) {
			log.error("can not find swftools.");
			return -1;
		}
		if (!languageDir.exists()) {
			log.error("can not find xpdf directory path.");
			return -1;
		}
		if (!pdfFile.exists()) {
			log.error("can not find pdf file.");
			return -1;
		}

		if (!swfDir.endsWith(File.separator)) {
			swfDir += File.separator;
		}
		File tgtFile = new File(swfDir);
		if (!tgtFile.exists()) {
			tgtFile.mkdirs();
		}

		int index = swfFileName.lastIndexOf(".");
		String absSwfToolsFilePath = swftoolsFile.getAbsolutePath()
				+ File.separator;
		String absLanguageDir = languageDir.getAbsolutePath();
		String absPdfFilePath = pdfFile.getAbsolutePath();
		String swfFileNameWithoutExt = swfFileName.substring(0, index);
		String absSwfDir = tgtFile.getAbsolutePath() + File.separator;

		String cmd = "\"" + absSwfToolsFilePath + "pdf2swf.exe\" \""
				+ absPdfFilePath + "\" -o \"" + absSwfDir
				+ swfFileNameWithoutExt + (paging ? "%" : "")
				+ ".swf\" -f -T 9 -t -s storeallcharacters -s languagedir=\""
				+ absLanguageDir + "\"";

		log.info("execute cmd: " + cmd);

		BufferedReader br = null;
		try {
			ProcessBuilder pb = new ProcessBuilder(cmd);
			pb.redirectErrorStream(true);
			Process process = pb.start();
			int page = -1;
			if (paging) {
				String line = null;
				Pattern pattern = Pattern.compile(SWF_TOOLS_RESPONSE_REGEX);
				Matcher matcher = null;

				br = new BufferedReader(new InputStreamReader(
						process.getInputStream()));
				while ((line = br.readLine()) != null) {
					matcher = pattern.matcher(line);
					if (matcher.matches()) {
						page = Integer.valueOf(matcher.group(1));
					}
				}
			} else {
				page = 1;
			}

			process.waitFor();
			log.info("create swf file successful.");
			return page;
		} catch (IOException e) {
			log.error(e.getMessage());
		} catch (InterruptedException e) {
			log.error(e.getMessage());
		} catch (NumberFormatException e) {
			log.error(e.getMessage());
		} finally {
			if (br != null) {
				try {
					br.close();
				} catch (IOException e) {
					log.error(e.getMessage());
				}
			}
		}
		return -1;
	}
}
