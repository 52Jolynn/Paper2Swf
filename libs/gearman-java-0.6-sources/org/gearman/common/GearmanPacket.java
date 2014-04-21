/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

/**
 * A <tt>GearmanPacket</tt> represents an indvidual message that can either be
 * sent to a Gearman Job Server or received from a Gearman Job Server.
 *
 * <pre>
 * A <tt>GearmanPacket</tt> consists of three components:
 *
 * - The Packet Magic: The component that identifies the message as a gearman
 *                     message and is used to determine if the message is a
 *                     request message (REQ) or a response message (RES).
 *
 * - The Packet Type:  The component that indentifies operations specified by
 *                     the message.
 *
 * - The Packet Data:  The payload of the message, the exact structure of which
 *                     depends on the packet type.
 * </pre>
 *
 */
public interface GearmanPacket {

    public static enum DataComponentName {
        FUNCTION_NAME,
        UNIQUE_ID,
        MINUTE,
        HOUR,
        DAY_OF_MONTH,
        MONTH,
        DAY_OF_WEEK,
        EPOCH,
        JOB_HANDLE,
        OPTION,
        KNOWN_STATUS,
        RUNNING_STATUS,
        NUMERATOR,
        DENOMINATOR,
        TIME_OUT,
        DATA,
        ERROR_CODE,
        ERROR_TEXT,
        CLIENT_ID
    }

    /**
     * Retrieves the event type for this packet.
     *
     * @return event type.
     */
    public GearmanPacketType getPacketType();

    /**
     * Retrieves the Packet as a series of bytes. Typicall called when about
     * to send the packet over a {@link  GearmanJobServerConnection}.
     *
     * @return a byte array representing the packet.
     */
    public byte [] toBytes();

    /**
     * Retrieves the payload, if any, associated with this packet.
     *
     * @return payload
     */
    public byte [] getData();

    /**
     * The data or payload of a packet can contain different set of components
     * depending on the type of packet. Clients of the packet class may want to
     * extract these components from the message payload. This method provides
     * a means for clients to extract the component without requiring knowledge
     * of the format of the payload.
     *
     * @param component The name of the component to be extracted.
     *
     * @return the value of the specified component. Should return an empty
     * array if the component is not contained the packet.
     */
    public byte [] getDataComponentValue( DataComponentName component);

    /**
     * Retrieves the magic type for this packet.
     * @return The {@link GearmanPacketMagic} for this packet.
     */
    public GearmanPacketMagic getMagic();

    /**
     * Determine if the packet represents a request message that requires a
     * responses from the server. There are certain request messages that
     * logically require a response, for example the GRAB_JOB message should
     * result in a JOB_ASSIGN or NO_JOB reply from the server. While there are
     * other messages that do not, such as CAN_DO.
     *
     * @return true if this packet represents a request message that requires a
     * response, else returns false.
     */
    public boolean requiresResponse();

}
