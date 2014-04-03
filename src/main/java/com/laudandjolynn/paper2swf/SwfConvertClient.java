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
	 */
	public void convertPdf2Swf(String swftoolsFilePath, String languageDir,
			String pdfFilePath, String swfDir, String swfFileName) {
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

		int totalBytes = swftoolsFilePathDataLen + languageDirDataLen
				+ pdfFilePathDataLen + swfDirDataLen + swfFileNameDataLen + 4;
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

		String function = Pdf2SwfConvertFunction.class.getCanonicalName();
		String uniqueId = null;
		GearmanJob job = GearmanJobImpl.createBackgroundJob(function, data,
				uniqueId);
		client.submit(job);
	}

	private class OpenOfficeConfig {
		private String host;
		private int port;

		public OpenOfficeConfig(String host, int port) {
			this.host = host;
			this.port = port;
		}
	}

	private void convertOffice2Swf(String swftoolsFilePath, String languageDir,
			String srcFilePath, String pdfFilePath, String swfDir,
			String swfFileName, OpenOfficeConfig cfg) {
		byte[] swftoolsFilePathData = ByteUtils.toUTF8Bytes(swftoolsFilePath);
		int swftoolsFilePathDataLen = swftoolsFilePathData.length;
		byte[] languageDirData = ByteUtils.toUTF8Bytes(languageDir);
		int languageDirDataLen = languageDirData.length;
		byte[] officeFilePathData = ByteUtils.toUTF8Bytes(srcFilePath);
		int officeFilePathDataLen = officeFilePathData.length;
		byte[] pdfFilePathData = ByteUtils.toUTF8Bytes(pdfFilePath);
		int pdfFilePathDataLen = pdfFilePathData.length;
		byte[] swfDirData = ByteUtils.toUTF8Bytes(swfDir);
		int swfDirDataLen = swfDirData.length;
		byte[] swfFileNameData = ByteUtils.toUTF8Bytes(swfFileName);
		int swfFileNameDataLen = swfFileNameData.length;

		int totalBytes = swftoolsFilePathDataLen + languageDirDataLen
				+ officeFilePathDataLen + pdfFilePathDataLen + swfDirDataLen
				+ swfFileNameDataLen + 5;
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

		if (cfg != null) {// open office
			byte[] hostData = ByteUtils.toUTF8Bytes(cfg.host);
			int hostDataLen = hostData.length;
			byte[] portData = ByteUtils.toBigEndian(cfg.port);
			int portDataLen = portData.length;

			byte[] oo = new byte[totalBytes + hostDataLen + portDataLen + 1];
			offset += swfFileNameDataLen;
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
	}

	public void convertOffice2Swf_openoffice(String host, int port,
			String swftoolsFilePath, String languageDir, String officeFilePath,
			String pdfFilePath, String swfDir, String swfFileName) {
		convertOffice2Swf(swftoolsFilePath, languageDir, officeFilePath,
				pdfFilePath, swfDir, swfFileName, new OpenOfficeConfig(host,
						port));
	}

	/**
	 * 
	 * @param swftoolsFilePath
	 *            swftools执行文件路径，包括文件名，比如c:\swftools\pdf2swf.exe
	 * @param languageDir
	 *            语言支持文件目录，比如c:\swftools\xpdf\
	 * @param officeFilePath
	 *            office文件路径
	 * @param pdfFilePath
	 *            PDF文件路径
	 * @param swfDir
	 *            swf文件存储目录
	 * @param swfFileName
	 *            swf文件名
	 */
	public void convertOffice2Swf_jacob(String swftoolsFilePath,
			String languageDir, String officeFilePath, String pdfFilePath,
			String swfDir, String swfFileName) {
		convertOffice2Swf(swftoolsFilePath, languageDir, officeFilePath,
				pdfFilePath, swfDir, swfFileName, null);
	}

	public void shutdown() throws IllegalStateException {
		if (client == null) {
			throw new IllegalStateException("No client to shutdown");
		}
		client.shutdown();
	}
}
