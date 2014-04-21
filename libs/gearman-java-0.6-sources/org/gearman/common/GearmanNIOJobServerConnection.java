/*
 * Copyright (C) 2012 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import java.io.BufferedInputStream;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.net.InetSocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.channels.SocketChannel;

import org.slf4j.LoggerFactory;

public class GearmanNIOJobServerConnection
        implements GearmanJobServerIpConnection {

    static final String DESCRIPTION_PREFIX = "GearmanNIOJobServerConnection";
    private final String DESCRIPTION;
    private final String host;
    private final int port;
    private final InetSocketAddress remote;
    private SocketChannel serverConnection = null;
    private Selector selector = null;
    private SelectionKey selectorKey = null;
    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_CLIENT_LOGGER_NAME);
    private ByteBuffer bytesReceived;
    private ByteBuffer bytesToSend;

    public GearmanNIOJobServerConnection(String hostname)
            throws IllegalArgumentException {
        this(hostname, Constants.GEARMAN_DEFAULT_TCP_PORT);
    }

    public GearmanNIOJobServerConnection(String hostname, int port)
            throws IllegalArgumentException {
        this(new InetSocketAddress(hostname, port));
    }

    public GearmanNIOJobServerConnection(InetSocketAddress remote)
            throws IllegalArgumentException {
        if (remote == null) {
            throw new IllegalArgumentException("Remote can not be null");
        }
        this.remote = remote;
        this.host = remote.getHostName();
        this.port = remote.getPort();
        bytesReceived = ByteBuffer.allocate(
                Constants.GEARMAN_DEFAULT_SOCKET_RECV_SIZE);
        bytesToSend = ByteBuffer.allocate(
                Constants.GEARMAN_DEFAULT_SOCKET_SEND_SIZE);
        DESCRIPTION = DESCRIPTION_PREFIX + ":" + remote.toString();
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    public void open() throws IOException {
        if (isOpen()) {
            throw new IllegalStateException("A session can not be " +
                    "initialized twice");
        }
        try {
            serverConnection = SocketChannel.open(remote);
            serverConnection.socket().setTcpNoDelay(true);
            serverConnection.socket().setSoLinger(true,
                    Constants.GEARMAN_DEFAULT_SOCKET_TIMEOUT);
            serverConnection.socket().setSoTimeout(
                    Constants.GEARMAN_DEFAULT_SOCKET_TIMEOUT * 1000);
            serverConnection.socket().setReceiveBufferSize(
                    Constants.GEARMAN_DEFAULT_SOCKET_RECV_SIZE);
            serverConnection.socket().setSendBufferSize(
                    Constants.GEARMAN_DEFAULT_SOCKET_RECV_SIZE);
            serverConnection.configureBlocking(false);
            serverConnection.finishConnect();
            selector = Selector.open();
            selectorKey = serverConnection.register(selector,
                    SelectionKey.OP_WRITE | SelectionKey.OP_READ);
        } catch (IOException ioe) {
//            LOG.warn("Received IOException while attempting to" +
//                    " initialize session " + this +
//                    ". Shuting down session", ioe);
            if (serverConnection != null && serverConnection.isOpen()) {
                if (selector != null && selector.isOpen()) {
                    try {
                        selector.close();
                    } catch (IOException selioe) {
                        LOG.warn("Received IOException while" +
                                " attempting to close selector.", selioe);
                    }
                }
                try {
                    serverConnection.close();
                } catch (IOException closeioe) {
                    LOG.warn("Received IOException while" +
                            " attempting to close connection to server. " +
                            "Giving up!", closeioe);
                }
            }
            throw ioe;
        }
        LOG.info("Connection " + this + " has been opened");
    }

    public void close() {
        if (!isOpen()) {
            throw new IllegalStateException("Can not close a session that " +
                    "has not been initialized");
        }
        LOG.info( "Session " + this + " is being closed.");
        selectorKey.cancel();
        try {
            selector.close();
        } catch (IOException ioe) {
            LOG.warn("Received IOException while attempting to " +
                    "close selector attached to session " + this, ioe);
        } finally {
            try {
                serverConnection.close();
            } catch (IOException cioe) {
                LOG.warn("Received IOException while attempting" +
                        " to close connection for session " + this, cioe);
            }
            serverConnection = null;
        }
        LOG.info( "Connection " + this + " has successfully closed.");
    }

    public void write(GearmanPacket request) throws IOException {

        if (request == null && bytesToSend.position() == 0) {
            return;
        }
        if (request != null) {
            int ps = request.getData().length +
                    Constants.GEARMAN_PACKET_HEADER_SIZE;
            if (bytesToSend.remaining() < ps) {
                int newCapacity = bytesToSend.capacity() * 2;
                while (newCapacity < ps && newCapacity > 0) {
                    newCapacity *=2;
                }
                bytesToSend = growBuffer(bytesToSend, newCapacity);
            }
            byte[] bytes = request.toBytes();
            ByteBuffer bb = ByteBuffer.allocate(bytes.length);
            bb.put(bytes);
            bb.rewind();
            bytesToSend.put(bb);
        }
        selector.selectNow();
        if (selectorKey.isWritable()) {
            //Lets never write more than DEFAULT_SOCKET_SEND_SIZE, so if the
            //buffersize is larger than this, set the limit to default
            int newLimit = bytesToSend.position();
            int oldLimit = newLimit;
            if (newLimit > Constants.GEARMAN_DEFAULT_SOCKET_SEND_SIZE) {
                newLimit = Constants.GEARMAN_DEFAULT_SOCKET_SEND_SIZE;
            }
            bytesToSend.limit(newLimit);
            bytesToSend.rewind();
            int bytesSent = serverConnection.write(bytesToSend);
            bytesToSend.limit(oldLimit);
            bytesToSend.compact();
            LOG.debug("Write command wrote " + bytesSent + " to " +
                    this + ". " + bytesToSend.position() + " bytes left in " +
                    "send buffer");
        } else {
            LOG.debug("Write command can not write request: " +
                    "Selector for " + this + " is not available for write. " +
                    "Will buffer request and send it later.");
        }
    }

    public GearmanPacket read() throws IOException {
        GearmanPacket returnPacket = null;
        selector.selectNow();
        if (selectorKey.isReadable()) {
            if (!bytesReceived.hasRemaining()) {
                bytesReceived = growBuffer(bytesReceived);
            }
            int bytesRead = serverConnection.read(bytesReceived);
            if (bytesRead >= 0) {
                LOG.debug( "Session " + this + " has read " +
                        bytesRead + " bytes from its job server. Buffer has " +
                        bytesReceived.remaining());
            } else {
        	    if (isOpen()) {
        	        close();
                }
                //TODO do something smarter here
                throw new IOException("Connection to job server severed");
            }
        } else {
            LOG.debug("Read command can not read request from" +
                    "session: Selector for " + this + " is not available " +
                    "for read. ");
        }
        if (bufferContainsCompletePacket(bytesReceived)) {
            byte[] pb = new byte[getSizeOfPacket(bytesReceived)];
            bytesReceived.limit(bytesReceived.position());
            bytesReceived.rewind();
            bytesReceived.get(pb);
            bytesReceived.compact();
            returnPacket = new GearmanPacketImpl(new BufferedInputStream(
                    new ByteArrayInputStream(pb)));
        }
        return returnPacket;
    }

    public SelectionKey registerSelector(Selector s, int mask)
            throws IOException {
        return serverConnection.register(s, mask);

    }

    public boolean canRead() {
        if (!selector.isOpen()) {
            return false;
        }
        try {
            selector.selectNow();
        } catch (IOException ioe) {
            LOG.warn("Failed to select on connection " +
                    this, ioe);
        }
        return (selectorKey.isReadable() ||
                bufferContainsCompletePacket(bytesReceived));
    }

    public boolean canWrite() {
        if (!selector.isOpen()) {
            return false;
        }
        try {
            selector.selectNow();
        } catch (IOException ioe) {
            LOG.warn("Connection Failed to select on socket " +
                    this, ioe);
        }
        return (selectorKey.isWritable());
    }

    public boolean hasBufferedWriteData() {
        return bytesToSend.position() > 0;
    }

    public Selector getSelector() {
        return selector;
    }

    public boolean isOpen() {
        return (serverConnection != null && serverConnection.isConnected());
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }

        if (this == that) {
            return true;
        }

        if (!(that instanceof GearmanNIOJobServerConnection)) {
            return false;
        }

        InetSocketAddress thatRemote = 
                ((GearmanNIOJobServerConnection) that).remote;

        return this.remote.equals(thatRemote);
    }

    // When you override equals you should override hashcode as well, since
    // two equal objects should have the same hashcode
    @Override
    public int hashCode() {
        return this.remote == null ? 0 : this.remote.hashCode();
    }

    private boolean bufferContainsCompletePacket(ByteBuffer b) {
        if (b.position() < Constants.GEARMAN_PACKET_HEADER_SIZE) {
            return false;
        }
        return b.position() >= getSizeOfPacket(b) ? true : false;
    }

    // DO NOT CALL UNLESS YOU ARE SURE THAT BYTEBUFFER HAS AT LEAST
    // GEARMAN_PACKET_HEADER_SIZE BYTES!
    private int getSizeOfPacket(ByteBuffer buffer) {
        int originalPosition = buffer.position();
        byte[] header = new byte[Constants.GEARMAN_PACKET_HEADER_SIZE];
        buffer.rewind();
        buffer.get(header);
        buffer.position(originalPosition);
        GearmanPacketHeader ph = new GearmanPacketHeader(header);
        return ph.getDataLength() + Constants.GEARMAN_PACKET_HEADER_SIZE;
    }

    private ByteBuffer growBuffer(ByteBuffer originalBuffer)
            throws IllegalArgumentException {
        return growBuffer(originalBuffer, originalBuffer.capacity() * 2);
    }

    private ByteBuffer growBuffer(ByteBuffer orginalBuffer, int newCapacity)
            throws IllegalArgumentException{
        if (newCapacity < orginalBuffer.capacity()) {
            throw new IllegalArgumentException("The new capacity of the " +
                    "buffer (" + newCapacity + ") may not be less than the" +
                    " orginal capacity (" + orginalBuffer.capacity()+")");
        }
        orginalBuffer.flip();
        ByteBuffer newBuffer = ByteBuffer.allocate(newCapacity);
        newBuffer.put(orginalBuffer);
        return newBuffer;


    }

	public String getHost() {
        return host;
	}

	public int getPort() {
        return port;
	}
}
