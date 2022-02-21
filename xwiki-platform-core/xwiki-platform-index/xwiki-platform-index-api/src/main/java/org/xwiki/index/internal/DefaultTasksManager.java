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
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.PriorityBlockingQueue;
import java.util.function.Predicate;
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
import org.xwiki.index.TaskManager;
import org.xwiki.index.internal.jmx.JMXTasks;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.tasks.XWikiDocumentIndexingTask;
import com.xpn.xwiki.doc.tasks.XWikiDocumentIndexingTaskId;

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

    @Override
    public CompletableFuture<TaskData> addTask(String wikiId, long docId, String version, String type)
    {
        XWikiDocumentIndexingTask xWikiTask = initTask(docId, type, version);
        try {
            this.tasksStore.get().addTask(wikiId, xWikiTask);
        } catch (XWikiException e) {
            this.logger.warn(
                "Failed to add a task for docId [{}], type [{}] and version [{}] in wiki [{}]. This task is queued but "
                    + "will not be will not be restarted if not completed before the server stops. Cause: [{}].", docId,
                type, version, wikiId, getRootCauseMessage(e));
        }

        TaskData taskData = convert(wikiId, xWikiTask);
        this.queue.add(taskData);
        return taskData.getFuture();
    }

    @Override
    public CompletableFuture<TaskData> replaceTask(String wikiId, long docId, String version, String type)
    {
        XWikiDocumentIndexingTask xWikiTask = initTask(docId, type, version);
        try {
            this.tasksStore.get().replaceTask(wikiId, xWikiTask);
        } catch (XWikiException e) {
            this.logger.warn("Failed to persist task with docId [{}], type [{}] and version [{}] in wiki [{}]. The "
                + "tasks are replaced but will not be restarted if not completed before the server "
                + "stops. Cause: [{}].", docId, type, version, wikiId, getRootCauseMessage(e));
        }

        Predicate<TaskData> filter = queuedTask -> Objects.equals(queuedTask.getWikiId(), wikiId)
            && Objects.equals(queuedTask.getType(), type)
            && Objects.equals(queuedTask.getDocId(), docId);

        // Cancel, then remove, the tasks that need to be replaced.
        this.queue.forEach(taskData -> {
            if (filter.test(taskData)) {
                taskData.getFuture().cancel(false);
            }
        });
        this.queue.removeIf(filter);
        TaskData taskData = convert(wikiId, xWikiTask);
        this.queue.add(taskData);
        return taskData.getFuture();
    }

    @Override
    public void initialize()
    {
        this.jmxRegistration.registerMBean(new JMXTasks(this::getQueueSize,
                () -> this.queue.stream().collect(Collectors.groupingBy(TaskData::getType, Collectors.counting()))),
            MBEAN_NAME);
        this.queue = new PriorityBlockingQueue<>(11, Comparator.comparingLong(TaskData::getTimestamp));
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
            } else if (!task.isDeprecated()) {
                this.taskExecutor.execute(task);
            }
        } catch (InterruptedException e) {
            this.logger.warn("The task manager consumer thread was interrupted. Cause: [{}].",
                getRootCauseMessage(e));
            Thread.currentThread().interrupt();
        } catch (Exception e) {
            this.logger.warn("Error during the execution of task [{}]. Cause: [{}].", task, getRootCauseMessage(e));
            if (task != null) {
                if (!task.tooManyAttempts()) {
                    // Push back the failed task at the beginning of the queue by resetting its timestamp.
                    task.setTimestamp(System.currentTimeMillis());
                    this.queue.put(task);
                } else {
                    this.logger.error("[{}] abandoned because it has failed too many times.", task);
                    task.getFuture().cancel(false);
                }
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
            this.queue.addAll(
                this.tasksStore.get().getAllTasks(wikiId, this.remoteObservationManagerConfiguration.getId())
                    .stream()
                    .map(task -> convert(wikiId, task))
                    .collect(Collectors.toList()));
        } catch (XWikiException e) {
            throw new InitializationException(String.format("Failed to get tasks for wiki [%s]", wikiId), e);
        }
    }

    private TaskData convert(String wikiId, XWikiDocumentIndexingTask task)
    {
        TaskData taskData = new TaskData();
        taskData.setTimestamp(task.getTimestamp().getTime());
        taskData.setVersion(task.getId().getVersion());
        taskData.setDocId(task.getId().getDocId());
        taskData.setType(task.getId().getType());
        taskData.setWikiId(wikiId);
        return taskData;
    }

    private XWikiDocumentIndexingTask initTask(long docId, String type, String version)
    {
        XWikiDocumentIndexingTask xWikiTask = new XWikiDocumentIndexingTask();
        XWikiDocumentIndexingTaskId id = new XWikiDocumentIndexingTaskId();
        id.setDocId(docId);
        id.setType(type);
        id.setVersion(version);
        id.setInstanceId(this.remoteObservationManagerConfiguration.getId());
        xWikiTask.setId(id);
        xWikiTask.setTimestamp(new Date());
        return xWikiTask;
    }
}
