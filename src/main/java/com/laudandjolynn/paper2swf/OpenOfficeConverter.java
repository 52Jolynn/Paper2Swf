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
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午8:47:27
 * @copyright: www.laudandjolynn.com
 */
public class OpenOfficeConverter implements PdfConverter {
	private final static Logger logger = LoggerFactory
			.getLogger(OpenOfficeConverter.class);
	private String host;
	private int port;
	private GenericObjectPool<SocketOpenOfficeConnection> gop = null;

	public OpenOfficeConverter() {
		this(SocketOpenOfficeConnection.DEFAULT_HOST,
				SocketOpenOfficeConnection.DEFAULT_PORT);
	}

	public OpenOfficeConverter(String host, int port) {
		this.host = host;
		this.port = port;
		SocketOpenOfficeConnectionFactory factory = new SocketOpenOfficeConnectionFactory(
				host, port);
		this.gop = new GenericObjectPool<SocketOpenOfficeConnection>(factory);
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	@Override
	public int convert(String srcFilePath, String destFilePath) {
		SocketOpenOfficeConnection conn = null;
		try {
			conn = gop.borrowObject();
			File srcFile = new File(srcFilePath);
			File tgtFile = new File(destFilePath);
			DefaultDocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
			DocumentFormat pdf = formatRegistry.getFormatByFileExtension("pdf");
			conn.connect();
			DocumentConverter converter = new OpenOfficeDocumentConverter(conn);
			converter.convert(srcFile, tgtFile, pdf);
			return 1;
		} catch (Exception e) {
			logger.error("调用OpenOffice转换失败.", e);
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
