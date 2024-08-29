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
package org.xwiki.refactoring.job;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.extension.RegisterExtension;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.job.Job;
import org.xwiki.job.Request;
import org.xwiki.job.event.status.JobStatus;
import org.xwiki.model.EntityType;
import org.xwiki.model.internal.reference.DefaultSymbolScheme;
import org.xwiki.model.internal.reference.LocalStringEntityReferenceSerializer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.test.LogLevel;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
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
 * Validate the behaviour of {@link XClassDeletingListener}
 *
 * @version $Id$
 * @since 10.10RC1
 */
@ComponentTest
// @formatter:off
@ComponentList({
    LocalStringEntityReferenceSerializer.class,
    DefaultSymbolScheme.class
})
// @formatter:on
class XClassDeletingListenerTest
{
    @MockComponent
    private EntityReferenceResolver<String> resolver;

    @MockComponent
    private QueryManager queryManager;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.WARN);

    @InjectMockComponents
    private XClassDeletingListener deletingListener;

    @Test
    void interactive() throws Exception
    {
        Request request = mock(Request.class);
        Job job = mock(Job.class);
        JobStatus status = mock(JobStatus.class);
        when(job.getRequest()).thenReturn(request);
        when(request.isInteractive()).thenReturn(true);
        when(job.getStatus()).thenReturn(status);

        Map<EntityReference, EntitySelection> concernedEntities = new HashMap<>();

        DocumentReference xclass1 = new DocumentReference("wiki1", "a", "xclass1");
        DocumentReference simplePage1 = new DocumentReference("wiki1", "b", "simplepage");
        DocumentReference simplePage2 = new DocumentReference("wiki2", "a", "otherpage");
        DocumentReference xclass2 = new DocumentReference("wiki2", "b", "xclass2");

        EntitySelection xclass1Selection = new EntitySelection(xclass1);
        EntitySelection simplePage1Selection = new EntitySelection(simplePage1);
        EntitySelection simplePage2Selection = new EntitySelection(simplePage2);
        EntitySelection xclass2Selection = new EntitySelection(xclass2);

        concernedEntities.put(xclass1, xclass1Selection);
        concernedEntities.put(simplePage1, simplePage1Selection);
        concernedEntities.put(simplePage2, simplePage2Selection);
        concernedEntities.put(xclass2, xclass2Selection);

        DocumentReference docObjReference1 = new DocumentReference("wiki1", "a", "xobject1");
        DocumentReference docObjReference2 = new DocumentReference("wiki1", "b", "xobject2");
        DocumentReference docObjReference3 = new DocumentReference("wiki2", "b", "xobject3");

        Query query = mock(Query.class);
        when(queryManager.createQuery(any(), eq(Query.HQL))).thenReturn(query);
        when(query.setLimit(anyInt())).thenReturn(query);

        Query query1 = mock(Query.class);
        when(query.bindValue("className", "a.xclass1")).thenReturn(query1);
        when(query1.setWiki("wiki1")).thenReturn(query1);
        when(query1.execute())
            .thenReturn(Arrays.asList("a.xobject1", "b.xobject2"));

        Query query2 = mock(Query.class);
        when(query.bindValue("className", "b.simplepage")).thenReturn(query2);
        when(query2.setWiki("wiki1")).thenReturn(query2);
        when(query2.execute()).thenReturn(Collections.emptyList());

        Query query3 = mock(Query.class);
        when(query.bindValue("className", "a.otherpage")).thenReturn(query3);
        when(query3.setWiki("wiki2")).thenReturn(query3);
        when(query3.execute()).thenReturn(Collections.emptyList());

        Query query4 = mock(Query.class);
        when(query.bindValue("className", "b.xclass2")).thenReturn(query4);
        when(query4.setWiki("wiki2")).thenReturn(query4);
        when(query4.execute()).thenReturn(Arrays.asList("b.xobject3"));

        when(resolver.resolve("a.xobject1", EntityType.DOCUMENT, xclass1)).thenReturn(docObjReference1);
        when(resolver.resolve("b.xobject2", EntityType.DOCUMENT, xclass1)).thenReturn(docObjReference2);
        when(resolver.resolve("b.xobject3", EntityType.DOCUMENT, xclass2)).thenReturn(docObjReference3);

        when(documentAccessBridge.isAdvancedUser(any())).thenReturn(true);

        doAnswer(invocationOnMock -> {
            XClassBreakingQuestion question = invocationOnMock.getArgument(0);
            assertEquals(concernedEntities, question.getConcernedEntities());

            Set<EntitySelection> expectedFreePages = new HashSet<>();
            expectedFreePages.add(simplePage1Selection);
            expectedFreePages.add(simplePage2Selection);

            assertEquals(expectedFreePages, question.getFreePages());

            Set<EntitySelection> expectedXPages = new HashSet<>();
            expectedXPages.add(xclass1Selection);
            expectedXPages.add(xclass2Selection);

            assertEquals(expectedXPages, question.getXClassPages());

            Map<EntityReference, Set<EntityReference>> expectedImpactedObjects = new HashMap<>();

            Set<EntityReference> expectedSetClass1 = new HashSet<>();
            expectedSetClass1.add(docObjReference1);
            expectedSetClass1.add(docObjReference2);
            expectedImpactedObjects.put(xclass1, expectedSetClass1);

            Set<EntityReference> expectedSetClass2 = new HashSet<>();
            expectedSetClass2.add(docObjReference3);
            expectedImpactedObjects.put(xclass2, expectedSetClass2);

            assertEquals(expectedImpactedObjects, question.getImpactedObjects());

            // Assert nothing is select by default
            for (EntitySelection selection : question.getConcernedEntities().values()) {
                assertFalse(selection.isSelected(), selection.getEntityReference() + " should not be selected");
            }
            return null;
        }).when(status).ask(any(), anyLong(), any());

        // Test
        DocumentsDeletingEvent event = new DocumentsDeletingEvent();
        this.deletingListener.onEvent(event, job, concernedEntities);

        // Check
        verify(status, times(1)).ask(any(), eq(5L), eq(TimeUnit.MINUTES));
        this.logCapture.ignoreAllMessages();
    }

    @Test
    void nonInteractive()
    {
        Request request = mock(Request.class);
        Job job = mock(Job.class);
        when(job.getRequest()).thenReturn(request);
        when(request.isInteractive()).thenReturn(false);
        JobStatus status = mock(JobStatus.class);
        when(job.getStatus()).thenReturn(status);

        // Test
        DocumentsDeletingEvent event = new DocumentsDeletingEvent();
        this.deletingListener.onEvent(event, job, null);

        // Verify
        assertEquals("XClass deleting listener will not check the document in non-interactive mode.",
            logCapture.getMessage(0));
        verifyNoInteractions(status);
    }

    @Test
    void cancel() throws Exception
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

        Query query = mock(Query.class);
        when(queryManager.createQuery(any(),any())).thenReturn(query);
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.bindValue(any(), any())).thenReturn(query);
        when(query.setWiki(any())).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("space.document"));

        when(resolver.resolve("space.document", EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("wiki", "space", "document"));

        InterruptedException e = new InterruptedException();
        doThrow(e).when(status).ask(any(), anyLong(), any());

        when(this.documentAccessBridge.isAdvancedUser(any())).thenReturn(true);

        // Test
        DocumentsDeletingEvent event = mock(DocumentsDeletingEvent.class);
        this.deletingListener.onEvent(event, job, concernedEntities);

        // Check
        verify(status, times(1)).ask(any(), eq(5L), eq(TimeUnit.MINUTES));
        verify(event).cancel(eq("Question has been interrupted."));
        assertEquals("Confirm question has been interrupted.", this.logCapture.getMessage(0));
    }

    @Test
    void simpleUser() throws Exception
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

        Query query = mock(Query.class);
        when(queryManager.createQuery(any(),any())).thenReturn(query);
        when(query.setLimit(anyInt())).thenReturn(query);
        when(query.bindValue(any(), any())).thenReturn(query);
        when(query.setWiki(any())).thenReturn(query);
        when(query.execute()).thenReturn(Arrays.asList("space.document"));

        when(resolver.resolve("space.document", EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("wiki", "space", "document"));

        when(this.documentAccessBridge.isAdvancedUser(any())).thenReturn(false);

        doAnswer(invocationOnMock -> {
            XClassBreakingQuestion question = invocationOnMock.getArgument(0);
            assertEquals(concernedEntities, question.getConcernedEntities());

            assertTrue(question.getFreePages().isEmpty());
            assertEquals(1, question.getXClassPages().size());
            assertEquals(1, question.getImpactedObjects().size());
            assertTrue(question.isRefactoringForbidden());
            return null;
        }).when(status).ask(any(), anyLong(), any());


        // Test
        DocumentsDeletingEvent event = mock(DocumentsDeletingEvent.class);
        this.deletingListener.onEvent(event, job, concernedEntities);

        // Check
        verify(status, times(1)).ask(any(), eq(1L), eq(TimeUnit.MINUTES));
        verify(event).cancel(eq("The question has been canceled because this refactoring is forbidden."));
    }
}
