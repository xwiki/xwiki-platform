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

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.LinkedBlockingQueue;

import javax.inject.Inject;

import org.slf4j.Logger;
import org.xwiki.component.descriptor.ComponentDescriptor;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.concurrent.ContextStoreManager;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStatus;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.events.EventStreamAddedEvent;
import org.xwiki.eventstream.events.EventStreamDeletedEvent;
import org.xwiki.eventstream.events.MailEntityAddedEvent;
import org.xwiki.eventstream.events.MailEntityDeleteEvent;
import org.xwiki.eventstream.internal.events.EventStatusAddOrUpdatedEvent;
import org.xwiki.eventstream.internal.events.EventStatusDeletedEvent;
import org.xwiki.observation.ObservationManager;

/**
 * Helper to implement asynchronous writing of events.
 * 
 * @version $Id$
 * @since 12.5RC1
 */
public abstract class AbstractAsynchronousEventStore implements EventStore, Initializable, Disposable
{
    private static final List<String> CONTEXT_ENTRIES = Arrays.asList("user", "author", "wiki");

    /**
     * The type of task.
     * 
     * @version $Id$
     */
    protected enum EventStoreTaskType
    {
        SAVE_EVENT,

        /**
         * @since 12.6
         */
        SAVE_STATUS,

        /**
         * @since 12.6
         */
        SAVE_MAIL_ENTITY,

        DELETE_EVENT,

        DELETE_EVENT_BY_ID,

        DELETE_STATUS,

        /**
         * @since 12.6
         */
        DELETE_STATUSES,

        /**
         * @since 12.6
         */
        DELETE_MAIL_ENTITY,

        /**
         * @since 12.6
         */
        PREFILTER_EVENT
    }

    /**
     * A queued store task.
     * 
     * @param <O> the return type of the task
     * @param <I> the input type of the task
     * @version $Id$
     */
    protected static class EventStoreTask<O, I>
    {
        /**
         * An order to stop the processing thread.
         */
        public static final EventStoreTask<Object, Object> STOP = new EventStoreTask<>(null, null, null);

        private final CompletableFuture<O> future;

        private final I input;

        private final EventStoreTaskType type;

        private final Map<String, Serializable> context;

        private O output;

        protected EventStoreTask(I input, EventStoreTaskType type, Map<String, Serializable> contextStore)
        {
            this.input = input;
            this.type = type;
            this.context = contextStore;

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

    private static class DeleteStatusesData
    {
        private final String entityId;

        private final Date date;

        DeleteStatusesData(String entityId, Date date)
        {
            this.entityId = entityId;
            this.date = date;
        }
    }

    @Inject
    protected Logger logger;

    @Inject
    protected ComponentDescriptor<EventStore> descriptor;

    @Inject
    protected ObservationManager observation;

    @Inject
    private ContextStoreManager contextStore;

    @Inject
    private Execution execution;

    private Thread thread;

    private int queueCapacity;

    private BlockingQueue<EventStoreTask<?, ?>> queue;

    private boolean notifyEach;

    private boolean notifyAll;

    private boolean disposed;

    /**
     * Give an estimation of the number of events that are going to be added to the store. Can be negative if there is
     * more deletes than add.
     * 
     * @return the current number of events to add to the store
     * @since 12.8RC1
     * @since 12.7.1
     * @since 12.6.2
     */
    public int getQueueSize()
    {
        int size = 0;
        for (EventStoreTask<?, ?> task : this.queue) {
            switch (task.type) {
                case DELETE_EVENT:
                case DELETE_EVENT_BY_ID:
                    --size;
                    break;

                case SAVE_EVENT:
                    ++size;
                    break;

                default:
                    break;
            }
        }

        return size;
    }

    private <O, I> CompletableFuture<O> addTask(I input, EventStoreTaskType type)
    {
        // Remember a few standard things from the context
        Map<String, Serializable> context;
        try {
            context = this.contextStore.save(CONTEXT_ENTRIES);
        } catch (ComponentLookupException e) {
            this.logger.error("Failed to save context of the event", e);

            context = null;
        }

        EventStoreTask<O, I> task = new EventStoreTask<>(input, type, context);

        addTask(task);

        return task.future;
    }

    private <O, I> void addTask(EventStoreTask<O, I> task)
    {
        try {
            this.queue.put(task);
        } catch (InterruptedException e) {
            task.future.completeExceptionally(e);

            Thread.currentThread().interrupt();
        }
    }

    @Override
    public CompletableFuture<Event> saveEvent(Event event)
    {
        return addTask(event, EventStoreTaskType.SAVE_EVENT);
    }

    @Override
    public CompletableFuture<EventStatus> saveEventStatus(EventStatus status)
    {
        return addTask(status, EventStoreTaskType.SAVE_STATUS);
    }

    @Override
    public CompletableFuture<EventStatus> saveMailEntityEvent(EntityEvent event)
    {
        return addTask(event, EventStoreTaskType.SAVE_MAIL_ENTITY);
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

    @Override
    public CompletableFuture<Void> deleteEventStatuses(String entityId, Date date)
    {
        return addTask(new DeleteStatusesData(entityId, date), EventStoreTaskType.DELETE_STATUSES);
    }

    @Override
    public CompletableFuture<Optional<EventStatus>> deleteMailEntityEvent(EntityEvent event)
    {
        return addTask(event, EventStoreTaskType.DELETE_MAIL_ENTITY);
    }

    @Override
    public CompletableFuture<Event> prefilterEvent(Event event)
    {
        return addTask(event, EventStoreTaskType.PREFILTER_EVENT);
    }

    private void run()
    {
        while (!this.disposed) {
            EventStoreTask<?, ?> firstTask;
            try {
                firstTask = this.queue.take();
            } catch (InterruptedException e) {
                this.logger.warn("The thread handling asynchronous storage for event store [{}] has been interrupted",
                    this.descriptor.getRoleHint(), e);

                Thread.currentThread().interrupt();
                break;
            }

            processTasks(firstTask);
        }
    }

    private void processTasks(EventStoreTask<?, ?> firstTask)
    {
        this.execution.setContext(new ExecutionContext());

        // Make sure to not treat more than the queue capacity in a single batch
        List<EventStoreTask<?, ?>> tasks = new ArrayList<>(this.queueCapacity);
        try {
            for (EventStoreTask<?, ?> task = firstTask; task != null; task = this.queue.poll()) {
                if (task == EventStoreTask.STOP) {
                    break;
                }

                try {
                    // Execute the task
                    processTask(task);

                    // Add a successful task to the batch
                    tasks.add(task);

                    // Stop if the batch has been reached
                    if (tasks.size() == this.queueCapacity) {
                        break;
                    }
                } catch (Exception e) {
                    // Indicate that the task failed
                    task.future.completeExceptionally(e);
                }
            }
        } finally {
            // Give a chance to the extended class to do something before the tasks are declared complete
            afterTasks(tasks);

            this.execution.removeContext();
        }
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

            case DELETE_STATUSES:
                EventStoreTask<Void, DeleteStatusesData> deleteStatusesTask =
                    (EventStoreTask<Void, DeleteStatusesData>) task;
                processTaskOutput(deleteStatusesTask,
                    syncDeleteEventStatuses(deleteStatusesTask.input.entityId, deleteStatusesTask.input.date));
                break;

            case SAVE_STATUS:
                processTaskOutput((EventStoreTask<EventStatus, EventStatus>) task,
                    syncSaveEventStatus((EventStatus) task.input));
                break;

            case DELETE_MAIL_ENTITY:
                processTaskOutput((EventStoreTask<Optional<EntityEvent>, EntityEvent>) task,
                    syncDeleteMailEntityEvent((EntityEvent) task.input));
                break;

            case SAVE_MAIL_ENTITY:
                processTaskOutput((EventStoreTask<EntityEvent, EntityEvent>) task,
                    syncSaveMailEntityEvent((EntityEvent) task.input));
                break;

            case PREFILTER_EVENT:
                processTaskOutput((EventStoreTask<Event, Event>) task, syncPrefilterEvent((Event) task.input));
                break;

            default:
                break;
        }

        return false;
    }

    private <O, I> void processTaskOutput(EventStoreTask<O, I> task, O output)
    {
        task.output = output;

        if (this.notifyEach) {
            complete(task, output);
        }
    }

    private <O, I> void complete(EventStoreTask<O, I> task, O output)
    {
        if (task.context != null) {
            // Restore a few things from the context in case the listener need them (for example to lookup the right
            // components for the context of the event)
            try {
                this.contextStore.restore(task.context);
            } catch (ComponentLookupException e) {
                this.logger.error("Failed to restore context of the event", output, e);
            }
        }

        // Notify Future listeners.
        // We do so before the call to event listeners because callers do not need to wait for them before continuing,
        // and instead should continue as soon as the output value is available.
        task.future.complete(output);

        // Notify event listeners
        Object notificationOuput = task.output;
        boolean skipNotify = false;
        if (task.output instanceof Optional<?>) {
            Optional<?> optionalOutput = (Optional<?>) task.output;
            if (optionalOutput.isPresent()) {
                notificationOuput = optionalOutput.get();
            } else {
                skipNotify = true;
            }
        }
        if (!skipNotify) {
            switch (task.type) {
                case DELETE_EVENT:
                    this.observation.notify(new EventStreamDeletedEvent(), notificationOuput);
                    break;

                case DELETE_EVENT_BY_ID:
                    this.observation.notify(new EventStreamDeletedEvent(), notificationOuput);
                    break;

                case SAVE_EVENT:
                    this.observation.notify(new EventStreamAddedEvent(), notificationOuput);
                    break;

                case DELETE_STATUS:
                    this.observation.notify(new EventStatusDeletedEvent(), notificationOuput);
                    break;

                case DELETE_STATUSES:
                    this.observation.notify(new EventStatusDeletedEvent(), null);
                    break;

                case SAVE_STATUS:
                    this.observation.notify(new EventStatusAddOrUpdatedEvent(), notificationOuput);
                    break;

                case DELETE_MAIL_ENTITY:
                    this.observation.notify(new MailEntityDeleteEvent(), notificationOuput);
                    break;

                case SAVE_MAIL_ENTITY:
                    this.observation.notify(new MailEntityAddedEvent(), notificationOuput);
                    break;

                default:
                    break;
            }
        }
    }

    /**
     * @param status the event status to save
     */
    protected abstract EventStatus syncSaveEventStatus(EventStatus status) throws EventStreamException;

    /**
     * @param event the event/entity relation to save
     * @since 12.6
     */
    protected abstract EntityEvent syncSaveMailEntityEvent(EntityEvent event) throws EventStreamException;

    /**
     * @param event the event to save
     */
    protected abstract Event syncSaveEvent(Event event) throws EventStreamException;

    /**
     * @param event the event to save update
     * @since 12.6
     */
    protected abstract Event syncPrefilterEvent(Event event) throws EventStreamException;

    /**
     * @param status the event status to save
     */
    protected abstract Optional<EventStatus> syncDeleteEventStatus(EventStatus status) throws EventStreamException;

    /**
     * @param entityId the id of the entity for which to remove the statuses
     * @param date the date before which to remove the statuses
     * @since 12.6
     */
    protected abstract Void syncDeleteEventStatuses(String entityId, Date date) throws EventStreamException;

    /**
     * @param event the event/entity relation to delete
     * @since 12.6
     */
    protected abstract Optional<EntityEvent> syncDeleteMailEntityEvent(EntityEvent event) throws EventStreamException;

    /**
     * @param eventId the id of the event to delete
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
                complete(task, task.output);
            }
        }
    }

    protected void initialize(int queueCapacity, boolean notifyEach, boolean notifyAll)
    {
        this.notifyEach = notifyEach;
        this.notifyAll = !notifyEach && notifyAll;

        this.queueCapacity = queueCapacity;
        this.queue = new LinkedBlockingQueue<>(this.queueCapacity);

        this.thread = new Thread(this::run);
        this.thread.setName("Asynchronous handler for event store [" + descriptor.getRoleHint() + "]");
        this.thread.setPriority(Thread.NORM_PRIORITY - 1);
        this.thread.start();
    }

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        this.disposed = true;

        // Make sure to wake up the thread
        addTask(EventStoreTask.STOP);

        // Wait for the processing to be over but not more than 10s in case it's stuck for some reason
        try {
            this.thread.join(10000);
        } catch (InterruptedException e) {
            this.logger.warn("The thread handling asynchronous storage for event store [{}] has been interrupted",
                this.descriptor.getRoleHint(), e);

            this.thread.interrupt();
        }
    }
}
