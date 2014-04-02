package com.laudandjolynn.paper2swf;

import org.gearman.client.GearmanJobResult;
import org.gearman.util.ByteUtils;
import org.gearman.worker.AbstractGearmanFunction;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午10:34:00
 * @copyright: www.laudandjolynn.com
 */
public class PdfConvertFunction extends AbstractGearmanFunction {

	@Override
	public GearmanJobResult executeFunction() {
		String data = ByteUtils.fromUTF8Bytes((byte[]) this.data);
		System.out.println(data);
		return null;
	}

}
