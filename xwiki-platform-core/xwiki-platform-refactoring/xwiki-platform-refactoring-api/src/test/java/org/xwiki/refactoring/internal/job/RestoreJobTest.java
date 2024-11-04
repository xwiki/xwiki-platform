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

import org.junit.jupiter.api.Test;
import org.xwiki.job.Job;
import org.xwiki.job.JobGroupPath;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.refactoring.job.RestoreRequest;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.atMost;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RestoreJob}.
 *
 * @version $Id$
 */
@ComponentTest
class RestoreJobTest extends AbstractJobTest
{
    @InjectMockComponents
    private RestoreJob restoreJob;

    private WikiReference wikiReference = new WikiReference("mywiki");

    private DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

    @Override
    protected Job getJob()
    {
        return this.restoreJob;
    }

    @Test
    void restoreSingleDocument() throws Throwable
    {
        long deletedDocumentId = 13;

        RestoreRequest request = createRequest();
        request.setDeletedDocumentIds(List.of(deletedDocumentId));
        request.setCheckRights(true);
        run(request);

        verifyContext();

        // Verify that the specified document is restored.
        verify(this.modelBridge).restoreDeletedDocument(deletedDocumentId, request);
    }

    @Test
    void restoreBatch() throws Throwable
    {
        long deletedDocumentId1 = 13;
        long deletedDocumentId2 = 42;
        String batchId = "abc123";

        when(modelBridge.getDeletedDocumentIds(batchId))
            .thenReturn(List.of(deletedDocumentId1, deletedDocumentId2));

        RestoreRequest request = createRequest();
        request.setBatchId(batchId);
        run(request);

        verifyContext();

        // Verify that the individual documents from the batch are restored.
        verify(this.modelBridge).restoreDeletedDocument(deletedDocumentId1, request);
        verify(this.modelBridge).restoreDeletedDocument(deletedDocumentId2, request);
    }

    @Test
    void restoreBatchAndDocuments() throws Throwable
    {
        // Batch documents.
        long deletedDocumentId1 = 13;
        long deletedDocumentId2 = 42;
        String batchId = "abc123";

        // Individual documents.
        // Note: A has the same ID as a document in the batch, so it should be restored only once.
        long deletedDocumentIdA = deletedDocumentId2;
        long deletedDocumentIdB = 7;
        long deletedDocumentIdC = 3;

        when(modelBridge.getDeletedDocumentIds(batchId))
            .thenReturn(List.of(deletedDocumentId1, deletedDocumentId2));

        RestoreRequest request = createRequest();
        request.setBatchId(batchId);
        request.setDeletedDocumentIds(List.of(deletedDocumentIdA, deletedDocumentIdB, deletedDocumentIdC));
        run(request);

        verifyContext();

        // Verify that each document is restored exactly 1 time.
        verify(this.modelBridge, atMost(1)).restoreDeletedDocument(deletedDocumentId1, request);
        verify(this.modelBridge, atMost(1)).restoreDeletedDocument(deletedDocumentId2, request);
        verify(this.modelBridge, atMost(1)).restoreDeletedDocument(deletedDocumentIdB, request);
        verify(this.modelBridge, atMost(1)).restoreDeletedDocument(deletedDocumentIdC, request);
    }

    @Test
    void jobGroupAtWikiLevel() throws Exception
    {
        RestoreRequest request = createRequest();
        this.restoreJob.initialize(request);

        JobGroupPath expectedJobGroupPath = new JobGroupPath(wikiReference.getName(), RestoreJob.ROOT_GROUP);
        assertEquals(expectedJobGroupPath, this.restoreJob.getGroupPath());
    }

    @Test
    void failToExecuteIfNoWikiSpecified() throws Throwable
    {
        long deletedDocumentId = 13;

        RestoreRequest request = createRequest();
        request.setDeletedDocumentIds(List.of(deletedDocumentId));
        request.setWikiReference(null);

        try {
            run(request);
        } catch (IllegalArgumentException actual) {
            // Verify that the job threw an exception.
            Throwable expected = new IllegalArgumentException("No wiki reference was specified in the job request");

            assertEquals(expected.getClass(), actual.getClass());
            assertEquals(expected.getMessage(), actual.getMessage());
        }

        // Verify that the document is not restored.
        verify(this.modelBridge, never()).restoreDeletedDocument(deletedDocumentId, request);
        assertEquals("Exception thrown during job execution", getLogCapture().getMessage(0));
    }

    private RestoreRequest createRequest()
    {
        RestoreRequest request = new RestoreRequest();

        request.setCheckRights(false);
        request.setUserReference(userReference);
        request.setWikiReference(wikiReference);

        return request;
    }

    private void verifyContext()
    {
        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelContext).setCurrentEntityReference(wikiReference);
    }
}
