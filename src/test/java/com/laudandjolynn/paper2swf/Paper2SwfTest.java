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
 * @date: 2014年4月2日 下午4:29:28
 * @copyright: www.laudandjolynn.com
 */
public class Paper2SwfTest extends TestCase {
	public void testOffice2Swf() {
		String swftoolsFilePath = "D:\\SWFTools";
		String languageDir = "D:\\SWFTools\\xpdf";
		String swfDir = Pdf2SwfTest.class.getResource("/").getPath();
		String srcFilePath = Pdf2SwfTest.class
				.getResource("/TestConverter.wps").getPath();
		assertEquals(1, Paper2Swf.office2Swf_jacob(swftoolsFilePath,
				languageDir, srcFilePath, swfDir, "TestConverter.pdf", false));
	}
}
