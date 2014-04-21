/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */

package org.gearman.worker;

import org.gearman.common.Constants;
import org.slf4j.LoggerFactory;


public class DefaultGearmanFunctionFactory implements GearmanFunctionFactory {
    private final String className;
    private final String functionName;
    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_WORKER_LOGGER_NAME);

    public DefaultGearmanFunctionFactory(String className) {
        GearmanFunction f = createFunctionInstance(className);
        if (f == null) {
            throw new IllegalStateException("Unable to create instance of " +
                    "function " + className);
        }
        this.className = className;
        String fname = f.getName();
        if (fname== null) {
            fname = className;
        } else {
            fname = fname.trim();
            if ("".equals(fname)) {
                fname = className;
            }
        }
        this.functionName = fname;
    }

    public DefaultGearmanFunctionFactory(String functionName, String className) {
        this.className = className;
        this.functionName = functionName;
    }

    public String getFunctionName() {
        return functionName;
    }

    public GearmanFunction getFunction() {
        return createFunctionInstance(className);
    }

    private static GearmanFunction createFunctionInstance(String className) {
        GearmanFunction f = null;
        try {
            Class c = Class.forName(className);
            Object o = c.newInstance();
            if (o instanceof GearmanFunction) {
                f = (GearmanFunction) o;
            } else {
                LOG.warn("Specified class " + className +
                        " is not a Gearman Function ");
            }
        } catch (Exception e) {
            LOG.warn("Unable to create instance of " +
                    "Function: " + className, e);
        }
        return f;
    }
}
