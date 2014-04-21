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

import java.io.File;

import org.apache.commons.pool.impl.GenericObjectPool;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import com.laudandjolynn.paper2swf.utils.SocketOpenOfficeConnectionFactory;

/**
 * open office转换器
 * 
 * <pre>
 * OpenOffice服务启动方法：
 *  soffice -headless -accept="socket,host=127.0.0.1,port=8100;urp;" -nofirststartwizard
 *  参考资料：http://www.artofsolving.com/node/10
 * </pre>
 * 
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午8:47:27
 * @copyright: www.laudandjolynn.com
 */
public class OpenOfficeConverter implements PdfConverter {
	private final static Logger logger = LoggerFactory
			.getLogger(OpenOfficeConverter.class);
	private GenericObjectPool<SocketOpenOfficeConnection> gop = null;

	/**
	 * 默认构造函数，localhost, default port
	 */
	public OpenOfficeConverter() {
		this(SocketOpenOfficeConnection.DEFAULT_HOST,
				SocketOpenOfficeConnection.DEFAULT_PORT);
	}

	/**
	 * 
	 * @param host
	 *            openoffice服务地址
	 * @param port
	 *            openoffice服务端口
	 */
	public OpenOfficeConverter(String host, int port) {
		SocketOpenOfficeConnectionFactory factory = new SocketOpenOfficeConnectionFactory(
				host, port);
		this.gop = new GenericObjectPool<SocketOpenOfficeConnection>(factory);
	}

	@Override
	public int office2Pdf(String srcFilePath, String destFilePath) {
		SocketOpenOfficeConnection conn = null;
		try {
			conn = gop.borrowObject();
			File srcFile = new File(srcFilePath);
			logger.info("open document with OpenOffice "
					+ srcFile.getAbsolutePath());

			File tgtFile = new File(destFilePath);
			logger.info("convert pdf document to " + tgtFile.getAbsolutePath());
			if (tgtFile.isDirectory()) {
				logger.error("destination path must be a file path.");
				return 0;
			}
			if (tgtFile.exists()) {
				tgtFile.delete();
			}
			DefaultDocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
			DocumentFormat pdf = formatRegistry.getFormatByFileExtension("pdf");
			conn.connect();
			DocumentConverter converter = new OpenOfficeDocumentConverter(conn);
			converter.convert(srcFile, tgtFile, pdf);
			return 1;
		} catch (Exception e) {
			logger.error("call OpenOffice fail.", e);
		} finally {
			if (conn != null) {
				try {
					gop.returnObject(conn);
				} catch (Exception e) {
					logger.error(e.getMessage(), e);
				}
			}
		}
		return 0;
	}

}
