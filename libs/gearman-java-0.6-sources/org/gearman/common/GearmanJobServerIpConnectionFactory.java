/*
 * Copyright (C) 2012 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

/**
 * Factory used to create connections to a target Gearman Job Server
 *
 */
public interface GearmanJobServerIpConnectionFactory {
	
	/**
	 * Creates a new connection handle for a specific Gearman Job Server
	 * 
	 * @return
	 */
	public GearmanJobServerConnection createConnection(String host, int port);
	
}
