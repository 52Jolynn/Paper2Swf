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
		PdfConverter converter = new JacobConverter();
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.wps").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath();
		assertEquals(1, converter.convert(srcFilePath, destFilePath));
	}

	public void testDoc2Pdf() {
		PdfConverter converter = new JacobConverter();
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.doc").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath();
		assertEquals(1, converter.convert(srcFilePath, destFilePath));
	}

	public void testOpenOffice2Pdf() {
		PdfConverter converter = new OpenOfficeConverter();
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.doc").getPath();
		String destFilePath = Pdf2SwfTest.class.getResource("/").getPath();
		assertEquals(1, converter.convert(srcFilePath, destFilePath));
	}
}
