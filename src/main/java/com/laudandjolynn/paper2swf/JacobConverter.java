package com.laudandjolynn.paper2swf;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.laudandjolynn.paper2swf.utils.Office2PdfUtils;

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

	@Override
	public int convert(String srcFilePath, String destFilePath) {
		int index = srcFilePath.lastIndexOf(".");
		if (index == -1) {
			logger.error("source file must contain file extension.");
			return 0;
		}
		String fileType = srcFilePath.substring(index + 1).toLowerCase();
		if (fileType.equals("doc") || fileType.equals("docx")) {
			return Office2PdfUtils.doc2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("xls") || fileType.equals("xlsx")) {
			return Office2PdfUtils.xls2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("ppt") || fileType.equals("pptx")) {
			return Office2PdfUtils.ppt2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("wps")) {
			return Office2PdfUtils.wps2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("dps")) {
			return Office2PdfUtils.dps2pdf(srcFilePath, destFilePath);
		} else if (fileType.equals("et")) {
			return Office2PdfUtils.et2pdf(srcFilePath, destFilePath);
		}
		logger.error("can't handle the file type currently: " + fileType);
		return 0;
	}
}
