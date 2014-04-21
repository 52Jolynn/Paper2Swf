package com.laudandjolynn.paper2swf;

import org.gearman.client.GearmanJobResult;
import org.gearman.client.GearmanJobResultImpl;
import org.gearman.util.ByteUtils;
import org.gearman.worker.AbstractGearmanFunction;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.laudandjolynn.paper2swf.utils.ConvertException;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 下午1:08:21
 * @copyright: www.laudandjolynn.com
 */
public class Office2PdfConvertFunction extends AbstractGearmanFunction {
	private final static Logger logger = LoggerFactory
			.getLogger(Office2PdfConvertFunction.class);

	@Override
	public GearmanJobResult executeFunction() {
		String data = ByteUtils.fromUTF8Bytes((byte[]) this.data);
		String[] params = data.split("\0");

		int index = 0;
		String srcFilePath = params[index++];
		String destFilePath = params[index++];

		PdfConverter converter = null;
		if (params.length > index) {
			String host = params[index++];
			int port = ByteUtils.fromBigEndian(ByteUtils
					.toUTF8Bytes(params[index]));
			converter = new OpenOfficeConverter(host, port);
		} else {
			converter = new JacobConverter();
		}
		int r = converter.office2Pdf(srcFilePath, destFilePath);
		if (r == -1) {
			logger.error("convert office to pdf fail.");
			throw new ConvertException("convert office to pdf fail");
		}

		GearmanJobResult result = new GearmanJobResultImpl(
				ByteUtils.toBigEndian(r));
		return result;
	}
}
