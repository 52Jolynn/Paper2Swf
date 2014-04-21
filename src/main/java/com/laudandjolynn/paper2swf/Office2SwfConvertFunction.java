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
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.util.ByteUtils;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.laudandjolynn.paper2swf.utils.ConvertException;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月3日 上午11:07:55
 * @copyright: www.laudandjolynn.com
 */
public class Office2SwfConvertFunction extends AbstractGearmanFunction {
	private final static Logger logger = LoggerFactory
			.getLogger(Office2SwfConvertFunction.class);

	@Override
	public GearmanJobResult executeFunction() {
		String data = ByteUtils.fromUTF8Bytes((byte[]) this.data);
		String[] params = data.split("\0");

		int index = 0;
		String swftoolsFilePath = params[index++];
		String languageDir = params[index++];
		String officeFilePath = params[index++];
		String swfDir = params[index++];
		String swfFileName = params[index++];
		boolean paging = params[index++].equals("true") ? true : false;

		int r = -1;
		if (params.length > index) {
			String host = params[index++];
			int port = ByteUtils.fromBigEndian(ByteUtils
					.toUTF8Bytes(params[index]));
			r = Paper2Swf.office2Swf_openoffice(host, port, swftoolsFilePath,
					languageDir, officeFilePath, swfDir, swfFileName, paging);
		} else {
			r = Paper2Swf.office2Swf_jacob(swftoolsFilePath, languageDir,
					officeFilePath, swfDir, swfFileName, paging);
		}
		if (r == -1) {
			logger.error("convert office to swf fail.");
			throw new ConvertException("convert office to swf fail");
		}
		GearmanJobResult result = new GearmanJobResultImpl(
				ByteUtils.toBigEndian(r));
		return result;
	}
}
