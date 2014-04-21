/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import java.util.UUID;

import org.gearman.client.GearmanIOEventListener;
import org.slf4j.LoggerFactory;

public class GearmanTask implements GearmanIOEventListener {

    public static enum State {

        NEW, SUBMITTED, RUNNING, FINISHED, EXCEPTION
    }
    static final String DESCRIPTION_PREFIX = "GearmanTask";
    private final String DESCRIPTION;
    private State state = null;
    private GearmanPacket requestPacket = null;
    private GearmanServerResponseHandler handler = null;
    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_JOB_LOGGER_NAME);

    public GearmanTask(GearmanPacket packet) {
        this(null, packet);
    }

    public GearmanTask(GearmanServerResponseHandler handler,
            GearmanPacket packet) {
        if (packet == null) {
            throw new IllegalArgumentException("The request can not be null");
        }

        if (!packet.getMagic().equals(GearmanPacketMagic.REQ)) {
            throw new IllegalArgumentException("Invalid request. The packet " +
                    "is not a request packet");
        }

        state = State.NEW;
        UUID uuid = UUID.randomUUID();
        requestPacket = packet; //TODO make a copy of the packet or make sure the packet is hardened against tampering
        this.handler = handler;
        DESCRIPTION = DESCRIPTION_PREFIX + ":" + handler + ":" + uuid;

    }

    public void handleGearmanIOEvent(GearmanPacket p)
            throws IllegalArgumentException {
        if (p == null) {
            throw new IllegalArgumentException("You can not add a null" +
                    " response packet");
        }

        boolean cont = true;
        while (cont) {
            //Validate packet
            switch (state) {

                // request needs to be submitted to server
                case NEW:
                    if (p.getMagic().equals(GearmanPacketMagic.REQ)) {
                        if (requestPacket.requiresResponse()) {
                            changeState(State.SUBMITTED);
                        } else {
                            changeState(State.FINISHED);
                        }
                    }
                    cont = false;
                    break;

                case SUBMITTED:
                    if (p.getMagic().equals(GearmanPacketMagic.RES)) {
                        changeState(State.RUNNING);
                    } else {
                        cont = false;
                    }
                    break;

                case RUNNING:
                    if (p.getMagic().equals(GearmanPacketMagic.RES)) {
                        if (handler == null) {
                            LOG.warn("ServerRequest requires " +
                                    "response, but not response handler was " +
                                    "provided for the request. Request = " +
                                    this);
                            changeState(State.EXCEPTION);
                        } else {
                            try {
                                handler.handleEvent(p);
                                if (handler.isDone()) {
                                    changeState(State.FINISHED);
                                } else {
                                    cont = false;
                                }
                            } catch (GearmanException ge) {
                                changeState(State.EXCEPTION);
                            }
                        }
                    } else {
                        cont = false;
                    }
                    break;

                case FINISHED:
                    cont = false;
                    break;

                case EXCEPTION:
                    throw new GearmanException("Encountered Fatal Exception" +
                            " while driving the following ServerRequest: " +
                            requestPacket);

                default:
                    throw new GearmanException("Unknown Request state " +
                            state + " for request " + requestPacket);
            }
        }
    }

    public GearmanPacket getRequestPacket() {
        return requestPacket;
    }

    public GearmanServerResponseHandler getHandler() {
        return handler;
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    public State getState() {
        return state;
    }

    private void changeState(State newState) {
        if (!newState.equals(state)) {
            LOG.debug("Request " + this + " is changing state" +
                    " from " + this.state + " to " + newState);
            this.state = newState;
        }
    }
}
