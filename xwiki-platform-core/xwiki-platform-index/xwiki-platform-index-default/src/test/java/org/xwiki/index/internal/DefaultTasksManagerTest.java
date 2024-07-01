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

import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CancellationException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.atomic.AtomicReference;

import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.doc.tasks.XWikiDocumentIndexingTask;
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

import ch.qos.logback.classic.Level;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.matchesPattern;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
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
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

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
            invocation.getArgument(0);
            return null;
        }).when(this.taskExecutor).execute(any());
    }

    @Test
    void addTask() throws Exception
    {
        this.tasksManager.startThread();
        CompletableFuture<TaskData> taskFuture =
            this.tasksManager.addTask("wikiId", 42, "1.3", "testtask");

        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        task.setDocId(42);
        task.setType("testtask");
        task.setInstanceId(INSTANCE_ID);
        task.setVersion("1.3");
        verify(this.tasksStore).addTask("wikiId", task);

        TaskData taskData = new TaskData(42, "1.3", "testtask", "wikiId");
        assertEquals(taskData, taskFuture.get());
        verify(this.taskExecutor).execute(taskData);
    }

    @Test
    void addTaskFailsOnce() throws Exception
    {
        this.tasksManager.startThread();
        // Fails the first time, then succeeds the second time execute is called.
        doThrow(new RuntimeException("Test")).doAnswer(invocation -> {
            invocation.getArgument(0);
            return null;
        }).when(this.taskExecutor).execute(any());

        CompletableFuture<TaskData> taskFuture =
            this.tasksManager.addTask("wikiId", 42, "1.3", "testtask");

        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        task.setDocId(42);
        task.setType("testtask");
        task.setInstanceId(INSTANCE_ID);
        task.setVersion("1.3");
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
        this.tasksManager.startThread();
        XWikiDocumentIndexingTask task = new XWikiDocumentIndexingTask();
        task.setDocId(42);
        task.setType("testtask");
        task.setInstanceId(INSTANCE_ID);
        task.setVersion("");

        doThrow(new XWikiException()).when(this.tasksStore).addTask("wikiId", task);

        CompletableFuture<TaskData> taskFuture = this.tasksManager.addTask("wikiId", 42, "testtask");

        TaskData taskData = new TaskData(42, "", "testtask", "wikiId");
        assertEquals(taskData, taskFuture.get());
        verify(this.taskExecutor).execute(taskData);

        assertEquals(1, this.logCapture.size());
        assertEquals("Failed to add a task for docId [42], type [testtask] and version [] in wiki [wikiId]."
            + " This task is queued but will not be will not be restarted if not completed before the server stops."
            + " Cause: [XWikiException: Error number 0 in 0].", this.logCapture.getMessage(0));
        assertEquals(Level.WARN, this.logCapture.getLogEvent(0).getLevel());
    }

    @Test
    void initQueueFromDatabase() throws Exception
    {
        when(this.wikiDescriptorManager.getAllIds()).thenReturn(List.of("wikiId", "wikiB"));
        XWikiDocumentIndexingTask xWikiTask = new XWikiDocumentIndexingTask();
        xWikiTask.setTimestamp(new Date());
        xWikiTask.setVersion("1.3");
        xWikiTask.setType("testtask");
        xWikiTask.setDocId(42);
        when(this.tasksStore.getAllTasks("wikiId", INSTANCE_ID)).thenReturn(List.of(xWikiTask));

        this.tasksManager.startThread();

        // Queue a new task to have something to wait for Waits 1ms to make sure that the task is with a timestamps 
        // higher than the tasks from the database.
        Thread.sleep(1);
        CompletableFuture<TaskData> future = this.tasksManager.addTask("wikiId", 42, "1.3", "othertask");

        // Wait for the new task to be consumed to make sure that all the initialization process is completed.
        assertNotNull(future.get());
        verify(this.tasksStore).getAllTasks("wikiId", INSTANCE_ID);
        verify(this.tasksStore).getAllTasks("wikiB", INSTANCE_ID);
        verify(this.taskExecutor).execute(new TaskData(42, "1.3", "testtask", "wikiId"));
        verify(this.taskExecutor).execute(new TaskData(42, "1.3", "othertask", "wikiId"));
    }

    @Test
    void addTaskDuringTaskExecution() throws Exception
    {
        this.tasksManager.startThread();

        AtomicReference<CompletableFuture<TaskData>> taskDataCompletableFuture = new AtomicReference<>();

        // Block the fist task and let the next tasks execute instantly.
        doAnswer(invocation -> {
            // TODO: Remove once XWIKI-21820 is closed, without this sleep the test might flicker if the two addTask to 
            // doc 442 version 1.2 are executed during the same millisecond, leading to a flickering test.
            Thread.sleep(1);
            taskDataCompletableFuture.set(this.tasksManager.addTask("wikiA", 42, "1.2", "concurrent"));
            return null;
        })
            .doAnswer(invocation -> {
                verify(this.tasksStore, never()).deleteTask("wikiA", 42, "1.2", "concurrent");
                return null;
            })
            .doAnswer(invocation -> null)
            .when(this.taskExecutor).execute(any());

        this.tasksManager.addTask("wikiA", 42, "1.2", "concurrent").get();
        taskDataCompletableFuture.get().get();

        verify(this.taskExecutor, times(2)).execute(any());

        // Queue another task to make sure that the previous tasks are fully consumed. Otherwise, the deleteTask might 
        // not be called before the end of the test. 
        this.tasksManager.addTask("wikiA", 42, "1.3", "concurrent").get();

        verify(this.tasksStore).deleteTask("wikiA", 42, "1.2", "concurrent");
    }

    @Test
    void addTaskConcurrently() throws Exception
    {
        CompletableFuture<TaskData> future0 = this.tasksManager.addTask("wikiA", 42, "1.2", "concurrent");
        Thread.sleep(1);
        CompletableFuture<TaskData> future1 = this.tasksManager.addTask("wikiA", 42, "1.2", "concurrent");

        this.tasksManager.startThread();

        assertThrows(CancellationException.class, future0::get);
        assertNotNull(future1.get());

        verify(this.taskExecutor).execute(org.mockito.ArgumentMatchers.same(future1.get()));
        verifyNoMoreInteractions(this.taskExecutor);

        // Queue another task to make sure that the previous tasks are fully consumed. Otherwise, the deleteTask might 
        // not be called before the end of the test.
        this.tasksManager.addTask("wikiA", 42, "1.3", "concurrent").get();

        verify(this.tasksStore).deleteTask("wikiA", 42, "1.2", "concurrent");
    }

    @Test
    void getQueueSizePerType()
    {
        this.tasksManager.addTask("wikiA", 42, "1.2", "typeB");
        this.tasksManager.addTask("wikiA", 42, "1.2", "typeA");
        this.tasksManager.addTask("wikiA", 42, "1.2", "typeB");
        this.tasksManager.addTask("wikiB", 42, "1.2", "typeA");
        this.tasksManager.addTask("wikiB", 42, "1.2", "typeA");
        assertEquals(Map.of("typeA", 1L, "typeB", 2L), this.tasksManager.getQueueSizePerType("wikiA"));
        assertEquals(Map.of("typeA", 2L), this.tasksManager.getQueueSizePerType("wikiB"));
        assertEquals(Map.of(), this.tasksManager.getQueueSizePerType("wikiC"));
    }
}
