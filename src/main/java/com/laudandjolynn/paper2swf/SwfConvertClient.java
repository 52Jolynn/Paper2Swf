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
 * @date: 2014年4月2日 下午10:56:19
 * @copyright: www.laudandjolynn.com
 */
public class SwfConvertClient {
	private final static Logger logger = LoggerFactory
			.getLogger(SwfConvertClient.class);
	private GearmanClient client;

	/**
	 * 
	 * @param conn
	 *            job server连接对象
	 */
	public SwfConvertClient(GearmanJobServerConnection conn) {
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
	public SwfConvertClient(String host, int port) {
		this(new GearmanNIOJobServerConnection(host, port));
	}

	/**
	 * 
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @param pdfFilePath
	 *            PDF文件路径
	 * @param swfDir
	 *            swf文件存储目录
	 * @param swfFileName
	 *            swf文件名
	 * @param paging
	 *            是否分页
	 */
	public void pdf2Swf(String swftoolsFilePath, String languageDir,
			String pdfFilePath, String swfDir, String swfFileName,
			boolean paging) {
		byte[] swftoolsFilePathData = ByteUtils.toUTF8Bytes(swftoolsFilePath);
		int swftoolsFilePathDataLen = swftoolsFilePathData.length;
		byte[] languageDirData = ByteUtils.toUTF8Bytes(languageDir);
		int languageDirDataLen = languageDirData.length;
		byte[] pdfFilePathData = ByteUtils.toUTF8Bytes(pdfFilePath);
		int pdfFilePathDataLen = pdfFilePathData.length;
		byte[] swfDirData = ByteUtils.toUTF8Bytes(swfDir);
		int swfDirDataLen = swfDirData.length;
		byte[] swfFileNameData = ByteUtils.toUTF8Bytes(swfFileName);
		int swfFileNameDataLen = swfFileNameData.length;
		byte[] pagingData = paging ? "true".getBytes() : "false".getBytes();
		int pagingDataLen = pagingData.length;

		int totalBytes = swftoolsFilePathDataLen + languageDirDataLen
				+ pdfFilePathDataLen + swfDirDataLen + swfFileNameDataLen
				+ pagingDataLen + 5;
		byte[] data = new byte[totalBytes];
		int offset = 0;
		// swf tools file path
		System.arraycopy(swftoolsFilePathData, 0, data, offset,
				swftoolsFilePathDataLen);
		offset += swftoolsFilePathDataLen;
		data[offset++] = '\0';

		// language dir
		System.arraycopy(languageDirData, 0, data, offset, languageDirDataLen);
		offset += languageDirDataLen;
		data[offset++] = '\0';

		// pdf file path
		System.arraycopy(pdfFilePathData, 0, data, offset, pdfFilePathDataLen);
		offset += pdfFilePathDataLen;
		data[offset++] = '\0';

		// swf dir
		System.arraycopy(swfDirData, 0, data, offset, swfDirDataLen);
		offset += swfDirDataLen;
		data[offset++] = '\0';

		// swf file name
		System.arraycopy(swfFileNameData, 0, data, offset, swfFileNameDataLen);
		offset += swfFileNameDataLen;
		data[offset++] = '\0';

		// paging
		System.arraycopy(pagingData, 0, data, offset, pagingDataLen);

		String function = Pdf2SwfConvertFunction.class.getCanonicalName();
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

	private void office2Swf(String swftoolsFilePath, String languageDir,
			String srcFilePath, String swfDir, String swfFileName,
			boolean paging, OpenOfficeConfig cfg) {
		byte[] swftoolsFilePathData = ByteUtils.toUTF8Bytes(swftoolsFilePath);
		int swftoolsFilePathDataLen = swftoolsFilePathData.length;
		byte[] languageDirData = ByteUtils.toUTF8Bytes(languageDir);
		int languageDirDataLen = languageDirData.length;
		byte[] officeFilePathData = ByteUtils.toUTF8Bytes(srcFilePath);
		int officeFilePathDataLen = officeFilePathData.length;
		byte[] swfDirData = ByteUtils.toUTF8Bytes(swfDir);
		int swfDirDataLen = swfDirData.length;
		byte[] swfFileNameData = ByteUtils.toUTF8Bytes(swfFileName);
		int swfFileNameDataLen = swfFileNameData.length;
		byte[] pagingData = paging ? "true".getBytes() : "false".getBytes();
		int pagingDataLen = pagingData.length;

		int totalBytes = swftoolsFilePathDataLen + languageDirDataLen
				+ officeFilePathDataLen + swfDirDataLen + swfFileNameDataLen
				+ pagingDataLen + 5;
		byte[] data = new byte[totalBytes];
		int offset = 0;
		// swf tools file path
		System.arraycopy(swftoolsFilePathData, 0, data, offset,
				swftoolsFilePathDataLen);
		offset += swftoolsFilePathDataLen;
		data[offset++] = '\0';

		// language dir
		System.arraycopy(languageDirData, 0, data, offset, languageDirDataLen);
		offset += languageDirDataLen;
		data[offset++] = '\0';

		// office file path
		System.arraycopy(officeFilePathData, 0, data, offset,
				officeFilePathDataLen);
		offset += officeFilePathDataLen;
		data[offset++] = '\0';

		// swf dir
		System.arraycopy(swfDirData, 0, data, offset, swfDirDataLen);
		offset += swfDirDataLen;
		data[offset++] = '\0';

		// swf file name
		System.arraycopy(swfFileNameData, 0, data, offset, swfFileNameDataLen);
		offset += swfFileNameDataLen;
		data[offset++] = '\0';

		// paging
		System.arraycopy(pagingData, 0, data, offset, pagingDataLen);

		if (cfg != null) {// open office
			byte[] hostData = ByteUtils.toUTF8Bytes(cfg.getHost());
			int hostDataLen = hostData.length;
			byte[] portData = ByteUtils.toBigEndian(cfg.getPort());
			int portDataLen = portData.length;

			byte[] oo = new byte[totalBytes + hostDataLen + portDataLen + 2];
			offset += pagingDataLen;
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

		String function = Office2SwfConvertFunction.class.getCanonicalName();
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

	/**
	 * 
	 * @param host
	 *            openoffice服务地址
	 * @param port
	 *            openoffice服务端口
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @param officeFilePath
	 *            office文件路径
	 * @param swfDir
	 *            swf文件存储目录
	 * @param swfFileName
	 *            swf文件名
	 * @param paging
	 *            是否分页
	 */
	public void office2Swf_openoffice(String host, int port,
			String swftoolsFilePath, String languageDir, String officeFilePath,
			String swfDir, String swfFileName, boolean paging) {
		office2Swf(swftoolsFilePath, languageDir, officeFilePath, swfDir,
				swfFileName, paging, new OpenOfficeConfig(host, port));
	}

	/**
	 * 
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @param officeFilePath
	 *            office文件路径
	 * @param swfDir
	 *            swf文件存储目录
	 * @param swfFileName
	 *            swf文件名
	 * @param paging
	 *            是否分页
	 */
	public void office2Swf_jacob(String swftoolsFilePath, String languageDir,
			String officeFilePath, String swfDir, String swfFileName,
			boolean paging) {
		office2Swf(swftoolsFilePath, languageDir, officeFilePath, swfDir,
				swfFileName, paging, null);
	}

	public void shutdown() throws IllegalStateException {
		if (client == null) {
			throw new IllegalStateException("No client to shutdown");
		}
		client.shutdown();
	}
}
