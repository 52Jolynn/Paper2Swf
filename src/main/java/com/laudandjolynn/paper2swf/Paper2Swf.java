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

import java.io.File;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.laudandjolynn.paper2swf.utils.JacobNativesLoader;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午4:29:42
 * @copyright: www.laudandjolynn.com
 */
public class Paper2Swf {
	private final static Logger logger = LoggerFactory
			.getLogger(Paper2Swf.class);

	public enum ConvertTech {
		JACOB, OPEN_OFFICE
	}

	/**
	 * pdf to swf, sync
	 * 
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
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
	public static int pdf2Swf(String swftoolsFilePath, String languageDir,
			String pdfFilePath, String swfDir, String swfFileName,
			boolean paging) {
		SwfConverter converter = new SwfConverter(swftoolsFilePath, languageDir);
		return converter.pdf2Swf(pdfFilePath, swfDir, swfFileName, paging);
	}

	/**
	 * 使用jacob将office转成pdf, 然后再转swf, sync
	 * 
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @param swfDir
	 *            目的swf存储目录
	 * @param swfFileName
	 *            swf文件名
	 * @param pageing
	 *            是否分页
	 * @return 返回生成的swf的页数，-1表示转换失败
	 */
	public static int office2Swf_jacob(String swftoolsFilePath,
			String languageDir, String srcFilePath, String swfDir,
			String swfFileName, boolean paging) {
		PdfConverter pdfConverter = new JacobConverter();
		String tempDir = JacobNativesLoader.nativesDir.getAbsolutePath();
		String pdfFilePath = tempDir + File.separator
				+ UUID.randomUUID().toString() + ".pdf";
		File pdfFile = new File(pdfFilePath);
		try {
			pdfConverter.office2Pdf(srcFilePath, pdfFilePath);
			return pdf2Swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
					swfFileName, paging);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (pdfFile.exists()) {
				pdfFile.delete();
			}
		}
		return -1;
	}

	/**
	 * 使用openoffice将office转成pdf, 然后再转swf, sync
	 * 
	 * @param host
	 * @param port
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @param officeFilePath
	 *            office文件路径
	 * @param swfDir
	 *            目的swf存储目录
	 * @param swfFileName
	 *            swf文件名
	 * @param pageing
	 *            是否分页
	 * @return 返回生成的swf的页数，-1表示转换失败
	 */
	public static int office2Swf_openoffice(String host, int port,
			String swftoolsFilePath, String languageDir, String officeFilePath,
			String swfDir, String swfFileName, boolean paging) {
		PdfConverter pdfConverter = new OpenOfficeConverter(host, port);
		String tempDir = JacobNativesLoader.nativesDir.getAbsolutePath();
		String pdfFilePath = tempDir + File.separator
				+ UUID.randomUUID().toString() + ".pdf";
		File pdfFile = new File(pdfFilePath);
		try {
			pdfConverter.office2Pdf(officeFilePath, pdfFilePath);
			return pdf2Swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
					swfFileName, paging);
		} catch (Exception e) {
			logger.error(e.getMessage(), e);
		} finally {
			if (pdfFile.exists()) {
				pdfFile.delete();
			}
		}
		return -1;
	}
}
