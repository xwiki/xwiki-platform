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
package org.xwiki.index.internal;

import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.ExecutionContext;
import org.xwiki.context.ExecutionContextException;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.doc.tasks.XWikiDocumentIndexingTask;
import org.xwiki.index.TaskManager;
import org.xwiki.index.internal.jmx.JMXTasks;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;

import static java.lang.Thread.NORM_PRIORITY;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Initialize a {@link PriorityBlockingQueue} with the tasks stored in database.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@Component
@Singleton
public class DefaultTasksManager implements TaskManager, Initializable, Disposable, Runnable
{
    private static final String MBEAN_NAME = "name=index";

    private PriorityBlockingQueue<TaskData> queue;

    /**
     * Stores the latest timestamp for the tasks. If a task is queued with an outdated timestamp, it will be skipped and
     * canceled.
     */
    private ConcurrentHashMap<TaskData, Long> latestTimestampTasksMap;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private Provider<TasksStore> tasksStore;

    @Inject
    private RemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @Inject
    private JMXBeanRegistration jmxRegistration;

    @Inject
    private TaskExecutor taskExecutor;

    @Inject
    private ExecutionContextManager executionContextManager;

    @Inject
    private Logger logger;

    /**
     * When {@code true}, indicates that the {@link #run()} method should stop.
     */
    private boolean halt;

    /**
     * Lock used to ensure that no thread is in a state where a task has been added to the database but not to the
     * queue.
     */
    private final ReentrantReadWriteLock readWriteLock = new ReentrantReadWriteLock();

    /**
     * Read lock. Contrary to the name, this is used to protect operations that modify the state but do not need any
     * consistency guarantees.
     */
    private final ReentrantReadWriteLock.ReadLock readLock = this.readWriteLock.readLock();

    /**
     * Write lock. Acquire this if no modification of the queue must be happening.
     */
    private final ReentrantReadWriteLock.WriteLock writeLock = this.readWriteLock.writeLock();

    @Override
    public CompletableFuture<TaskData> addTask(String wikiId, long docId, String type)
    {
        return addTask(wikiId, docId, "", type);
    }

    @Override
    public CompletableFuture<TaskData> addTask(String wikiId, long docId, String version, String type)
    {
        XWikiDocumentIndexingTask xWikiTask = initTask(docId, type, version);
        this.readLock.lock();
        try {
            try {
                this.tasksStore.get().addTask(wikiId, xWikiTask);
            } catch (Exception e) {
                this.logger.warn(
                    "Failed to add a task for docId [{}], type [{}] and version [{}] in wiki [{}]. This task is queued"
                        + " but will not be will not be restarted if not completed before the server stops."
                        + " Cause: [{}].",
                    docId, type, version, wikiId, getRootCauseMessage(e));
            }

            TaskData taskData = convert(wikiId, xWikiTask);
            this.latestTimestampTasksMap.put(taskData, taskData.getTimestamp());
            this.queue.add(taskData);
            return taskData.getFuture();
        } finally {
            this.readLock.unlock();
        }
    }

    @Override
    public void initialize()
    {
        this.jmxRegistration.registerMBean(new JMXTasks(this::getQueueSize,
                () -> this.queue.stream().collect(Collectors.groupingBy(TaskData::getType, Collectors.counting()))),
            MBEAN_NAME);
        this.queue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(TaskData::getTimestamp));
        this.latestTimestampTasksMap = new ConcurrentHashMap<>();
    }

    @Override
    public void dispose()
    {
        this.jmxRegistration.unregisterMBean(MBEAN_NAME);
        this.queue.add(TaskData.STOP);
    }

    /**
     * Start the consumer thread.
     */
    public void startThread()
    {
        Thread thread = new Thread(this);
        thread.setName("task-manager-consumer");
        thread.setPriority(NORM_PRIORITY - 1);
        thread.start();
    }

    @Override
    public long getQueueSize()
    {
        return this.queue.size();
    }

    @Override
    public long getQueueSize(String type)
    {
        return this.queue.stream().filter(taskData -> Objects.equals(taskData.getType(), type)).count();
    }

    @Override
    public Map<String, Long> getQueueSizePerType(String wikiId)
    {
        return this.queue.stream()
            .filter(taskData -> Objects.equals(taskData.getWikiId(), wikiId))
            .collect(Collectors.groupingBy(TaskData::getType, Collectors.counting()));
    }

    @Override
    public void run()
    {
        try {
            initQueue();
            while (!this.halt) {
                consume();
            }
        } catch (InitializationException e) {
            this.logger.error("Failed to initialize the tasks consumer thread.", e);
        }
    }

    private void consume()
    {
        TaskData task = null;
        try {
            task = this.queue.take();
            task.increaseAttempts();
            if (task.isStop()) {
                this.halt = true;
            } else {
                if (isTimestampValid(task)) {
                    this.taskExecutor.execute(task);
                    task.getFuture().complete(task);
                } else {
                    task.getFuture().cancel(false);
                }
                deleteTask(task);
            }
        } catch (InterruptedException e) {
            this.logger.warn("The task manager consumer thread was interrupted while processing task [{}] for "
                + "document [{}]. Cause: [{}].", task, getTaskDocumentReferenceForLogging(task),
                getRootCauseMessage(e));
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            this.logger.warn("Error during the execution of task [{}] for document [{}]. Cause: [{}].", task,
                getTaskDocumentReferenceForLogging(task), getRootCauseMessage(e));
            this.logger.debug("Stack trace for previous error: ", e);
            if (task != null && isTimestampValid(task)) {
                if (!task.tooManyAttempts()) {
                    // Push back the failed task at the beginning of the queue by resetting its timestamp.
                    long newTimestamp = System.currentTimeMillis();
                    this.latestTimestampTasksMap.put(task, newTimestamp);
                    task.setTimestamp(newTimestamp);
                    this.queue.put(task);
                } else {
                    this.logger.error("[{}] abandoned because it has failed too many times.", task, e);
                    deleteTask(task);
                    task.getFuture().cancel(false);
                }
            } else if (task != null) {
                task.getFuture().cancel(false);
            }
        } 
    }

    private void initQueue() throws InitializationException
    {
        try {
            this.executionContextManager.initialize(new ExecutionContext());
            // Load the tasks for all wikis.
            for (String wikiId : this.wikiDescriptorManager.getAllIds()) {
                loadWiki(wikiId);
            }
        } catch (WikiManagerException e) {
            throw new InitializationException("Failed to list the wiki IDs.", e);
        } catch (ExecutionContextException e) {
            throw new InitializationException("Error when initializing the execution context.", e);
        }
    }

    private void loadWiki(String wikiId) throws InitializationException
    {
        try {
            List<XWikiDocumentIndexingTask> tasksInDB = this.tasksStore.get().getAllTasks(wikiId,
                this.remoteObservationManagerConfiguration.getId());

            // Check for each task if it is already in the queue. This is necessary as tasks might
            // have been added before this call, see XWIKI-19471.
            // For this, get a snapshot of all existing tasks. This doesn't include insertions afterwards but that's
            // not important as they are not in the tasks from the DB, either. For this property to hold, it is
            // important, though, to first get the tasks from the DB and then from the queue.
            // Note that if this queried the queue for every task, the running time would be quadratic in the number of
            // tasks, that's why there is this snapshot in a hash set.
            Set<TaskData> existingTasks;
            // Make sure no task is in the DB but not in the queue.
            this.writeLock.lock();
            try {
                existingTasks = new HashSet<>(this.queue);
            } finally {
                this.writeLock.unlock();
            }

            for (XWikiDocumentIndexingTask task : tasksInDB) {
                TaskData taskData = convert(wikiId, task);
                if (!existingTasks.contains(taskData)) {
                    this.latestTimestampTasksMap.computeIfAbsent(taskData, TaskData::getTimestamp);
                    this.queue.put(taskData);
                }
            }
        } catch (XWikiException e) {
            throw new InitializationException(String.format("Failed to get tasks for wiki [%s]", wikiId), e);
        }
    }

    private TaskData convert(String wikiId, XWikiDocumentIndexingTask task)
    {
        TaskData taskData = new TaskData();
        taskData.setTimestamp(task.getTimestamp().getTime());
        taskData.setVersion(task.getVersion());
        taskData.setDocId(task.getDocId());
        taskData.setType(task.getType());
        taskData.setWikiId(wikiId);
        return taskData;
    }

    private XWikiDocumentIndexingTask initTask(long docId, String type, String version)
    {
        XWikiDocumentIndexingTask xWikiTask = new XWikiDocumentIndexingTask();
        xWikiTask.setDocId(docId);
        xWikiTask.setType(type);
        xWikiTask.setVersion(version);
        xWikiTask.setInstanceId(this.remoteObservationManagerConfiguration.getId());
        xWikiTask.setTimestamp(new Date());
        return xWikiTask;
    }

    /**
     * @param task a task
     * @return {@code true} if the timestamp of the task matches the latest timestamp for the same tasks in the queue,
     *     {@code false} otherwise
     */
    private boolean isTimestampValid(TaskData task)
    {
        return task.getTimestamp() == this.latestTimestampTasksMap.getOrDefault(task, 0L);
    }

    private void deleteTask(TaskData task)
    {
        this.writeLock.lock();
        try {
            if (isTimestampValid(task)) {
                try {
                    this.tasksStore.get()
                        .deleteTask(task.getWikiId(), task.getDocId(), task.getVersion(), task.getType());
                } catch (XWikiException e) {
                    this.logger.error("Failed to delete task [{}] from the queue. It will be reloaded on restart.",
                        task, e);
                }
                this.latestTimestampTasksMap.remove(task);
            }
        } finally {
            this.writeLock.unlock();
        }
    }

    private DocumentReference getTaskDocumentReferenceForLogging(TaskData taskData)
    {
        DocumentReference result;
        try {
            result =
                this.tasksStore.get().getDocument(taskData.getWikiId(), taskData.getDocId()).getDocumentReference();
        } catch (XWikiException e) {
            // Failed to get the document for some reason, return null since this method is only used to provide
            // more information for debugging
            result = null;
        }
        return result;
    }
}
