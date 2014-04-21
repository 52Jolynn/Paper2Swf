/*
 * Copyright (C) 2012 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

public class GearmanNIOJobServerConnectionFactory implements
		GearmanJobServerIpConnectionFactory {	

	public GearmanJobServerConnection createConnection(String host, int port) {
		return new GearmanNIOJobServerConnection(host,port);
	}

}
