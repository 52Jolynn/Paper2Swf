/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

public class GearmanException extends RuntimeException {

    public static final long serialVersionUID = 1L;

    public GearmanException(String msg) {
        super(msg);
    }

    public GearmanException() {
        super();
    }
}
