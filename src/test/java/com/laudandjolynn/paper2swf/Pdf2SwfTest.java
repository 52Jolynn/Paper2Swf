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
 * @date: 2014年4月2日 下午4:48:08
 * @copyright: www.laudandjolynn.com
 */
public class Pdf2SwfTest extends TestCase {
	public void testPdf2Swf() {
		String swftoolsFilePath = "D:\\SWFTools";
		String languageDir = "D:\\SWFTools\\xpdf";
		SwfConverter converter = new SwfConverter(swftoolsFilePath, languageDir);
		String pdfFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.pdf").getPath();
		String swfDir = Pdf2SwfTest.class.getResource("/").getPath();
		assertEquals(1, converter.pdf2Swf(pdfFilePath, swfDir,
				"TestConverter.swf", false));
		assertEquals(2, converter.pdf2Swf(pdfFilePath, swfDir,
				"TestConverter.swf", true));
	}
}
