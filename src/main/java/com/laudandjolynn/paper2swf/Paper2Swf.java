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

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午4:29:42
 * @copyright: www.laudandjolynn.com
 */
public class Paper2Swf {
	public enum ConvertTech {
		JACOB, OPEN_OFFICE
	}

	/**
	 * pdf to swf, sync
	 * 
	 * @param swftoolsFilePath
	 * @param languageDir
	 * @param pdfFilePath
	 * @param swfDir
	 * @param swfFileName
	 * @return
	 */
	public static int pdf2swf(String swftoolsFilePath, String languageDir,
			String pdfFilePath, String swfDir, String swfFileName) {
		SwfConverter converter = new SwfConverter(swftoolsFilePath, languageDir);
		return converter.convertPdf2Swf(pdfFilePath, swfDir, swfFileName);
	}

	/**
	 * 使用jacob将office转成pdf, 然后再转swf, sync
	 * 
	 * @param swftoolsFilePath
	 * @param languageDir
	 * @param srcFilePath
	 * @param pdfFilePath
	 * @param swfDir
	 * @param swfFileName
	 * @return
	 */
	public static int office2swf_jacob(String swftoolsFilePath,
			String languageDir, String srcFilePath, String pdfFilePath,
			String swfDir, String swfFileName) {
		PdfConverter pdfConverter = new JacobConverter();
		pdfConverter.convert(srcFilePath, pdfFilePath);
		return pdf2swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
				swfFileName);
	}

	/**
	 * 使用openoffice将office转成pdf, 然后再转swf, sync
	 * 
	 * @param host
	 * @param port
	 * @param swftoolsFilePath
	 * @param languageDir
	 * @param officeFilePath
	 * @param pdfFilePath
	 * @param swfDir
	 * @param swfFileName
	 * @return
	 */
	public static int office2swf_openoffice(String host, int port,
			String swftoolsFilePath, String languageDir, String officeFilePath,
			String pdfFilePath, String swfDir, String swfFileName) {
		PdfConverter pdfConverter = new OpenOfficeConverter(host, port);
		pdfConverter.convert(officeFilePath, pdfFilePath);
		return pdf2swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
				swfFileName);
	}
}
