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
package org.xwiki.refactoring.script;

import java.util.Arrays;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.context.Execution;
import org.xwiki.job.JobExecutor;
import org.xwiki.model.EntityType;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.refactoring.job.PermanentlyDeleteRequest;
import org.xwiki.refactoring.job.RefactoringJobs;
import org.xwiki.refactoring.job.RestoreRequest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

public class RequestFactoryTest
{
    @Mock
    private DocumentAccessBridge documentAccessBridge;

    @Mock
    private ModelContext modelContext;

    private DocumentReference userReference = new DocumentReference("wiki", "Users", "Carol");

    private RequestFactory getRequestFactory()
    {
        return new RequestFactory(this.documentAccessBridge, this.modelContext);
    }

    @Before
    public void configure() throws Exception
    {
        MockitoAnnotations.initMocks(this);
        when(documentAccessBridge.getCurrentUserReference()).thenReturn(this.userReference);
        when(documentAccessBridge.getCurrentAuthorReference()).thenReturn(this.userReference);
    }

    @Test
    public void createRestoreRequest() throws Exception
    {
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);
        RestoreRequest restoreRequest =
            getRequestFactory().createRestoreRequest(documentIds);
        assertEquals(documentIds, restoreRequest.getDeletedDocumentIds());
        assertTrue(StringUtils.join(restoreRequest.getId(), '/')
            .startsWith(RefactoringJobs.RESTORE));
        assertTrue(restoreRequest.isCheckRights());
    }

    @Test
    public void createRestoreRequestBatchId() throws Exception
    {
        String batchId = "batch-id";
        RestoreRequest restoreRequest =
            getRequestFactory().createRestoreRequest(batchId);
        assertEquals(batchId, restoreRequest.getBatchId());
        assertTrue(StringUtils.join(restoreRequest.getId(), '/')
            .startsWith(RefactoringJobs.RESTORE));
        assertTrue(restoreRequest.isCheckRights());
    }

    @Test
    public void createPermanentlyDeleteRequest() throws Exception
    {
        List<Long> documentIds = Arrays.asList(1L, 2L, 3L);
        PermanentlyDeleteRequest permanentlyDeleteRequest =
            getRequestFactory().createPermanentlyDeleteRequest(documentIds);
        assertEquals(documentIds, permanentlyDeleteRequest.getDeletedDocumentIds());
        assertTrue(StringUtils.join(permanentlyDeleteRequest.getId(), '/')
            .startsWith(RefactoringJobs.PERMANENTLY_DELETE));
        assertTrue(permanentlyDeleteRequest.isCheckRights());
    }

    @Test
    public void createPermanentlyDeleteRequestBatch() throws Exception
    {
        String batchId = "batch-id";
        PermanentlyDeleteRequest permanentlyDeleteRequest =
            getRequestFactory().createPermanentlyDeleteRequest(batchId);
        assertEquals(batchId, permanentlyDeleteRequest.getBatchId());
        assertTrue(StringUtils.join(permanentlyDeleteRequest.getId(), '/')
            .startsWith(RefactoringJobs.PERMANENTLY_DELETE));
        assertTrue(permanentlyDeleteRequest.isCheckRights());
    }
}
