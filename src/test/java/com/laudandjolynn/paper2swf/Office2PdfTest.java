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

import junit.framework.TestCase;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午4:47:59
 * @copyright: www.laudandjolynn.com
 */
public class Office2PdfTest extends TestCase {
	public void testWps2Pdf() {
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.wps").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterWps.pdf";

		// jacob
		PdfConverter converter = new JacobConverter();
		assertEquals(1, converter.office2Pdf(srcFilePath, destFilePath));
	}

	public void testEt2Pdf() {
		String srcFilePath = Pdf2SwfTest.class.getResource("/TestConverter.et")
				.getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterEt.pdf";

		// jacob
		PdfConverter converter = new JacobConverter();
		assertEquals(1, converter.office2Pdf(srcFilePath, destFilePath));
	}

	public void testDps2Pdf() {
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.dps").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterDps.pdf";

		// jacob
		PdfConverter converter = new JacobConverter();
		assertEquals(1, converter.office2Pdf(srcFilePath, destFilePath));
	}

	public void testDoc2Pdf() {
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.doc").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterDoc.pdf";
		// jacob
		PdfConverter c1 = new JacobConverter();
		assertEquals(1, c1.office2Pdf(srcFilePath, destFilePath));

		// openoffice
		PdfConverter c2 = new OpenOfficeConverter();
		assertEquals(1, c2.office2Pdf(srcFilePath, destFilePath));
	}

	public void testXls2Pdf() {
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.xls").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterXls.pdf";

		// jacob
		PdfConverter c1 = new JacobConverter();
		assertEquals(1, c1.office2Pdf(srcFilePath, destFilePath));

		// openoffice
		PdfConverter c2 = new OpenOfficeConverter();
		assertEquals(1, c2.office2Pdf(srcFilePath, destFilePath));
	}

	public void testPpt2Pdf() {
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.ppt").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath()
				+ "TestConverterPpt.pdf";

		// jacob
		PdfConverter c1 = new JacobConverter();
		assertEquals(1, c1.office2Pdf(srcFilePath, destFilePath));

		// openoffice
		PdfConverter c2 = new OpenOfficeConverter();
		assertEquals(1, c2.office2Pdf(srcFilePath, destFilePath));
	}
}
