package com.laudandjolynn.paper2swf;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午8:44:32
 * @copyright: www.laudandjolynn.com
 */
public interface PdfConverter {
	/**
	 * PDF转换
	 * 
	 * @param srcFilePath
	 *            源文件
	 * @param destFilePath
	 *            目标文件
	 * @return 返回0表示转换失败，1表示成功
	 */
	public int convert(String srcFilePath, String destFilePath);
}
