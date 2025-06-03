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
package org.xwiki.refactoring.internal.job;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.bridge.event.DocumentsDeletingEvent;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.refactoring.RefactoringConfiguration;
import org.xwiki.refactoring.batch.BatchOperation;
import org.xwiki.refactoring.batch.BatchOperationExecutor;
import org.xwiki.refactoring.job.EntityRequest;
import org.xwiki.refactoring.job.question.EntitySelection;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DeleteJob}.
 *
 * @version $Id$
 */
@ComponentTest
class DeleteJobTest extends AbstractEntityJobTest
{
    @InjectMockComponents
    private DeleteJob deleteJob;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    private RefactoringConfiguration configuration;

    @MockComponent
    private DocumentAccessBridge documentAccessBridge;

    @MockComponent
    private BatchOperationExecutor batchOperationExecutor;

    @Override
    protected Job getJob()
    {
        return this.deleteJob;
    }

    @Test
    void deleteDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");

        EntityRequest request = createRequest(documentReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);

        run(request);

        verify(this.observationManager).notify(any(DocumentsDeletingEvent.class), any(DeleteJob.class),
            eq(Map.of(documentReference, new EntitySelection(documentReference))));
        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).delete(documentReference);
    }

    @Test
    void deleteDocumentSkipRecyclebin() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        when(this.configuration.isRecycleBinSkippingActivated()).thenReturn(true);
        when(this.documentAccessBridge.isAdvancedUser()).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");

        EntityRequest request = createRequest(documentReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        request.setProperty(DeleteJob.SHOULD_SKIP_RECYCLE_BIN_PROPERTY, true);

        run(request);

        verify(this.observationManager).notify(any(DocumentsDeletingEvent.class), any(DeleteJob.class),
            eq(Map.of(documentReference, new EntitySelection(documentReference))));
        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).delete(documentReference, true);
    }

    @Test
    void deleteMissingDocument() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");

        run(createRequest(documentReference));

        assertEquals(1, getLogCapture().size());
        assertEquals(Level.WARN, getLogCapture().getLogEvent(0).getLevel());
        assertEquals("Skipping [wiki:Space.Page] because it doesn't exist.", getLogCapture().getMessage(0));
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
    }

    @Test
    void deleteDocumentWithoutDeleteRightUser() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, documentReference)).thenReturn(false);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, documentReference)).thenReturn(true);

        EntityRequest request = createRequest(documentReference);
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);

        run(request);

        assertEquals(1, getLogCapture().size());
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
        assertEquals("You are not allowed to delete [wiki:Space.Page].", getLogCapture().getMessage(0));
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
    }

    @Test
    void deleteDocumentWithoutDeleteRightAuthor() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.DELETE, userReference, documentReference)).thenReturn(true);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.DELETE, authorReference, documentReference)).thenReturn(false);

        EntityRequest request = createRequest(documentReference);
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);

        run(request);

        assertEquals(1, getLogCapture().size());
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
        assertEquals("You are not allowed to delete [wiki:Space.Page].", getLogCapture().getMessage(0));
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
    }

    @Test
    void deleteSpaceHomeDeep() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "WebHome");
        EntityRequest request = createRequest(documentReference);
        request.setDeep(true);

        run(request);

        // We only verify if the job fetches the documents from the space. The rest of the test is in #deleteSpace()
        verify(this.modelBridge, atLeastOnce()).getDocumentReferences(documentReference.getLastSpaceReference());
    }

    @Test
    void deleteSpace() throws Exception
    {
        SpaceReference spaceReference = new SpaceReference("Space", new WikiReference("wiki"));
        DocumentReference aliceReference = new DocumentReference("wiki", "Space", "Alice");
        DocumentReference bobReference = new DocumentReference("wiki", "Space", "Bob");
        when(this.modelBridge.getDocumentReferences(spaceReference)).thenReturn(
            List.of(aliceReference, bobReference));

        run(createRequest(spaceReference));

        // We only verify that the code tries to delete the documents.
        assertEquals(2, getLogCapture().size());
        assertEquals(Level.WARN, getLogCapture().getLogEvent(0).getLevel());
        assertEquals(Level.WARN, getLogCapture().getLogEvent(1).getLevel());
        assertEquals("Skipping [wiki:Space.Alice] because it doesn't exist.", getLogCapture().getMessage(0));
        assertEquals("Skipping [wiki:Space.Bob] because it doesn't exist.", getLogCapture().getMessage(1));
    }

    @Test
    void deleteUnsupportedEntity() throws Exception
    {
        WikiReference foo = new WikiReference("foo");

        run(createRequest(foo));

        assertEquals(2, getLogCapture().size());
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(1).getLevel());
        assertEquals("Unsupported entity type [WIKI].", getLogCapture().getMessage(0));
        assertEquals("Unsupported entity type [WIKI].", getLogCapture().getMessage(1));
        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
    }

    private void run(EntityRequest request) throws Exception
    {
        this.deleteJob.initialize(request);
        this.deleteJob.run();

        Throwable error = this.deleteJob.getStatus().getError();
        if (this.deleteJob.getStatus().getError() != null) {
            throw new Exception(error);
        }
    }

    private EntityRequest createRequest(EntityReference entityReference)
    {
        doAnswer(it -> {
            this.deleteJob.process(entityReference);
            return null;
        }).when(this.batchOperationExecutor).execute(any(BatchOperation.class));
        EntityRequest request = new EntityRequest();
        request.setEntityReferences(List.of(entityReference));
        return request;
    }
}
