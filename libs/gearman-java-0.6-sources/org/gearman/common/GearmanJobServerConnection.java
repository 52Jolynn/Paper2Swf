/*
 * Copyright (C) 2009 by Eric Herman <eric@freesa.org>
 * Copyright (C) 2009 by Robert Stewart <robert@wombatnation.com>
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import java.io.IOException;

/**
 * This interface exposes the API that classes representing connections to a
 * Gearman Job Server must implement.
 */
public interface GearmanJobServerConnection {

    /**
     * Open the connection to the Gearman Job Server.
     * @throws IOException if an I/O exception was encountered.
     */
    void open() throws IOException;

    /**
     * Closes a connection to the Gearman Job Server.
     */
    void close();

    /**
     * Writes a {@link GearmanPacket} into the connection with the Gearman
     * Job Server.
     *
     * @param packet The request to be written.
     * @throws IOException if an I/O exception was encountered.
     */
    void write(GearmanPacket packet) throws IOException;

    /**
     * Reads a {@link GearmanPacket} from the connection with the Gearman
     * Job Server.
     * @return the GearmanPacket read from the connection.
     * @throws IOException if an I/O exception was encountered.
     */
    GearmanPacket read() throws IOException;

    /**
     * Is the connection open.
     *
     * @return true if the connection is open, else false.
     */
    boolean isOpen();

    /**
     * Can a read operation be executed at this time.
     *
     * @return true if a read can be executed, else returns false.
     */
    boolean canRead();

    /**
     * Can a write operation be executed at this time.
     *
     * @return true if a write can be executed, else returns false.
     */
    boolean canWrite();
}
