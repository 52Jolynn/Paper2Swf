package com.laudandjolynn.paper2swf.utils;

import java.io.File;

import org.apache.log4j.Logger;

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

	private static int TRANSFER_SUCCEED = 1;// 转换成功
	private static int TRANSFER_FAILURE = 0;// 转换失败

	// MS
	private static int WORD_DO_NOT_SAVE_CHANGES = 0;// WORD不保存待定的变更
	private static int WORD_PDF_FORMAT_CODE = 17;// WORD PDF格式
	private static int DISPLAY_ALERTS = 0;// 不显示对话框
	private static int EXCEL_PDF_FORMAT_CODE = 0;// EXCEl PDF格式
	private static int PPT_PDF_FORMAT_CODE = 32; // PPT PDF格式
	private static int PPT_OPEN_READ_ONLY_MODE = 1; // PPT以只读模式打开
	private static int WORD_FINAL_REVISIONS_VIEW = 0;// 审阅最终模式

	// WPS
	private static int WPS_DO_NOT_SAVE_CHANGES = 0;// WPS不保存待定的变更
	private static int DPS_OPEN_READ_ONLY_MODE = 1; // DPS以只读模式打开
	private static boolean ET_DO_NOT_SAVE_CHANGES = false;// ET不保存待定的变更

	/**
	 * 将doc、docx转换成pdf
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see MS VBA
	 */
	public static int doc2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent word = null;
			try {
				word = new ActiveXComponent("Word.Application");
				word.setProperty("Visible", false);
				word.setProperty("DisplayAlerts", DISPLAY_ALERTS);

				Dispatch docs = word.getProperty("Documents").toDispatch();

				log.info("使用MS Word打开文档..." + srcFilePath);
				Dispatch doc = Dispatch.call(docs, "Open", srcFilePath, true,
						true).toDispatch();

				Dispatch activeWindow = Dispatch.get(doc, "ActiveWindow")
						.toDispatch();
				Dispatch view = Dispatch.get(activeWindow, "View").toDispatch();
				Dispatch.put(view, "ShowRevisionsAndComments", false);
				Dispatch.put(view, "RevisionsView", new Variant(
						WORD_FINAL_REVISIONS_VIEW));

				log.info("转换文档到PDF..." + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(doc, "SaveAs", destFilePath, WORD_PDF_FORMAT_CODE);
				Dispatch.call(doc, "Close", false);
				log.info("转换完成");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				log.info("调用MS Word转换失败:" + e.getMessage());
			} finally {
				if (word != null) {
					log.info("退出Word");
					word.invoke("Quit", WORD_DO_NOT_SAVE_CHANGES);
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");

		return TRANSFER_FAILURE;
	}

	/**
	 * 将xls、xlsx转换成pdf
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see MS VBA
	 */
	public static int xls2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent excel = null;
			try {
				excel = new ActiveXComponent("Excel.Application");
				excel.setProperty("Visible", new Variant(false));
				Dispatch workbooks = excel.getProperty("Workbooks")
						.toDispatch();
				log.info("使用MS Excel打开文档..." + srcFilePath);
				Dispatch workbook = Dispatch.call(workbooks, "Open",
						srcFilePath, false, true).toDispatch();
				log.info("转换文档到PDF..." + destFilePath);

				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(workbook, "ExportAsFixedFormat",
						EXCEL_PDF_FORMAT_CODE, destFilePath);
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
		return TRANSFER_FAILURE;
	}

	/**
	 * 将ppt、pptx转换成pdf
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see MS VBA
	 */
	public static int ppt2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent ppt = null;
			try {
				ppt = new ActiveXComponent("Powerpoint.Application");
				ppt.setProperty("Visible", new Variant(true));
				Dispatch presentations = ppt.getProperty("Presentations")
						.toDispatch();

				log.info("使用MS Powerpoint打开文档..." + srcFilePath);
				Dispatch presentation = Dispatch.call(presentations, "Open",
						new Variant(srcFilePath),
						new Variant(PPT_OPEN_READ_ONLY_MODE)).toDispatch();

				log.info("转换文档到PDF..." + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(presentation, "SaveAs",
						new Variant(destFilePath), new Variant(
								PPT_PDF_FORMAT_CODE));
				Dispatch.call(presentation, "Close");
				log.info("转换完成");
				return TRANSFER_SUCCEED;
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
		return TRANSFER_FAILURE;
	}

	/**
	 * 将wps转换成pdf
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see WPS VBA
	 */
	public static int wps2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent wps = null;
			try {
				wps = new ActiveXComponent("WPS.Application");
				wps.setProperty("Visible", false);
				Dispatch docs = wps.getProperty("Documents").toDispatch();
				log.info("调用WPS文字打开文档..." + srcFilePath);
				Dispatch doc = Dispatch.call(docs, "Open",
						new Variant(srcFilePath), new Variant(false),
						new Variant(true)).toDispatch();

				log.info("转换文档到PDF..." + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(doc, "ExportPdf", new Variant(destFilePath));
				Dispatch.call(doc, "Close",
						new Variant(WPS_DO_NOT_SAVE_CHANGES));
				log.info("转换完成");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				log.info("调用WPS文字转换失败:" + e.getMessage());
			} finally {
				if (wps != null) {
					log.info("退出WPS文字");
					wps.invoke("Quit", new Variant(WPS_DO_NOT_SAVE_CHANGES));
				}
				ComThread.Release();
			}
		}
		log.info("文档不存在...");

		return TRANSFER_FAILURE;
	}

	/**
	 * 将dps转换成pdf
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see WPS VBA
	 */
	public static int dps2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent dps = null;
			try {
				dps = new ActiveXComponent("WPP.Application");
				Dispatch presentations = dps.getProperty("Presentations")
						.toDispatch();

				log.info("使用WPS演示打开文档..." + srcFilePath);
				Dispatch presentation = Dispatch.call(presentations, "Open",
						new Variant(srcFilePath), new Variant(null),
						new Variant(DPS_OPEN_READ_ONLY_MODE)).toDispatch();

				log.info("转换文档到PDF..." + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(presentation, "ExportPdf", new Variant(
						destFilePath));
				Dispatch.call(presentation, "Close");
				log.info("转换完成");
				return TRANSFER_SUCCEED;
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

		return TRANSFER_FAILURE;
	}

	/**
	 * 将et转换成pdf
	 * 
	 * @param srcFilePath
	 *            源文件路径
	 * @param destFilePath
	 *            目的文件路径
	 * @return 返回0表示转换失败，1表示成功
	 * 
	 * @see WPS VBA
	 */
	public static int et2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent et = null;
			try {
				et = new ActiveXComponent("ET.Application");
				et.setProperty("Visible", false);
				Dispatch workbooks = et.getProperty("Workbooks").toDispatch();
				log.info("调用WPS表格打开文档..." + srcFilePath);
				Dispatch workbook = Dispatch.call(workbooks, "Open",
						new Variant(srcFilePath)).toDispatch();

				log.info("转换文档到PDF..." + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(workbook, "ExportPdf", new Variant(destFilePath));
				Dispatch.call(workbook, "Close", new Variant(
						ET_DO_NOT_SAVE_CHANGES));
				log.info("转换完成");
				return TRANSFER_SUCCEED;
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
		return TRANSFER_FAILURE;
	}
}