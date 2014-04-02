package com.laudandjolynn.paper2swf;

import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午10:56:19
 * @copyright: www.laudandjolynn.com
 */
public class SwfConvertClient {
	private GearmanClient client;

	public SwfConvertClient(GearmanJobServerConnection conn) {
		client = new GearmanClientImpl();
		client.addJobServer(conn);
	}

	public SwfConvertClient(String host, int port) {
		this(new GearmanNIOJobServerConnection(host, port));
	}

	public void convert(String pdfFilePath, String swfDir, String swfFileName) {
		String function = PdfConvertFunction.class.getCanonicalName();
		String uniqueId = null;

		byte[] data = ByteUtils.toUTF8Bytes("");
		GearmanJob job = GearmanJobImpl.createBackgroundJob(function, data,
				uniqueId);
		client.submit(job);
	}

	public void shutdown() throws IllegalStateException {
		if (client == null) {
			throw new IllegalStateException("No client to shutdown");
		}
		client.shutdown();
	}
}
