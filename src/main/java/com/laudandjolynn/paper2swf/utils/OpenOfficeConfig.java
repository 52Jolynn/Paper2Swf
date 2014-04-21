package com.laudandjolynn.paper2swf.utils;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 下午1:24:16
 * @copyright: www.laudandjolynn.com
 */
public class OpenOfficeConfig {
	private String host;
	private int port;

	public OpenOfficeConfig(String host, int port) {
		this.host = host;
		this.port = port;
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

}
