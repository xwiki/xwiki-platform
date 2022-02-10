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

import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.remote.RemoteObservationManagerConfiguration;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.tasks.XWikiDocumentIndexingTask;
import com.xpn.xwiki.doc.tasks.XWikiDocumentIndexingTaskId;

import ch.qos.logback.classic.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultTasksManager}.
 *
 * @version $Id$
 * @since 14.1RC1
 */
@ComponentTest
class DefaultTasksManagerTest
{
    public static final DocumentReference DOCUMENT_REFERENCE_WIKIID_42 =
        new DocumentReference("xwiki", "Space", "Page42");

    public static final DocumentReference DOCUMENT_REFERENCE_WIKIID_43 =
        new DocumentReference("xwiki", "Space", "Page43");

    public static final String INSTANCE_ID = "instance-id";

    @InjectMockComponents
    private DefaultTasksManager tasksManager;

    @MockComponent
    private WikiDescriptorManager wikiDescriptorManager;

    @MockComponent
    private Provider<TasksStore> tasksStoreProvider;

    @MockComponent
    private RemoteObservationManagerConfiguration remoteObservationManagerConfiguration;

    @MockComponent
    private JMXBeanRegistration jmxRegistration;

    @MockComponent
    private TaskExecutor taskExecutor;

    @Mock
    private TasksStore tasksStore;

    @RegisterExtension
    LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.tasksStoreProvider.get()).thenReturn(this.tasksStore);
        when(this.remoteObservationManagerConfiguration.getId()).thenReturn(INSTANCE_ID);

        XWikiDocument documentWikiId42 = mock(XWikiDocument.class);
        when(this.tasksStore.getDocument("wikiId", 42)).thenReturn(documentWikiId42);
        when(documentWikiId42.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE_WIKIID_42);
        when(documentWikiId42.getVersion()).thenReturn("1.3");

        XWikiDocument documentWikiId43 = mock(XWikiDocument.class);
        when(this.tasksStore.getDocument("wikiId", 43)).thenReturn(documentWikiId43);
        when(documentWikiId43.getDocumentReference()).thenReturn(DOCUMENT_REFERENCE_WIKIID_43);
        when(documentWikiId43.getVersion()).thenReturn("1.3");

        // By default, tasks as consumer instantaneously. 
        doAnswer(invocation -> {
            TaskData task = invocation.getArgument(0);
            task.getFuture().complete(task);
            return null;
        }).when(this.taskExecutor).execute(any());
    }

    @Test
    void addTask() throws Exception
    {
        CompletableFuture<TaskData> taskFuture =
            this.tasksManager.addTask("wikiId", 42, "1.3", "testtask");

        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        XWikiDocumentIndexingTaskId taskId = new XWikiDocumentIndexingTaskId();
        taskId.setDocId(42);
        taskId.setType("testtask");
        taskId.setInstanceId(INSTANCE_ID);
        taskId.setVersion("1.3");
        task.setId(taskId);
        verify(this.tasksStore).addTask("wikiId", task);

        TaskData taskData = new TaskData(42, "1.3", "testtask", "wikiId");
        assertEquals(taskData, taskFuture.get());
        verify(this.taskExecutor).execute(taskData);
    }

    @Test
    void addTaskFailsOnce() throws Exception
    {
        // Fails the first time, then succeeds the second time execute is called.
        doThrow(new RuntimeException("Test")).doAnswer(invocation -> {
            TaskData taskData = invocation.getArgument(0);
            taskData.getFuture().complete(taskData);
            return null;
        }).when(this.taskExecutor).execute(any());

        CompletableFuture<TaskData> taskFuture =
            this.tasksManager.addTask("wikiId", 42, "1.3", "testtask");

        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        XWikiDocumentIndexingTaskId taskId = new XWikiDocumentIndexingTaskId();
        taskId.setDocId(42);
        taskId.setType("testtask");
        taskId.setInstanceId(INSTANCE_ID);
        taskId.setVersion("1.3");
        task.setId(taskId);
        verify(this.tasksStore).addTask("wikiId", task);

        TaskData taskData = new TaskData(42, "1.3", "testtask", "wikiId");
        assertEquals(taskData, taskFuture.get());
        verify(this.taskExecutor, times(2)).execute(taskData);

        assertEquals(1, this.logCapture.size());
        assertThat(this.logCapture.getMessage(0),
            matchesPattern("^Error during the execution of task \\[.+]. Cause: \\[RuntimeException: Test]\\.$"));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void addTaskDatabaseIssue() throws Exception
    {
        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        XWikiDocumentIndexingTaskId taskId = new XWikiDocumentIndexingTaskId();
        taskId.setDocId(42);
        taskId.setType("testtask");
        taskId.setInstanceId(INSTANCE_ID);
        taskId.setVersion("1.3");
        task.setId(taskId);

        doThrow(new XWikiException()).when(this.tasksStore).addTask("wikiId", task);

        CompletableFuture<TaskData> taskFuture =
            this.tasksManager.addTask("wikiId", 42, "1.3", "testtask");

        TaskData taskData = new TaskData(42, "1.3", "testtask", "wikiId");
        assertEquals(taskData, taskFuture.get());
        verify(this.taskExecutor).execute(taskData);

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to add a task for docId [42], type [testtask] and version [1.3] in wiki [wikiId]."
            + " This task is queued but will not be will not be restarted if not completed before the server stops."
            + " Cause: [XWikiException: Error number 0 in 0].", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void replaceTask() throws Exception
    {
        CompletableFuture<Void> blockTask = new CompletableFuture<>();
        // Block the fist task and let the next tasks execute instantly.
        doAnswer(invocation -> {
            Void unused = blockTask.get();
            TaskData taskData = invocation.getArgument(0);
            taskData.getFuture().complete(taskData);
            return unused;
        }).when(this.taskExecutor).execute(any());

        // Queue a first task to make it consumed and blocked. Then, queue a second task, then replace the first one 
        // with a new one. Only two Tasks must be consumed in the end.
        // Sleeps 1 millisecond between each new task to be able to assert the execution order of the tasks without
        // timestamp collision issues.
        this.tasksManager.addTask("wikiId", 42, "1.2", "blocked");
        Thread.sleep(1);
        CompletableFuture<TaskData> future0 = this.tasksManager.replaceTask("wikiId", 42, "1.2", "testtask");
        Thread.sleep(1);
        CompletableFuture<TaskData> future1 = this.tasksManager.replaceTask("wikiId", 42, "1.3", "othertask");
        Thread.sleep(1);
        CompletableFuture<TaskData> future2 = this.tasksManager.replaceTask("wikiId", 43, "1.3", "testtask");
        Thread.sleep(1);
        CompletableFuture<TaskData> future3 = this.tasksManager.replaceTask("wikiId", 42, "1.3", "testtask");

        verify(this.tasksStore, times(4)).replaceTask(any(), any());
        assertEquals(3, this.tasksManager.getQueueSize());
        assertEquals(2, this.tasksManager.getQueueSize("testtask"));
        assertEquals(1, this.tasksManager.getQueueSize("othertask"));

        // Complete the blocking completable to let the thread continue consuming the queue.
        blockTask.complete(null);

        // Cancelled since it has been replaced by the latest task.
        assertThrows(CancellationException.class, future0::get);
        assertNotNull(future1.get());
        assertNotNull(future2.get());
        assertNotNull(future3.get());

        InOrder inOrder = inOrder(this.taskExecutor);
        inOrder.verify(this.taskExecutor).execute(new TaskData(42, "1.2", "blocked", "wikiId"));
        inOrder.verify(this.taskExecutor).execute(new TaskData(42, "1.3", "othertask", "wikiId"));
        inOrder.verify(this.taskExecutor).execute(new TaskData(43, "1.3", "testtask", "wikiId"));
        inOrder.verify(this.taskExecutor).execute(new TaskData(42, "1.3", "testtask", "wikiId"));
    }

    @Test
    void replaceTaskDatabaseIssue() throws Exception
    {
        doThrow(new XWikiException()).when(this.tasksStore).replaceTask(any(), any());

        CompletableFuture<TaskData> future = this.tasksManager.replaceTask("wikiId", 42, "1.3", "testtask");

        // Cancelled since it has been replaced by the latest task.
        assertNotNull(future.get());

        TaskData task = new TaskData();
        task.setDocId(42);
        task.setType("testtask");
        task.setWikiId("wikiId");
        task.setVersion("1.3");
        verify(this.taskExecutor).execute(task);

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to persist task with docId [42], type [testtask] and version [1.3] in wiki"
            + " [wikiId]. The tasks are replaced but will not be restarted if not completed before the server stops."
            + " Cause: [XWikiException: Error number 0 in 0].", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }
}
