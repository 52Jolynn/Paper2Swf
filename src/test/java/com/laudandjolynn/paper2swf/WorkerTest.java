package com.laudandjolynn.paper2swf;

import org.gearman.worker.GearmanFunction;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月21日 上午11:46:52
 * @copyright: www.laudandjolynn.com
 */
public class WorkerTest {
	private final static String HOST = "192.168.1.241";
	private final static int PORT = 4730;

	@SuppressWarnings("unchecked")
	public static void main(String[] args) {
		WorkerRunner worker = new WorkerRunner(HOST, PORT);
		try {
			Class<?> c = Class.forName(Pdf2SwfConvertFunction.class
					.getCanonicalName());
			if (GearmanFunction.class.isAssignableFrom(c)) {
				worker.addFunction((Class<GearmanFunction>) c);
			}
			c = Class.forName(Office2SwfConvertFunction.class
					.getCanonicalName());
			if (GearmanFunction.class.isAssignableFrom(c)) {
				worker.addFunction((Class<GearmanFunction>) c);
			}
			c = Class.forName(Office2PdfConvertFunction.class
					.getCanonicalName());
			if (GearmanFunction.class.isAssignableFrom(c)) {
				worker.addFunction((Class<GearmanFunction>) c);
			}
			worker.start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
