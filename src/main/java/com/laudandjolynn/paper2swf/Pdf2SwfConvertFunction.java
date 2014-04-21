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

import org.gearman.client.GearmanJobResult;
import org.gearman.util.ByteUtils;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午10:34:00
 * @copyright: www.laudandjolynn.com
 */
public class Pdf2SwfConvertFunction extends AbstractGearmanFunction {
	private final static Logger logger = LoggerFactory
			.getLogger(Pdf2SwfConvertFunction.class);

	@Override
	public GearmanJobResult executeFunction() {
		String data = ByteUtils.fromUTF8Bytes((byte[]) this.data);
		String[] params = data.split("\0");

		int index = 0;
		String swftoolsFilePath = params[index++];
		String languageDir = params[index++];
		String pdfFilePath = params[index++];
		String swfDir = params[index++];
		String swfFileName = params[index++];
		int paging = ByteUtils.fromBigEndian(params[index].getBytes());

		int r = Paper2Swf.pdf2swf(swftoolsFilePath, languageDir, pdfFilePath,
				swfDir, swfFileName, paging == 1 ? true : false);
		if (r == -1) {
			logger.error("convert pdf to swf fail.");
		}
		return null;
	}

}
