package com.laudandjolynn.paper2swf.utils;

import org.apache.commons.pool.BasePoolableObjectFactory;

import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午8:28:33
 * @copyright: www.laudandjolynn.com
 */
public class SocketOpenOfficeConnectionFactory extends
		BasePoolableObjectFactory<SocketOpenOfficeConnection> {
	private String host = null;
	private int port;

	/**
	 * 构造函数
	 * 
	 * @param host
	 *            OpenOffice服务地址
	 * @param port
	 *            OpenOffice服务端口
	 */
	public SocketOpenOfficeConnectionFactory(String host, int port) {
		this.host = host;
		this.port = port;
	}

	@Override
	public SocketOpenOfficeConnection makeObject() throws Exception {
		return new SocketOpenOfficeConnection(host, port);
	}

	@Override
	public void destroyObject(SocketOpenOfficeConnection conn) throws Exception {
		conn.disconnect();
	}

	@Override
	public boolean validateObject(SocketOpenOfficeConnection conn) {
		return conn.isConnected();
	}
}
