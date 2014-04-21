/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Use and distribution licensed under the
 * GNU Lesser General Public License (LGPL) version 2.1.
 * See the COPYING file in the parent directory for full text.
 */
package org.gearman.example;


import org.gearman.client.GearmanClient;
import org.gearman.client.GearmanClientImpl;
import org.gearman.client.GearmanJob;
import org.gearman.client.GearmanJobImpl;
import org.gearman.client.GearmanJobResult;
import org.gearman.common.Constants;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;

public class ReverseClient {

    private GearmanClient client;

    public ReverseClient(GearmanJobServerConnection conn) {
        client = new GearmanClientImpl();
        client.addJobServer(conn);
    }

    public ReverseClient(String host, int port) {
        this(new GearmanNIOJobServerConnection(host, port));
    }

    public String reverse(String input) {
        String function = ReverseFunction.class.getCanonicalName();
        String uniqueId = null;
        byte[] data = ByteUtils.toUTF8Bytes(input);
        GearmanJobResult res = null;
        GearmanJob job = GearmanJobImpl.createJob(function, data, uniqueId);
        String value = "";

        client.submit(job);

        try {
            res = job.get();
            value = ByteUtils.fromUTF8Bytes(res.getResults());
        } catch (Exception e) {
            e.printStackTrace();                                                //NOPMD
        }
        return value;
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
        String payload = args[args.length - 1];
        for (String arg : args) {
            if (arg.startsWith("-h")) {
                host = arg.substring(2);
            } else if (arg.startsWith("-p")) {
                port = Integer.parseInt(arg.substring(2));
            }
        }
        ReverseClient rc = new ReverseClient(host, port);
        System.out.println(rc.reverse(payload));                                //NOPMD
        rc.shutdown();
    }

    public static void usage() {
        String[] usage = {
            "usage: org.gearman.example.ReverseClient [-h<host>] [-p<port>] " +
                    "<string>",
            "\t-h<host> - job server host",
            "\t-p<port> - job server port",
            "\n\tExample: java org.gearman.example.ReverseClient Foo",
            "\tExample: java org.gearman.example.ReverseClient -h127.0.0.1 " +
                    "-p4730 Bar", //
        };

        for (String line : usage) {
            System.err.println(line);                                           //NOPMD
        }
    }
}
