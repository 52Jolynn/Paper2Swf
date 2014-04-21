/*
 * Copyright (C) 2013 by Eric Lambert <eric.d.lambert@gmail.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.worker;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.gearman.client.GearmanIOEventListener;
import org.gearman.client.GearmanJobResult;
import org.gearman.common.GearmanPacket;
import org.gearman.common.GearmanPacketImpl;
import org.gearman.common.GearmanPacketMagic;
import org.gearman.common.GearmanPacketType;
import org.gearman.util.ByteUtils;

public abstract class AbstractGearmanFunction implements GearmanFunction {

    private static final String NULL_JOB_RESULT = "executeFunction call returned null GearmanJobResult";
    private static final byte[] NULL_JOB_RESULT_BYTES = ByteUtils
	    .toAsciiBytes(NULL_JOB_RESULT);

    protected final String name;
    protected Object data;
    protected byte[] jobHandle;
    protected Set<GearmanIOEventListener> listeners;
    protected byte [] uniqueId;

    public AbstractGearmanFunction() {
        this(null);
    }

    public AbstractGearmanFunction(String name) {
        listeners = new HashSet<GearmanIOEventListener>();
        jobHandle = new byte[0];
        if (name == null) {
            this.name = this.getClass().getCanonicalName();
        } else {
            this.name = name;
        }
    }

    public String getName() {                                                   
        return name;
    }

    public void setData(Object data) {
        this.data = data;
    }

    public void setJobHandle(byte[] handle) throws IllegalArgumentException {
        if (handle == null) {
            throw new IllegalArgumentException("handle can not be null");
        }
        if (handle.length == 0) {
            throw new IllegalArgumentException("handle can not be empty");
        }
        jobHandle = new byte[handle.length];
        System.arraycopy(handle, 0, jobHandle, 0, handle.length);
    }

    public byte[] getJobHandle() {                                              
        byte[] rt = new byte[jobHandle.length];
        System.arraycopy(jobHandle, 0, rt, 0, jobHandle.length);
        return rt;
    }

    public void registerEventListener(GearmanIOEventListener listener)
            throws IllegalArgumentException {
        if (listener == null) {
            throw new IllegalArgumentException("listener can not be null");
        }
        listeners.add(listener);
    }

    public void fireEvent(GearmanPacket event)
            throws IllegalArgumentException {
        if (event == null) {
            throw new IllegalArgumentException("event can not be null");
        }
        for (GearmanIOEventListener listener : listeners) {
            listener.handleGearmanIOEvent(event);
        }
    }

    public void sendData(byte[] data) {
        fireEvent(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.WORK_DATA,
                GearmanPacketImpl.generatePacketData(jobHandle, data)));

    }

    public void sendWarning(byte[] warning) {
        fireEvent(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.WORK_WARNING,
                GearmanPacketImpl.generatePacketData(jobHandle, warning)));
    }

    public void sendException(byte[] exception) {
        fireEvent(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.WORK_EXCEPTION,
                GearmanPacketImpl.generatePacketData(jobHandle, exception)));
    }

    public void sendStatus(int denominator, int numerator) {
        fireEvent(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                GearmanPacketType.WORK_STATUS,
                GearmanPacketImpl.generatePacketData(jobHandle,
                ByteUtils.toUTF8Bytes(String.valueOf(numerator)),
                ByteUtils.toUTF8Bytes(String.valueOf(denominator)))));
    }

    public abstract GearmanJobResult executeFunction();

    public GearmanJobResult call() {
        GearmanPacket event = null;
        GearmanJobResult result = null;
        Exception thrown = null;
        try {
            result = executeFunction();
        } catch (Exception e) {
            thrown = e;
        }
        if (result == null) {
            byte[] exceptionBytes = null;
            RuntimeException toThrow = null;
            if (thrown != null) {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                thrown.printStackTrace(new PrintStream(baos));
                exceptionBytes = baos.toByteArray();
                toThrow = new RuntimeException(thrown);
            } else {
                exceptionBytes = NULL_JOB_RESULT_BYTES;
                toThrow = new IllegalStateException(NULL_JOB_RESULT);
            }
            fireEvent(new GearmanPacketImpl(GearmanPacketMagic.REQ,
                    GearmanPacketType.WORK_EXCEPTION,
                    GearmanPacketImpl.generatePacketData(jobHandle,
                            exceptionBytes)));
            throw toThrow;
        }
        byte[] warnings = result.getWarnings();
        if (warnings.length > 0) {
            sendWarning(warnings);
        }

        if (result.jobSucceeded()) {
            event = new GearmanPacketImpl(GearmanPacketMagic.REQ,
                    GearmanPacketType.WORK_COMPLETE,
                    GearmanPacketImpl.generatePacketData(jobHandle,
                            result.getResults()));
        } else {
            event = new GearmanPacketImpl(GearmanPacketMagic.REQ,
                    GearmanPacketType.WORK_FAIL, jobHandle);

        }
        fireEvent(event);
        return result;
    }

    public void setUniqueId(byte[] uuid) {
        if (uuid == null) {
            throw new IllegalArgumentException("UUID can not be null");
        }
        this.uniqueId = Arrays.copyOf(uuid, uuid.length);
    }

    public byte[] getUniqueId() {
        if (this.uniqueId != null) {
            return Arrays.copyOf(this.uniqueId, this.uniqueId.length);
        }
        return null;
    }
}
