/*
 * See the NOTICE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.xwiki.eventstream.internal;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.phase.Initializable;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.eventstream.events.EventStreamDeletedEvent;
import org.xwiki.eventstream.internal.events.EventStatusAddOrUpdatedEvent;
import org.xwiki.eventstream.internal.events.EventStatusDeletedEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Helper to implement asynchronous writing of events.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public abstract class AbstractAsynchronousEventStore implements EventStore, Initializable
{
    /**
     * The type of task.
     * 
     * @version $Id$
     */
    protected enum EventStoreTaskType
    {
        SAVE_EVENT,

        SAVE_EVENT_STATUS,

        DELETE_EVENT,

        DELETE_EVENT_BY_ID,

        DELETE_STATUS,

        STOP_THREAD
    }

    /**
     * A queued store task.
     * 
     * @param <O> the return type of the task
     * @param <I> the input type of the task
     * @version $Id$
     */
    protected class EventStoreTask<O, I>
    {
        private final CompletableFuture<O> future;

        private final I input;

        private final EventStoreTaskType type;

        private O output;

        protected EventStoreTask(I input, EventStoreTaskType type)
        {
            this.input = input;
            this.type = type;

            this.future = new CompletableFuture<>();
        }

        /**
         * @return the input the input
         */
        public I getInput()
        {
            return this.input;
        }

        /**
         * @return the type
         */
        public EventStoreTaskType getType()
        {
            return this.type;
        }
    }

    @Inject
    protected Logger logger;

    @Inject
    protected ComponentDescriptor<EventStore> descriptor;

    @Inject
    protected ObservationManager observation;

    private BlockingQueue<EventStoreTask<?, ?>> queue;

    private boolean notifyEach;

    private boolean notifyAll;

    private <O, I> CompletableFuture<O> addTask(I input, EventStoreTaskType type)
    {
        EventStoreTask<O, I> task = new EventStoreTask<>(input, type);

        try {
            this.queue.put(task);
        } catch (InterruptedException e) {
            task.future.completeExceptionally(e);

            Thread.currentThread().interrupt();
        }

        return task.future;
    }

    @Override
    public CompletableFuture<Event> saveEvent(Event event)
    {
        return addTask(event, EventStoreTaskType.SAVE_EVENT);
    }

    @Override
    public CompletableFuture<EventStatus> saveEventStatus(EventStatus status)
    {
        return addTask(status, EventStoreTaskType.SAVE_EVENT_STATUS);
    }

    @Override
    public CompletableFuture<Optional<Event>> deleteEvent(String eventId)
    {
        return addTask(eventId, EventStoreTaskType.DELETE_EVENT_BY_ID);
    }

    @Override
    public CompletableFuture<Optional<Event>> deleteEvent(Event event)
    {
        return addTask(event, EventStoreTaskType.DELETE_EVENT);
    }

    @Override
    public CompletableFuture<Optional<EventStatus>> deleteEventStatus(EventStatus status)
    {
        return addTask(status, EventStoreTaskType.DELETE_STATUS);
    }

    private void run()
    {
        boolean stop = false;

        while (!stop) {
            EventStoreTask<?, ?> firstTask;
            try {
                firstTask = this.queue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The thread handling asynchronous storage for event store [{}] has been interrupted",
                    this.descriptor.getRoleHint(), e);

                Thread.currentThread().interrupt();
                break;
            }

            stop = processTasks(firstTask);
        }
    }

    private boolean processTasks(EventStoreTask<?, ?> firstTask)
    {
        List<EventStoreTask<?, ?>> tasks = new ArrayList<>();
        try {
            for (EventStoreTask<?, ?> task = firstTask; task != null; task = this.queue.poll()) {
                try {
                    if (processTask(task)) {
                        return true;
                    }
                } catch (Exception e) {
                    task.future.completeExceptionally(e);
                }

                tasks.add(task);
            }
        } finally {
            afterTasks(tasks);
        }

        return false;
    }

    private boolean processTask(EventStoreTask<?, ?> task) throws EventStreamException
    {
        switch (task.type) {
            case DELETE_EVENT:
                processTaskOutput((EventStoreTask<Optional<Event>, Event>) task, syncDeleteEvent((Event) task.input));
                break;

            case DELETE_EVENT_BY_ID:
                processTaskOutput((EventStoreTask<Optional<Event>, Event>) task, syncDeleteEvent((String) task.input));
                break;

            case SAVE_EVENT:
                processTaskOutput((EventStoreTask<Event, Event>) task, syncSaveEvent((Event) task.input));
                break;

            case DELETE_STATUS:
                processTaskOutput((EventStoreTask<Optional<EventStatus>, EventStatus>) task,
                    syncDeleteEventStatus((EventStatus) task.input));
                break;

            case SAVE_EVENT_STATUS:
                processTaskOutput((EventStoreTask<EventStatus, EventStatus>) task,
                    syncSaveEventStatus((EventStatus) task.input));
                break;

            case STOP_THREAD:
                // Stop the thread
                return true;

            default:
                break;
        }

        return false;
    }

    private <O, I> void processTaskOutput(EventStoreTask<O, I> task, O output)
    {
        task.output = output;

        if (this.notifyEach) {
            // Complete and notify right away
            task.future.complete(output);

            notify(task);
        }
    }

    private void notify(EventStoreTask<?, ?> task)
    {
        switch (task.type) {
            case DELETE_EVENT:
                this.observation.notify(new EventStreamDeletedEvent(), task.output);
                break;

            case DELETE_EVENT_BY_ID:
                this.observation.notify(new EventStreamDeletedEvent(), task.output);
                break;

            case SAVE_EVENT:
                this.observation.notify(new EventStreamAddedEvent(), task.output);
                break;

            case DELETE_STATUS:
                this.observation.notify(new EventStatusDeletedEvent(), task.output);
                break;

            case SAVE_EVENT_STATUS:
                this.observation.notify(new EventStatusAddOrUpdatedEvent(), task.output);
                break;

            default:
                break;
        }
    }

    /**
     * @param the event status to save
     */
    protected abstract EventStatus syncSaveEventStatus(EventStatus status) throws EventStreamException;

    /**
     * @param the event to save
     */
    protected abstract Event syncSaveEvent(Event event) throws EventStreamException;

    /**
     * @param the event status to save
     */
    protected abstract Optional<EventStatus> syncDeleteEventStatus(EventStatus status) throws EventStreamException;

    /**
     * @param the id of the event to delete
     */
    protected abstract Optional<Event> syncDeleteEvent(String eventId) throws EventStreamException;

    /**
     * @param event the event to delete
     */
    protected abstract Optional<Event> syncDeleteEvent(Event event) throws EventStreamException;

    protected void afterTasks(List<EventStoreTask<?, ?>> tasks)
    {
        if (this.notifyAll) {
            for (EventStoreTask task : tasks) {
                task.future.complete(task.output);
                notify(task);
            }
        }
    }

    protected void initialize(int queueSize, boolean notifyEach, boolean notifyAll)
    {
        this.notifyEach = notifyEach;
        this.notifyAll = !notifyEach && notifyAll;

        this.queue = new LinkedBlockingQueue<>(queueSize);

        Thread thread = new Thread(this::run);
        thread.setName("Asynchronous handler for event store [" + descriptor.getRoleHint() + "]");
        thread.start();
        thread.setPriority(Thread.NORM_PRIORITY - 1);
    }
}
