package com.laudandjolynn.paper2swf;

import java.util.ArrayList;
import java.util.List;

import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnectionFactory;
import org.gearman.worker.GearmanFunction;
import org.gearman.worker.GearmanWorker;
import org.gearman.worker.GearmanWorkerImpl;

/**
 * @author: Laud
 * @email: htd0324@gmail.com
 * @date: 2014年4月2日 下午10:37:52
 * @copyright: www.laudandjolynn.com
 */
public class SwfConvertWorkerRunner {
	private GearmanJobServerConnection conn = null;
	private List<Class<GearmanFunction>> functions = new ArrayList<Class<GearmanFunction>>();

	public SwfConvertWorkerRunner(String host, int port) {
		GearmanNIOJobServerConnectionFactory factory = new GearmanNIOJobServerConnectionFactory();
		conn = factory.createConnection(host, port);
	}

	public void addFunction(Class<GearmanFunction> function) {
		functions.add(function);
	}

	public void start() {
		GearmanWorker worker = new GearmanWorkerImpl();
		worker.addServer(conn);
		for (Class<GearmanFunction> fun : functions) {
			worker.registerFunction(fun);
		}
		worker.work();
	}
}
