/*
 * Copyright (C) 2009 by Eric Lambert <Eric.Lambert@sun.com>
 * Use and distribution licensed under the BSD license.  See
 * the COPYING file in the parent directory for full text.
 */
package org.gearman.common;

import java.io.IOException;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import org.gearman.client.GearmanIOEventListener;
import org.slf4j.LoggerFactory;

public class GearmanJobServerSession
        implements GearmanSessionEventHandler, GearmanIOEventListener {

    static final String DESCRIPTION_PREFIX = "GearmanJobServerSession";
    private final String DESCRIPTION;
    private final GearmanNIOJobServerConnection connection;
    private Queue<GearmanPacket> packetsToWrite = null;
    private static final org.slf4j.Logger LOG =  LoggerFactory.getLogger(
            Constants.GEARMAN_SESSION_LOGGER_NAME);
    private SelectionKey sessionSelectionKey = null;
    private GearmanSessionEventHandler responseHandler = null;
    private Queue<GearmanTask> newTaskList = null;
    private Queue<GearmanTask> tasksAwaitingAckList = null;

    public GearmanJobServerSession(GearmanJobServerConnection conn)
            throws IllegalArgumentException {
        if (!(conn instanceof GearmanNIOJobServerConnection)) {
            throw new IllegalArgumentException("Session currently only " +
                    "supports instances of " +
                    GearmanNIOJobServerConnection.class.getName());
        }
        connection = (GearmanNIOJobServerConnection) conn;
        DESCRIPTION = DESCRIPTION_PREFIX + ":" +
                Thread.currentThread().getId() + ":" + conn.toString();
    }

    @Override
    public String toString() {
        return DESCRIPTION;
    }

    public void initSession(Selector sel, GearmanSessionEventHandler handler)
            throws IllegalStateException, IOException {
        if (isInitialized()) {
            throw new IllegalStateException("A session can not be " +
                    "initialized twice");
        }
        connection.open();
        packetsToWrite = new LinkedList<GearmanPacket>();
        sessionSelectionKey = connection.registerSelector(sel,
                SelectionKey.OP_READ);
        this.responseHandler = handler;
        newTaskList = new LinkedList<GearmanTask>();
        tasksAwaitingAckList = new LinkedList<GearmanTask>();
        LOG.info("Session " + this + " has been initialized.");      //NOPMD
    }

    public GearmanJobServerConnection getConnection() {
        return connection;
    }

    public SelectionKey getSelectionKey() {
        if (!isInitialized()) {
            throw new IllegalStateException("Session " + this +
                    " has not been initialized");
        }
        return sessionSelectionKey;
    }

    public boolean isInitialized() {
        return (connection != null && connection.isOpen());
    }

    public void waitForTasksToComplete()
            throws IllegalStateException, InterruptedException {
        if (!isInitialized()) {
            throw new IllegalStateException("Session " + this +
                    " has not been initialized");
        }
        try {
            waitForTasksToComplete(-1, TimeUnit.SECONDS);
        } catch (TimeoutException ignore) {
            //THIS SHOULD NEVER HAPPEN
            LOG.warn("Unexpected timeout exception received " +
                    "while waiting for current session task to complete. " +
                    "Timeout value was set to -1, which means do not timeout, " +
                    "yet it did. Go figure.",ignore);
        }
    }

    public void waitForTasksToComplete(long timeout, TimeUnit unit)
            throws InterruptedException, TimeoutException, IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("Session " + this +
                    " has not been initialized");
        }
        long timeOutInMills = timeout < 0 ? -1 :
            TimeUnit.MILLISECONDS.convert(timeout, unit) +
                System.currentTimeMillis();
        while ((newTaskList.size() > 0 || tasksAwaitingAckList.size() > 0) &&
                !(timeOutInMills < 0 ? false :
                    System.currentTimeMillis() > timeOutInMills)) {
            try {
                driveSessionIO();
            } catch (IOException ioe) {
                LOG.warn("Receieved an IO Exception while" +
                        " driving io for session " + this, ioe);
            }
            Thread.sleep(100);
        }
        if (newTaskList.size() > 0 || tasksAwaitingAckList.size() > 0) {
            throw new TimeoutException("Session " + this + " timed out " +
                    "waiting for all requests to complete");
        }
    }

    public void closeSession() {
        if (!isInitialized()) {
            LOG.warn("Attempted to close a session that is not open: " +
                    toString());
            return;
        }
        LOG.info("Session " + this + " is being closed.");
        sessionSelectionKey.cancel();
        connection.close();
        packetsToWrite.clear();
        packetsToWrite = null;
        tasksAwaitingAckList.clear();
        newTaskList.clear();
        LOG.info("Session " + this + " has successfully closed.");
    }

    public void submitTask(GearmanTask task)
            throws IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("Session hasnt been initialized." +
                    " Request may not be submitted at this time");
        }
        if (task == null) {
            throw new IllegalStateException("A null request can not be" +
                    " submitted to a server");
        }

        if (!task.getState().equals(GearmanTask.State.NEW)) {
            throw new IllegalStateException("Invalid task state: " +
                    task.getState());
        }

        newTaskList.add(task);
        packetsToWrite.add(task.getRequestPacket());
        sessionSelectionKey.interestOps(sessionSelectionKey.interestOps() |
                SelectionKey.OP_WRITE);
        LOG.info( "Session " + this + " is now handling " +
                "the task " + task);
    }

    public void handleGearmanIOEvent(GearmanPacket event)
            throws IllegalArgumentException {
        if (event == null) {
            throw new IllegalArgumentException("Can not handle a null event");
        }
        if (event.getMagic().equals(GearmanPacketMagic.RES)) {
            throw new IllegalArgumentException("Can not handle a Result event");
        }

        GearmanTask t = new GearmanTask(event);
        submitTask(t);
    }

    public void driveSessionIO()
            throws IOException, GearmanException, IllegalStateException {
        GearmanPacket p = null;
        if (!isInitialized()) {
            throw new IllegalStateException("you can not driveSessionIO on an" +
                    " un-initialized session: " + toString());
        }

        while (sessionHasDataToWrite() && canWrite()) {
            if (packetsToWrite.isEmpty()) {
                connection.write(null);
            } else {
                p = packetsToWrite.remove();
                connection.write(p);
                handleSessionEvent(new GearmanSessionEvent(p, this));           //NOPMD
            }
        }
        if (!sessionHasDataToWrite()) {
            sessionSelectionKey.interestOps(SelectionKey.OP_READ);
        }

        while (canRead()) {
            p = connection.read();
            if (p == null) {
                continue;
            }
            handleSessionEvent(new GearmanSessionEvent(p, this));               //NOPMD
        }
    }

    @Override
    public boolean equals(Object that) {
        if (that == null) {
            return false;
        }

        if (!(that instanceof GearmanJobServerSession)) {
            return false;
        }

        GearmanJobServerSession thatSession = (GearmanJobServerSession) that;

        return this.connection.equals(thatSession.connection);
    }

    @Override
    public int hashCode() {
        return connection.hashCode();
    }

    public void handleSessionEvent(GearmanSessionEvent event)
            throws IllegalArgumentException, IllegalStateException {
        if (event == null) {
            throw new IllegalArgumentException("Event can not be null");
        }
        GearmanPacket p = event.getPacket();
        if (p == null) {
            throw new IllegalArgumentException("Event does not have a packet");
        }
        GearmanPacketMagic m = p.getMagic();
        if (m.equals(GearmanPacketMagic.REQ)) {
            handleReqSessionEvent(event);
        } else if (m.equals(GearmanPacketMagic.RES)) {
            handleResSessionEvent(event);
        } else {
            throw new IllegalStateException("Event has bad magic type " + m);
        }
    }

    public int getNumberOfActiveTasks() throws IllegalStateException {
        if (!isInitialized()) {
            throw new IllegalStateException("Session hasnt been initialized.");
        }
        return tasksAwaitingAckList.size() + newTaskList.size();
    }

    public boolean sessionHasDataToWrite() {
        if (connection == null || !connection.isOpen()) {
            return false;
        }
        return packetsToWrite.isEmpty() ? connection.hasBufferedWriteData() : true;
    }

    private void handleReqSessionEvent(GearmanSessionEvent event) 
            throws IllegalStateException {
        GearmanPacket p = event.getPacket();
        GearmanTask t = newTaskList.remove();
        if (t == null) {
            String msg = "Session has received  request event " + event.packet +
                    " but has no task in new task queue.";
            LOG.error(msg);
            throw new IllegalStateException(msg);
        }
        t.handleGearmanIOEvent(p);
        GearmanTask.State state = t.getState();
        LOG.info("Session " + this + " handling a " +
                GearmanPacketMagic.REQ + "/" + p.getPacketType() + " event");
        switch (state) {
            case SUBMITTED:
                tasksAwaitingAckList.add(t);
                LOG.info("Added task " + t.getRequestPacket().getPacketType() +
                        " to taskAwaiting list. List size = " +
                        tasksAwaitingAckList.size() + "( Event was " + p.getPacketType() + ")");
                break;
            case FINISHED:
                break;
            default:
                String code = "000";
                String msg = "Task in invalid state (State = " + state +
                        ") after submission to server";
                LOG.warn(msg);
                responseHandler.handleSessionEvent(new GearmanSessionEvent(
                        new GearmanPacketImpl(
                        GearmanPacketMagic.RES, GearmanPacketType.ERROR,
                        GearmanPacketImpl.generatePacketData(code.getBytes(),
                        msg.getBytes())), this));
        }

    }

    private void handleResSessionEvent(GearmanSessionEvent event) {
        GearmanPacket p = event.getPacket();
        GearmanPacketType t = p.getPacketType();
        GearmanTask task = tasksAwaitingAckList.peek();
        GearmanPacketType taskType = null;
                LOG.info("Session " + this + " handling a " +
                GearmanPacketMagic.RES + "/" + p.getPacketType() + " event");
        switch (t) {
            case JOB_CREATED:
                taskType = task.getRequestPacket().getPacketType();
                if (GearmanPacketType.isJobSubmission(taskType)) {
                    task.handleGearmanIOEvent(p);
                    responseHandler.handleSessionEvent(event);
                } else {
                    handleTypeMismatch("Job Submission", taskType.toString());
                }
                break;
            case NO_JOB:
                taskType = task.getRequestPacket().getPacketType();
                if (taskType.equals(GearmanPacketType.GRAB_JOB) ||
                        taskType.equals(GearmanPacketType.GRAB_JOB_UNIQ)) {
                    task.handleGearmanIOEvent(p);
                } else {
                    handleTypeMismatch(GearmanPacketType.GRAB_JOB + " or " +
                            GearmanPacketType.GRAB_JOB_UNIQ,
                            taskType.toString());
                }
                break;
            case NOOP:
                if (task != null) {
                    taskType = task.getRequestPacket().getPacketType();
                    if (taskType.equals(GearmanPacketType.PRE_SLEEP)) {
                        task.handleGearmanIOEvent(p);
                    } else {
                        return;
                    }
                }
                break;
            case JOB_ASSIGN:
                taskType = task.getRequestPacket().getPacketType();
                if (taskType.equals(GearmanPacketType.GRAB_JOB)) {
                    task.handleGearmanIOEvent(p);
                } else {
                    handleTypeMismatch(GearmanPacketType.GRAB_JOB.toString(),
                            taskType.toString());
                }
                break;
            case JOB_ASSIGN_UNIQ:
                taskType = task.getRequestPacket().getPacketType();
                if (taskType.equals(GearmanPacketType.GRAB_JOB_UNIQ)) {
                    task.handleGearmanIOEvent(p);
                } else {
                    handleTypeMismatch(GearmanPacketType.GRAB_JOB_UNIQ.toString(),
                            taskType.toString());
                }
                break;
            case STATUS_RES:
                taskType = task.getRequestPacket().getPacketType();
                if (taskType.equals(GearmanPacketType.GET_STATUS)) {
                    task.handleGearmanIOEvent(p);
                } else {
                    handleTypeMismatch(GearmanPacketType.GET_STATUS.toString(),
                            taskType.toString());
                }
                break;
            case ECHO_RES:
                taskType = task.getRequestPacket().getPacketType();
                if (taskType.equals(GearmanPacketType.ECHO_REQ)) {
                    task.handleGearmanIOEvent(p);
                } else {
                    handleTypeMismatch(GearmanPacketType.ECHO_REQ.toString(),
                            taskType.toString());
                }
                break;
            case OPTION_RES:
                taskType = task.getRequestPacket().getPacketType();
                if (taskType.equals(GearmanPacketType.OPTION_REQ)) {
                    task.handleGearmanIOEvent(p);
                } else {
                    handleTypeMismatch(GearmanPacketType.OPTION_REQ.toString(),
                            taskType.toString());
                }
                break;
            default:
                responseHandler.handleSessionEvent(event);
                return;
        }

        if (task != null ) {
            if (task.getState().compareTo(GearmanTask.State.SUBMITTED) > 0) {
                tasksAwaitingAckList.remove();
                LOG.info("Removed task " + task.getRequestPacket().getPacketType() +
                        " from taskAwaiting list. List size = " +
                        tasksAwaitingAckList.size() + "( Event was " +
                        event.packet.getPacketType() + ")");
            } else {
                LOG.warn("Task " + task + " still in submitted " +
                        "state after receiving acknowlegement from server. " +
                        "Ack = " + p);

            }
        }

    }

    private void handleTypeMismatch(String expected, String got) {
        String code = "000";
        String msg = "Received " + got + " response from server," +
                " but last request was not " + expected;
        LOG.warn(msg);
        responseHandler.handleSessionEvent(
                new GearmanSessionEvent(new GearmanPacketImpl(
                GearmanPacketMagic.RES,
                GearmanPacketType.ERROR,
                GearmanPacketImpl.generatePacketData(code.getBytes(),
                msg.getBytes())), this));
    }

    private boolean canWrite() {
        if (connection == null) {
            return false;
        }
        return connection.canWrite();
    }

    private boolean canRead() {
        if (connection == null) {
            return false;
        }
        return connection.canRead();
    }

}
