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

import java.util.Arrays;
import java.util.Collections;
import java.util.Map;

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.job.Job;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.internal.LinkRefactoring;
import org.xwiki.refactoring.job.CopyRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit test for {@link CopyJob}.
 *
 * @version $Id$
 */
public class CopyJobTest extends AbstractEntityJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(CopyJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return this.mocker;
    }

    private CopyRequest createRequest(EntityReference source, EntityReference destination)
    {
        CopyRequest request = new CopyRequest();
        request.setEntityReferences(Arrays.asList(source));
        request.setDestination(destination);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setInteractive(false);
        return request;
    }

    @Test
    public void copyDocumentThrowErrorInCaseOfDocuments() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        DocumentReference copyReference = new DocumentReference("wiki", "Copy", "Page");
        when(this.modelBridge.copy(sourceReference, copyReference)).thenReturn(true);

        CopyRequest request = this.createRequest(sourceReference, copyReference);
        run(request);
        verify(this.mocker.getMockedLogger()).error("Unsupported destination entity type [{}] for a document.",
            EntityType.DOCUMENT);
    }

    @Test
    public void copyDocument() throws Throwable
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Foo");
        when(this.modelBridge.exists(sourceReference)).thenReturn(true);

        SpaceReference copyReference = new SpaceReference("wiki", "Copy", "Page");
        DocumentReference copyDestination = new DocumentReference("Foo", copyReference);
        when(this.modelBridge.copy(sourceReference, copyDestination)).thenReturn(true);

        CopyRequest request = this.createRequest(sourceReference, copyReference);
        Map<String, String> parameters = Collections.singletonMap("foo", "bar");
        request.setEntityParameters(sourceReference, parameters);
        run(request);
        verify(this.modelBridge).copy(sourceReference, copyDestination);

        LinkRefactoring linkRefactoring = getMocker().getInstance(LinkRefactoring.class);
        verify(linkRefactoring, never()).renameLinks(any(DocumentReference.class), any(DocumentReference.class),
            any(DocumentReference.class));
        verify(linkRefactoring).updateRelativeLinks(sourceReference, copyDestination);

        verify(this.modelBridge, never()).delete(any(DocumentReference.class));
        verify(this.modelBridge, never()).createRedirect(any(DocumentReference.class), any(DocumentReference.class));
        verify(this.mocker.getMockedLogger(), never()).error(any());
    }
}
