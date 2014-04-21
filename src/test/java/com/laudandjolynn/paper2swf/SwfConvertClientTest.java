package com.laudandjolynn.paper2swf;

import junit.framework.TestCase;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 上午10:34:39
 * @copyright: www.laudandjolynn.com
 */
public class SwfConvertClientTest extends TestCase {
	private final static String HOST = "192.168.1.241";
	private final static int PORT = 4730;

	public void testPdf2Swf() {
		SwfConvertClient client = new SwfConvertClient(HOST, PORT);
		String swftoolsFilePath = "D:\\SWFTools";
		String languageDir = "D:\\SWFTools\\xpdf";
		String pdfFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.pdf").getPath();
		String swfDir = Pdf2SwfTest.class.getResource("/").getPath();
		client.pdf2Swf(swftoolsFilePath, languageDir, pdfFilePath, swfDir,
				"TestConverter.swf", false);
		client.shutdown();
	}

	public void testOffice2Swf() {
		SwfConvertClient client = new SwfConvertClient(HOST, PORT);
		String swftoolsFilePath = "D:\\SWFTools";
		String languageDir = "D:\\SWFTools\\xpdf";
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.wps").getPath();
		String swfDir = Pdf2SwfTest.class.getResource("/").getPath();
		client.office2Swf_jacob(swftoolsFilePath, languageDir, srcFilePath,
				swfDir, "TestConverter.swf", false);
		client.shutdown();
	}
}
