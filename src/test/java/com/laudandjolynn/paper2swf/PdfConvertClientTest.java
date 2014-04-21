package com.laudandjolynn.paper2swf;

import junit.framework.TestCase;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 下午1:30:35
 * @copyright: www.laudandjolynn.com
 */
public class PdfConvertClientTest extends TestCase {
	private final static String HOST = "192.168.1.241";
	private final static int PORT = 4730;

	public void testOffice2Pdf() {
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.wps").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterWps.pdf";
		PdfConvertClient client = new PdfConvertClient(HOST, PORT);
		client.office2Pdf_jacob(srcFilePath, destFilePath);
	}
}
