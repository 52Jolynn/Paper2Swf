package com.laudandjolynn.paper2swf;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.jacob.activeX.ActiveXComponent;
import com.jacob.com.ComThread;
import com.jacob.com.Dispatch;
import com.jacob.com.Variant;
import com.laudandjolynn.paper2swf.utils.JacobNativesLoader;

/**
 * jacob转换器
 * 
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午8:55:00
 * @copyright: www.laudandjolynn.com
 */
public class JacobConverter implements PdfConverter {
	private final static Logger logger = LoggerFactory
			.getLogger(JacobConverter.class);
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

	static {
		// load native lib
		JacobNativesLoader.load();
	}

	@Override
	public int convert(String srcFilePath, String destFilePath) {
		int index = srcFilePath.lastIndexOf(".");
		if (index == -1) {
			logger.error("source file must contain file extension.");
			return 0;
		}
		String fileType = srcFilePath.substring(index + 1).toLowerCase();
		if (fileType.equals("doc") || fileType.equals("docx")) {
			return doc2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("xls") || fileType.equals("xlsx")) {
			return xls2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("ppt") || fileType.equals("pptx")) {
			return ppt2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("wps")) {
			return wps2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("dps")) {
			return dps2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("et")) {
			return et2pdf(srcFilePath, destFilePath);
		}
		logger.error("can't handle the file type currently: " + fileType);
		return 0;
	}

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
	private int doc2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent word = null;
			try {
				word = new ActiveXComponent("Word.Application");
				word.setProperty("Visible", false);
				word.setProperty("DisplayAlerts", DISPLAY_ALERTS);

				Dispatch docs = word.getProperty("Documents").toDispatch();

				logger.info("open document with MS Word." + srcFilePath);
				Dispatch doc = Dispatch.call(docs, "Open", srcFilePath, true,
						true).toDispatch();

				Dispatch activeWindow = Dispatch.get(doc, "ActiveWindow")
						.toDispatch();
				Dispatch view = Dispatch.get(activeWindow, "View").toDispatch();
				Dispatch.put(view, "ShowRevisionsAndComments", false);
				Dispatch.put(view, "RevisionsView", new Variant(
						WORD_FINAL_REVISIONS_VIEW));

				logger.info("convert pdf docuemnt to " + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(doc, "SaveAs", destFilePath, WORD_PDF_FORMAT_CODE);
				Dispatch.call(doc, "Close", false);
				logger.info("convert doc to pdf complete.");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				logger.error("call MS Word fail.", e);
			} finally {
				if (word != null) {
					logger.info("exit Word.");
					word.invoke("Quit", WORD_DO_NOT_SAVE_CHANGES);
				}
				ComThread.Release();
			}
		}
		logger.error("document does not exist.");

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
	private int xls2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent excel = null;
			try {
				excel = new ActiveXComponent("Excel.Application");
				excel.setProperty("Visible", new Variant(false));
				Dispatch workbooks = excel.getProperty("Workbooks")
						.toDispatch();
				logger.info("open document with MS Excel " + srcFilePath);
				Dispatch workbook = Dispatch.call(workbooks, "Open",
						srcFilePath, false, true).toDispatch();
				logger.info("convert pdf document to " + destFilePath);

				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(workbook, "ExportAsFixedFormat",
						EXCEL_PDF_FORMAT_CODE, destFilePath);
				Dispatch.call(workbook, "Close", false);
				logger.info("convert xls to pdf complete.");
			} catch (Exception e) {
				logger.error("call MS Excel fail.", e);
			} finally {
				if (excel != null) {
					logger.info("exit Excel.");
					excel.invoke("Quit");
				}
				ComThread.Release();
			}
		}
		logger.error("document does not exist.");
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
	private int ppt2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent ppt = null;
			try {
				ppt = new ActiveXComponent("Powerpoint.Application");
				ppt.setProperty("Visible", new Variant(true));
				Dispatch presentations = ppt.getProperty("Presentations")
						.toDispatch();

				logger.info("open document with MS Powerpoint " + srcFilePath);
				Dispatch presentation = Dispatch.call(presentations, "Open",
						new Variant(srcFilePath),
						new Variant(PPT_OPEN_READ_ONLY_MODE)).toDispatch();

				logger.info("convert pdf document to " + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(presentation, "SaveAs",
						new Variant(destFilePath), new Variant(
								PPT_PDF_FORMAT_CODE));
				Dispatch.call(presentation, "Close");
				logger.info("convert ppt to pdf complete.");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				logger.error("call MS Powerpoint fail.", e);
			} finally {
				if (ppt != null) {
					logger.info("exit Powerpoint.");
					ppt.invoke("Quit");
				}
				ComThread.Release();
			}
		}
		logger.error("document does not exist.");
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
	private int wps2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent wps = null;
			try {
				wps = new ActiveXComponent("WPS.Application");
				wps.setProperty("Visible", false);
				Dispatch docs = wps.getProperty("Documents").toDispatch();
				logger.info("open document with WPS " + srcFilePath);
				Dispatch doc = Dispatch.call(docs, "Open",
						new Variant(srcFilePath), new Variant(false),
						new Variant(true)).toDispatch();

				logger.info("convert pdf document to " + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(doc, "ExportPdf", new Variant(destFilePath));
				Dispatch.call(doc, "Close",
						new Variant(WPS_DO_NOT_SAVE_CHANGES));
				logger.info("convert wps to pdf complete.");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				logger.error("call WPS fail.", e);
			} finally {
				if (wps != null) {
					logger.error("exit WPS.");
					wps.invoke("Quit", new Variant(WPS_DO_NOT_SAVE_CHANGES));
				}
				ComThread.Release();
			}
		}
		logger.error("document does not exist.");

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
	private int dps2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent dps = null;
			try {
				dps = new ActiveXComponent("WPP.Application");
				Dispatch presentations = dps.getProperty("Presentations")
						.toDispatch();

				logger.info("open document with WPS " + srcFilePath);
				Dispatch presentation = Dispatch.call(presentations, "Open",
						new Variant(srcFilePath), new Variant(null),
						new Variant(DPS_OPEN_READ_ONLY_MODE)).toDispatch();

				logger.info("convert pdf document to " + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(presentation, "ExportPdf", new Variant(
						destFilePath));
				Dispatch.call(presentation, "Close");
				logger.info("convert dps to pdf complete.");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				logger.error("call WPS fail.", e);
			} finally {
				if (dps != null) {
					logger.info("exit WPS.");
					dps.invoke("Quit");
				}
				ComThread.Release();
			}
		}
		logger.error("document does not exist.");

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
	private int et2pdf(String srcFilePath, String destFilePath) {
		File file = new File(srcFilePath);
		if (file.exists()) {
			ComThread.InitMTA();
			ActiveXComponent et = null;
			try {
				et = new ActiveXComponent("ET.Application");
				et.setProperty("Visible", false);
				Dispatch workbooks = et.getProperty("Workbooks").toDispatch();
				logger.info("open document with WPS " + srcFilePath);
				Dispatch workbook = Dispatch.call(workbooks, "Open",
						new Variant(srcFilePath)).toDispatch();

				logger.info("convert pdf document to " + destFilePath);
				File toFile = new File(destFilePath);
				if (toFile.exists()) {
					toFile.delete();
				}
				Dispatch.call(workbook, "ExportPdf", new Variant(destFilePath));
				Dispatch.call(workbook, "Close", new Variant(
						ET_DO_NOT_SAVE_CHANGES));
				logger.info("convert et to pdf complete.");
				return TRANSFER_SUCCEED;
			} catch (Exception e) {
				logger.error("call WPS fail.", e);
			} finally {
				if (et != null) {
					logger.info("exit WPS.");
					et.invoke("Quit");
				}

				ComThread.Release();
			}
		}
		logger.error("document does not exist.");
		return TRANSFER_FAILURE;
	}
}
