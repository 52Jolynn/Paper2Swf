/*
 * Copyright (C) 2012 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

/**
 * This interface exposes the API that classes representing IP connections to a
 * Gearman Job Server must implement.
 */
public interface GearmanJobServerIpConnection extends
		GearmanJobServerConnection {
	
	/**
	 * 
	 * @return The host-name or address of the Gearman Job Server 
	 */
	public String getHost();
	
	/**
	 * 
	 * @return The port used to connect to the Gearman Job Server 
	 */
	public int getPort();

}
