/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.example;

import java.io.IOException;
import org.gearman.client.GearmanClientImpl;
import org.gearman.common.Constants;
import org.gearman.common.GearmanJobServerConnection;
import org.gearman.common.GearmanNIOJobServerConnection;
import org.gearman.util.ByteUtils;

public class EchoClient {

    private final GearmanClientImpl client;

    public EchoClient(String host, int port) throws IOException {
        GearmanJobServerConnection connection =
                new GearmanNIOJobServerConnection(host, port);
	client = new GearmanClientImpl();
	client.addJobServer(connection);
    }

    public String echo(String input) throws IOException {
        byte[] data = ByteUtils.toUTF8Bytes(input);
        byte[] respBytes = client.echo(data);
        return ByteUtils.fromUTF8Bytes(respBytes);
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

        try {
            EchoClient ec = new EchoClient(host,port);
            System.out.println(ec.echo(payload));                               //NOPMD
            ec.shutdown();
        } catch (IOException ioe) {
            ioe.printStackTrace();                                              //NOPMD
        }
    }

    public static void usage() {
        String[] usage = {
            "usage: gearmanij.example.EchoClient [-h<host>] [-p<port>] <string>",
            "\t-h<host> - job server host",
            "\t-p<port> - job server port",
            "\n\tExample: java gearmanij.example.EchoClient Foo",
            "\tExample: java gearmanij.example.EchoClient -h127.0.0.1" +
                    " -p4730 Bar", //
        };

        for (String line : usage) {
            System.err.println(line);                                           //NOPMD
        }
    }
}

