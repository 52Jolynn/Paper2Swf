/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.example;


import java.util.concurrent.Future;
import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.client.GearmanJobResult;
import org.gearman.common.Constants;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;

public class DigestClient {

    private GearmanClient client;
    private static final String ALGO_NAME = "MD5";

    public DigestClient(GearmanJobServerConnection conn) {
        client = new GearmanClientImpl();
        client.addJobServer(conn);
    }

    public DigestClient(String host, int port) {
        this(new GearmanNIOJobServerConnection(host, port));
    }

    public byte[] digest(byte[] input) {
        String function = DigestFunction.class.getCanonicalName();
        String uniqueId = null;
        byte [] algoNameBytes = ALGO_NAME.getBytes();
        byte [] digestParms = new byte [input.length + algoNameBytes.length + 1];
        System.arraycopy(algoNameBytes, 0, digestParms, 0, algoNameBytes.length);
        digestParms[algoNameBytes.length] = '\0';
        System.arraycopy(input, 0, digestParms, algoNameBytes.length + 1,
                input.length);
        GearmanJob job = GearmanJobImpl.createJob(function, digestParms,
                uniqueId);
        Future<GearmanJobResult> f = client.submit(job);
        GearmanJobResult jr = null;
        try {
            jr = f.get();
        } catch (Exception e) {
            e.printStackTrace();                                                //NOPMD
        }
        if (jr == null) {
            return new byte[0];
        } else {
            if (jr.jobSucceeded()) {
                return jr.getResults();
            } else {
                System.err.println(ByteUtils.fromUTF8Bytes(jr.getExceptions()));//NOPMD
                return new byte[0];
            }
        }
    }

    public void shutdown() throws IllegalStateException {
        if (client == null) {
            throw new IllegalStateException("No client to shutdown");
        }
        client.shutdown();
    }

    public static void main(String[] args) {
        if (args.length == 0 || args.length > 3) {
            usage();
            return;
        }
        String host = Constants.GEARMAN_DEFAULT_TCP_HOST;
        int port = Constants.GEARMAN_DEFAULT_TCP_PORT;
        byte[] payload = ByteUtils.toUTF8Bytes(args[args.length - 1]);
        for (String arg : args) {
            if (arg.startsWith("-h")) {
                host = arg.substring(2);
            } else if (arg.startsWith("-p")) {
                port = Integer.parseInt(arg.substring(2));
            }
        }
        DigestClient dc = new DigestClient(host, port);
        byte[] md5 = dc.digest(payload);
        System.out.println(ByteUtils.toHex(md5));                               //NOPMD
	dc.shutdown();
    }

    public static void usage() {
        String[] usage = {
            "usage: org.gearman.example.DigestClient [-h<host>] [-p<port>]" +
                    " <string>",
            "\t-h<host> - job server host",
            "\t-p<port> - job server port",
            "\n\tExample: java org.gearman.example.DigestClient Foo",
            "\tExample: java org.gearman.example.DigestClient -h127.0.0.1" +
                    " -p4730 Bar", //
        };

        for (String line : usage) {
            System.err.println(line);                                           //NOPMD
        }
    }
}
