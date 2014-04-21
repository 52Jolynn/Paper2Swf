package com.laudandjolynn.paper2swf;

import java.io.IOException;

import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.client.GearmanJobStatus;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.laudandjolynn.paper2swf.utils.OpenOfficeConfig;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 下午1:13:30
 * @copyright: www.laudandjolynn.com
 */
public class PdfConvertClient {
	private final static Logger logger = LoggerFactory
			.getLogger(PdfConvertClient.class);
	private GearmanClient client;

	/**
	 * 
	 * @param conn
	 *            job server连接对象
	 */
	public PdfConvertClient(GearmanJobServerConnection conn) {
		client = new GearmanClientImpl();
		client.addJobServer(conn);
	}

	/**
	 * 
	 * @param host
	 *            job server服务地址
	 * @param port
	 *            job server服务端口
	 */
	public PdfConvertClient(String host, int port) {
		this(new GearmanNIOJobServerConnection(host, port));
	}

	/**
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 */
	public void office2Pdf_jacob(String srcFilePath, String destFilePath) {
		office2Pdf(srcFilePath, destFilePath, null);
	}

	/**
	 * @param host
	 *            openoffice服务地址
	 * @param port
	 *            openoffice服务端口
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 */
	public void office2Pdf_openoffice(String host, int port,
			String srcFilePath, String destFilePath) {
		office2Pdf(srcFilePath, destFilePath, new OpenOfficeConfig(host, port));
	}

	/**
	 * 
	 * @param type
	 *            转换方式
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @param cfg
	 *            OpenOffice配置
	 */
	private void office2Pdf(String srcFilePath, String destFilePath,
			OpenOfficeConfig cfg) {
		byte[] srcFilePathData = ByteUtils.toUTF8Bytes(srcFilePath);
		int srcFilePathDataLen = srcFilePathData.length;
		byte[] destFilePathData = ByteUtils.toUTF8Bytes(destFilePath);
		int destFilePathDataLen = destFilePathData.length;

		int totalBytes = srcFilePathDataLen + destFilePathDataLen + 1;
		byte[] data = new byte[totalBytes];
		int offset = 0;

		// src file path
		System.arraycopy(srcFilePathData, 0, data, offset, srcFilePathDataLen);
		offset += srcFilePathDataLen;
		data[offset++] = '\0';

		// dest file path
		System.arraycopy(destFilePathData, 0, data, offset, destFilePathDataLen);

		if (cfg != null) {// open office
			byte[] hostData = ByteUtils.toUTF8Bytes(cfg.getHost());
			int hostDataLen = hostData.length;
			byte[] portData = ByteUtils.toBigEndian(cfg.getPort());
			int portDataLen = portData.length;

			byte[] oo = new byte[totalBytes + hostDataLen + portDataLen + 2];
			offset += destFilePathDataLen;
			data[offset++] = '\0';

			System.arraycopy(data, 0, oo, 0, data.length);

			// host
			System.arraycopy(hostData, 0, oo, offset, hostDataLen);
			offset += hostDataLen;
			data[offset++] = '\0';

			// port
			System.arraycopy(portData, 0, oo, offset, portDataLen);

			data = oo;
		}

		String function = Office2PdfConvertFunction.class.getCanonicalName();
		String uniqueId = null;
		GearmanJob job = GearmanJobImpl.createBackgroundJob(function, data,
				uniqueId);
		client.submit(job);

		try {
			GearmanJobStatus status = client.getJobStatus(job);
			logger.info("job status: " + status);
		} catch (IllegalStateException e) {
			logger.error(e.getMessage(), e);
		} catch (IOException e) {
			logger.error(e.getMessage(), e);
		}
	}
}
