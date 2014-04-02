package com.laudandjolynn.paper2swf;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午4:29:42
 * @copyright: www.laudandjolynn.com
 */
public class Paper2Swf {
	/**
	 * 
	 * @param swftoolsFilePath
	 * @param languageDir
	 * @param pdfFilePath
	 * @param swfDir
	 * @param swfFileName
	 * @param background
	 * @return
	 */
	public static int pdf2swf(String swftoolsFilePath, String languageDir,
			String pdfFilePath, String swfDir, String swfFileName,
			boolean background) {
		if (background) {
			return 0;
		} else {
			SwfConverter converter = new SwfConverter(swftoolsFilePath,
					languageDir);
			return converter.convertPdfToSwf(pdfFilePath, swfDir, swfFileName);
		}
	}

	/**
	 * 使用jacob将office转成pdf，然后再转swf
	 * 
	 * @param swftoolsFilePath
	 * @param languageDir
	 * @param srcFilePath
	 * @param pdfFilePath
	 * @param swfDir
	 * @param swfFileName
	 * @param background
	 * @return
	 */
	public static int office2swf_jacob(String swftoolsFilePath,
			String languageDir, String srcFilePath, String pdfFilePath,
			String swfDir, String swfFileName, boolean background) {
		PdfConverter pdfConverter = new JacobConverter();
		pdfConverter.convert(srcFilePath, pdfFilePath);
		return pdf2swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
				swfFileName, background);
	}

	/**
	 * 使用openoffice将office转成pdf，然后再转swf
	 * 
	 * @param host
	 * @param port
	 * @param swftoolsFilePath
	 * @param languageDir
	 * @param srcFilePath
	 * @param pdfFilePath
	 * @param swfDir
	 * @param swfFileName
	 * @param background
	 * @return
	 */
	public static int office2swf_openoffice(String host, int port,
			String swftoolsFilePath, String languageDir, String srcFilePath,
			String pdfFilePath, String swfDir, String swfFileName,
			boolean background) {
		PdfConverter pdfConverter = new OpenOfficeConverter(host, port);
		pdfConverter.convert(srcFilePath, pdfFilePath);
		return pdf2swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
				swfFileName, background);
	}
}
