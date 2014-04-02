package com.laudandjolynn.paper2swf.utils;

import java.io.File;

import org.apache.log4j.Logger;

import com.artofsolving.jodconverter.DefaultDocumentFormatRegistry;
import com.artofsolving.jodconverter.DocumentConverter;
import com.artofsolving.jodconverter.DocumentFormat;
import com.artofsolving.jodconverter.openoffice.connection.OpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.connection.SocketOpenOfficeConnection;
import com.artofsolving.jodconverter.openoffice.converter.OpenOfficeDocumentConverter;
import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;

/**
 * office to swf
 * 
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2012-9-23
 * @copyright: www.dreamoriole.com
 */
public class Office2PdfUtils {
	private final static Logger log = Logger.getLogger(Office2PdfUtils.class);

	private static int openOfficeListenerPort = 8100;
	private static int TRANSFERSUCCESS = 1;// 转换成功
	private static int TRANSFERFAILURE = 0;// 转换失败

	// MS
	private static int WORDDONOTSAVECHANGES = 0;// WORD不保存待定的变更
	private static int WORDFORMATPDF = 17;// WORD PDF格式
	private static int DISPLAYALERTS = 0;// 不显示对话框
	private static int EXCELFORMATPDF = 0;// EXCEl PDF格式
	private static int PPTFORMATPDF = 32; // PPT PDF格式
	private static int PPTREADONLYMODE = 1; // PPT以只读模式打开
	private static int WDREVISIONSVIEWFINAL = 0;// 审阅最终模式

	// WPS
	private static int WPSDONOTSAVECHANGES = 0;// WPS不保存待定的变更
	private static int DPSREADONLYMODE = 1; // DPS以只读模式打开
	private static boolean ETDONOTSAVECHANGES = false;// ET不保存待定的变更

	// OpenOffice Service不是线程安全的
	private static OpenOfficeConnection conn = null;

	/**
	 * 将doc、docx转换成pdf
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * 
	 * 
	 * @param destPath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see MS VBA
	 */
	public static int doc2pdf(String srcPath, String destPath) {
		File file = new File(srcPath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent word = null;
			try {
				word = new ActiveXComponent("Word.Application");
				word.setProperty("Visible", false);
				word.setProperty("DisplayAlerts", DISPLAYALERTS);

				Dispatch docs = word.getProperty("Documents").toDispatch();

				log.info("使用MS Word打开文档..." + srcPath);
				Dispatch doc = Dispatch.call(docs, "Open", srcPath, true, true)
						.toDispatch();

				Dispatch activeWindow = Dispatch.get(doc, "ActiveWindow")
						.toDispatch();
				Dispatch view = Dispatch.get(activeWindow, "View").toDispatch();
				Dispatch.put(view, "ShowRevisionsAndComments", false);
				Dispatch.put(view, "RevisionsView", new Variant(
						WDREVISIONSVIEWFINAL));

				log.info("转换文档到PDF..." + destPath);
				File toFile = new File(destPath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(doc, "SaveAs", destPath, WORDFORMATPDF);
				Dispatch.call(doc, "Close", false);
				log.info("转换完成");
				return TRANSFERSUCCESS;
			} catch (Exception e) {
				log.info("调用MS Word转换失败:" + e.getMessage());
			} finally {
				if (word != null) {
					log.info("退出Word");
					word.invoke("Quit", WORDDONOTSAVECHANGES);
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");

		return TRANSFERFAILURE;
	}

	/**
	 * 将xls、xlsx转换成pdf
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * 
	 * 
	 * @param destPath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see MS VBA
	 */
	public static int xls2pdf(String srcPath, String destPath) {
		File file = new File(srcPath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent excel = null;
			try {
				excel = new ActiveXComponent("Excel.Application");
				excel.setProperty("Visible", new Variant(false));
				Dispatch workbooks = excel.getProperty("Workbooks")
						.toDispatch();
				log.info("使用MS Excel打开文档..." + srcPath);
				Dispatch workbook = Dispatch.call(workbooks, "Open", srcPath,
						false, true).toDispatch();
				log.info("转换文档到PDF..." + destPath);

				File toFile = new File(destPath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(workbook, "ExportAsFixedFormat", EXCELFORMATPDF,
						destPath);
				Dispatch.call(workbook, "Close", false);
			} catch (Exception e) {
				log.error("调用MS Excel转换失败:" + e.getMessage());
			} finally {
				if (excel != null) {
					log.info("退出Excel");
					excel.invoke("Quit");
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");
		return TRANSFERFAILURE;
	}

	/**
	 * 将ppt、pptx转换成pdf
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * 
	 * 
	 * @param destPath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see MS VBA
	 */
	public static int ppt2pdf(String srcPath, String destPath) {
		File file = new File(srcPath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent ppt = null;
			try {
				ppt = new ActiveXComponent("Powerpoint.Application");
				ppt.setProperty("Visible", new Variant(true));
				Dispatch presentations = ppt.getProperty("Presentations")
						.toDispatch();

				log.info("使用MS Powerpoint打开文档..." + srcPath);
				Dispatch presentation = Dispatch.call(presentations, "Open",
						new Variant(srcPath), new Variant(PPTREADONLYMODE))
						.toDispatch();

				log.info("转换文档到PDF..." + destPath);
				File toFile = new File(destPath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(presentation, "SaveAs", new Variant(destPath),
						new Variant(PPTFORMATPDF));
				Dispatch.call(presentation, "Close");
				log.info("转换完成");
				return TRANSFERSUCCESS;
			} catch (Exception e) {
				log.info("调用MS Powerpoint转换失败:" + e.getMessage());
			} finally {
				if (ppt != null) {
					log.info("退出Powerpoint");
					ppt.invoke("Quit");
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");
		return TRANSFERFAILURE;
	}

	/**
	 * 将wps转换成pdf
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * 
	 * 
	 * @param destPath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see WPS VBA
	 */
	public static int wps2pdf(String srcPath, String destPath) {
		File file = new File(srcPath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent wps = null;
			try {
				wps = new ActiveXComponent("WPS.Application");
				wps.setProperty("Visible", false);
				Dispatch docs = wps.getProperty("Documents").toDispatch();
				log.info("调用WPS文字打开文档..." + srcPath);
				Dispatch doc = Dispatch.call(docs, "Open",
						new Variant(srcPath), new Variant(false),
						new Variant(true)).toDispatch();

				log.info("转换文档到PDF..." + destPath);
				File toFile = new File(destPath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(doc, "ExportPdf", new Variant(destPath));
				Dispatch.call(doc, "Close", new Variant(WPSDONOTSAVECHANGES));
				log.info("转换完成");
				return TRANSFERSUCCESS;
			} catch (Exception e) {
				log.info("调用WPS文字转换失败:" + e.getMessage());
			} finally {
				if (wps != null) {
					log.info("退出WPS文字");
					wps.invoke("Quit", new Variant(WPSDONOTSAVECHANGES));
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");

		return TRANSFERFAILURE;
	}

	/**
	 * 将dps转换成pdf
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * 
	 * 
	 * @param destPath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see WPS VBA
	 */
	public static int dps2pdf(String srcPath, String destPath) {
		File file = new File(srcPath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent dps = null;
			try {
				dps = new ActiveXComponent("WPP.Application");
				Dispatch presentations = dps.getProperty("Presentations")
						.toDispatch();

				log.info("使用WPS演示打开文档..." + srcPath);
				Dispatch presentation = Dispatch.call(presentations, "Open",
						new Variant(srcPath), new Variant(null),
						new Variant(DPSREADONLYMODE)).toDispatch();

				log.info("转换文档到PDF..." + destPath);
				File toFile = new File(destPath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(presentation, "ExportPdf", new Variant(destPath));
				Dispatch.call(presentation, "Close");
				log.info("转换完成");
				return TRANSFERSUCCESS;
			} catch (Exception e) {
				log.info("调用WPS演示转换失败:" + e.getMessage());
			} finally {
				if (dps != null) {
					log.info("退出WPS演示");
					dps.invoke("Quit");
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");

		return TRANSFERFAILURE;
	}

	/**
	 * 将et转换成pdf
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * 
	 * 
	 * @param destPath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see WPS VBA
	 */
	public static int et2pdf(String srcPath, String destPath) {
		File file = new File(srcPath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent et = null;
			try {
				et = new ActiveXComponent("ET.Application");
				et.setProperty("Visible", false);
				Dispatch workbooks = et.getProperty("Workbooks").toDispatch();
				log.info("调用WPS表格打开文档..." + srcPath);
				Dispatch workbook = Dispatch.call(workbooks, "Open",
						new Variant(srcPath)).toDispatch();

				log.info("转换文档到PDF..." + destPath);
				File toFile = new File(destPath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(workbook, "ExportPdf", new Variant(destPath));
				Dispatch.call(workbook, "Close",
						new Variant(ETDONOTSAVECHANGES));
				log.info("转换完成");
				return TRANSFERSUCCESS;
			} catch (Exception e) {
				log.info("调用WPS表格转换失败:" + e.getMessage());
			} finally {
				if (et != null) {
					log.info("退出WPS表格");
					et.invoke("Quit");
				}

				ComThread.Release();
			}
		}
		log.info("文档不存在...");
		return TRANSFERFAILURE;
	}

	/**
	 * 使用openoffice进行转换
	 * 
	 * @param srcPath
	 *            源文件路径
	 * 
	 * @param tgtPath
	 *            目标文件路径
	 * @return 返回0表示转换失败，1表示成功
	 */
	public static int office2pdf(String srcPath, String tgtPath) {
		conn = new SocketOpenOfficeConnection(openOfficeListenerPort);

		synchronized (conn) {
			try {
				File srcFile = new File(srcPath);
				File tgtFile = new File(tgtPath);
				DefaultDocumentFormatRegistry formatRegistry = new DefaultDocumentFormatRegistry();
				DocumentFormat pdf = formatRegistry
						.getFormatByFileExtension("pdf");
				conn.connect();
				DocumentConverter converter = new OpenOfficeDocumentConverter(
						conn);
				converter.convert(srcFile, tgtFile, pdf);
			} catch (Exception e) {
				log.info("调用OpenOffice转换失败:" + e.getMessage());
			} finally {
				if (conn != null) {
					conn.disconnect();
					conn = null;
				}
			}
		}
		return TRANSFERFAILURE;
	}

	/**
	 * 取得OpenOffice监听端口
	 * 
	 * @return
	 */
	public static int getOpenOfficeListenerPort() {
		return openOfficeListenerPort;
	}

	/**
	 * 设置OpenOffice监听端口
	 * 
	 * @param openOfficeListenerPort
	 */
	public static void setOpenOfficeListenerPort(int openOfficeListenerPort) {
		Office2PdfUtils.openOfficeListenerPort = openOfficeListenerPort;
	}
}