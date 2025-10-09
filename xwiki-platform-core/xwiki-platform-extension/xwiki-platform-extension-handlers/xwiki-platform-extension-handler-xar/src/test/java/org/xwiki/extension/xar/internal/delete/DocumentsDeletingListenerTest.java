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
package org.xwiki.extension.xar.internal.delete;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.extension.ExtensionId;
import org.xwiki.extension.repository.InstalledExtensionRepository;
import org.xwiki.extension.xar.internal.delete.question.ExtensionBreakingQuestion;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtension;
import org.xwiki.extension.xar.internal.repository.XarInstalledExtensionRepository;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
@ComponentTest
class DocumentsDeletingListenerTest
{
    @InjectMockComponents
    private DocumentsDeletingListener listener;

    private XarInstalledExtensionRepository repository;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @BeforeComponent
    void setUp(MockitoComponentManager mockitoComponentManager) throws Exception
    {
        this.repository = mock(XarInstalledExtensionRepository.class);
        mockitoComponentManager.registerComponent(InstalledExtensionRepository.class, "xar", this.repository);
    }

    @Test
    void onEvent() throws Exception
    {
        Request request = mock(Request.class);
        Job job = mock(Job.class);
        JobStatus status = mock(JobStatus.class);
        when(job.getRequest()).thenReturn(request);
        when(request.isInteractive()).thenReturn(true);
        when(job.getStatus()).thenReturn(status);

        Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();

        DocumentReference doc1 = new DocumentReference("a", "b", "c1");
        DocumentReference doc2 = new DocumentReference("a", "b", "c2");
        DocumentReference doc3 = new DocumentReference("a", "b", "c3");
        DocumentReference doc4 = new DocumentReference("a", "b", "c4");

        concernedEntities.put(doc1, new EntitySelection(doc1));
        concernedEntities.put(doc2, new EntitySelection(doc2));
        concernedEntities.put(doc3, new EntitySelection(doc3));
        concernedEntities.put(doc4, new EntitySelection(doc4));

        XarInstalledExtension ext1 = mock(XarInstalledExtension.class);
        XarInstalledExtension ext2 = mock(XarInstalledExtension.class);
        when(ext1.getId()).thenReturn(new ExtensionId("ext1"));
        when(ext2.getId()).thenReturn(new ExtensionId("ext2"));
        when(this.repository.getXarInstalledExtensions(doc1)).thenReturn(Arrays.asList(ext1, ext2));
        when(this.repository.isAllowed(doc1, Right.DELETE)).thenReturn(false);
        when(this.repository.getXarInstalledExtensions(doc2)).thenReturn(Collections.emptyList());
        when(this.repository.isAllowed(doc2, Right.DELETE)).thenReturn(true);
        when(this.repository.getXarInstalledExtensions(doc3)).thenReturn(Arrays.asList(ext2));
        when(this.repository.isAllowed(doc3, Right.DELETE)).thenReturn(false);
        when(this.repository.getXarInstalledExtensions(doc4)).thenReturn(Arrays.asList(ext1));
        when(this.repository.isAllowed(doc4, Right.DELETE)).thenReturn(true);

        doAnswer(invocationOnMock -> {
            ExtensionBreakingQuestion question = invocationOnMock.getArgument(0);
            assertEquals(concernedEntities, question.getConcernedEntities());
            // Ext 1
            assertEquals(1, question.getExtension("ext1").getPages().size());
            assertTrue(question.getExtension("ext1").getPages().contains(
                    concernedEntities.get(doc1)
            ));

            // Ext 2
            assertEquals(2, question.getExtension("ext2").getPages().size());
            assertTrue(question.getExtension("ext2").getPages().contains(
                    concernedEntities.get(doc1)
            ));
            assertTrue(question.getExtension("ext2").getPages().contains(
                    concernedEntities.get(doc3)
            ));

            // Free pages
            assertEquals(2, question.getFreePages().size());
            assertTrue(question.getFreePages().contains(concernedEntities.get(doc2)));
            assertTrue(question.getFreePages().contains(concernedEntities.get(doc4)));

            // Assert nothing is select by default
            for (EntitySelection selection : question.getConcernedEntities().values()) {
                assertFalse(selection.isSelected());
            }
            return null;
        }).when(status).ask(any(), anyLong(), any());

        // Test
        DocumentsDeletingEvent event = new DocumentsDeletingEvent();
        this.listener.onEvent(event, job, concernedEntities);

        // Check
        verify(status).ask(any(), eq(5L), eq(TimeUnit.MINUTES));
        assertEquals(1, logCapture.size());
        assertEquals("The question has been asked, however no answer has been received.", logCapture.getMessage(0));
    }

    @Test
    public void onEventWhenNonInteractive()
    {
        Request request = mock(Request.class);
        Job job = mock(Job.class);
        when(job.getRequest()).thenReturn(request);
        when(request.isInteractive()).thenReturn(false);
        JobStatus status = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(status);

        // Test
        DocumentsDeletingEvent event = new DocumentsDeletingEvent();
        this.listener.onEvent(event, job, null);

        // Verify
        assertEquals(1, logCapture.size());
        assertEquals("XAR Extension Documents Deleting Listener will not check the document in non-interactive mode.",
            logCapture.getMessage(0));
        verifyNoInteractions(status);
        verifyNoInteractions(repository);
    }

    @Test
    void onEventWhenCancelled() throws Exception
    {
        Request request = mock(Request.class);
        Job job = mock(Job.class);
        JobStatus status = mock(JobStatus.class);
        when(job.getRequest()).thenReturn(request);
        when(request.isInteractive()).thenReturn(true);
        when(job.getStatus()).thenReturn(status);

        Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();
        DocumentReference doc1 = new DocumentReference("a", "b", "c1");
        concernedEntities.put(doc1, new EntitySelection(doc1));

        XarInstalledExtension ext1 = mock(XarInstalledExtension.class);
        when(ext1.getId()).thenReturn(new ExtensionId("ext1"));
        when(this.repository.getXarInstalledExtensions(doc1)).thenReturn(Arrays.asList(ext1));

        InterruptedException e = new InterruptedException();
        doThrow(e).when(status).ask(any(), anyLong(), any());

        // Test
        DocumentsDeletingEvent event = mock(DocumentsDeletingEvent.class);
        this.listener.onEvent(event, job, concernedEntities);

        // Check
        verify(status).ask(any(), eq(5L), eq(TimeUnit.MINUTES));
        verify(event).cancel(eq("Question has been interrupted."));
        assertEquals(1, logCapture.size());
        assertEquals("Confirm question has been interrupted.", logCapture.getMessage(0));
    }
}
