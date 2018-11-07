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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CreateJob}.
 * 
 * @version $Id$
 */
public class CreateJobTest extends AbstractEntityJobTest
{
    @Rule
    public MockitoComponentMockingRule<Job> mocker = new MockitoComponentMockingRule<Job>(CreateJob.class);

    @Override
    protected MockitoComponentMockingRule<Job> getMocker()
    {
        return this.mocker;
    }

    @Test
    public void createDocumentFromTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference templateReference = new DocumentReference("wiki", "Code", "Template");
        when(this.modelBridge.exists(templateReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        CreateRequest request = createRequest(documentReference, templateReference);
        request.setUserReference(userReference);
        request.setAuthorReference(userReference);
        request.setCheckRights(false);
        run(request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).copy(templateReference, documentReference);
        verify(this.modelBridge).removeLock(documentReference);
    }

    @Test
    public void createSpaceFromTemplate() throws Throwable
    {
        SpaceReference newSpaceReference = new SpaceReference("wiki", "Space");
        SpaceReference templateSpaceReference = new SpaceReference("wiki", "Code", "Template");

        DocumentReference templateDocumentReference = new DocumentReference("Index", templateSpaceReference);
        when(this.modelBridge.getDocumentReferences(templateSpaceReference)).thenReturn(
            Arrays.asList(templateDocumentReference));
        when(this.modelBridge.exists(templateDocumentReference)).thenReturn(true);

        DocumentReference newDocumentReference = new DocumentReference("Index", newSpaceReference);
        when(this.modelBridge.exists(newDocumentReference)).thenReturn(false);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        CreateRequest request = createRequest(newSpaceReference, templateSpaceReference);
        request.setUserReference(userReference);
        request.setCheckRights(false);
        run(request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).copy(templateDocumentReference, newDocumentReference);
        verify(this.modelBridge, never()).removeLock(any(DocumentReference.class));
    }

    @Test
    public void createDocumentWithoutTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, null);
        request.setCheckRights(false);
        run(request);

        verify(this.modelBridge).create(documentReference);
        verify(this.modelBridge).removeLock(documentReference);
        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
    }

    @Test
    public void createSpaceWithoutTemplate() throws Throwable
    {
        SpaceReference spaceReference = new SpaceReference("wiki", "Space");
        DocumentReference spaceHomeReference = new DocumentReference("WebHome", spaceReference);
        when(this.modelBridge.exists(spaceHomeReference)).thenReturn(false);

        CreateRequest request = createRequest(spaceReference, null);
        request.setCheckRights(false);
        run(request);

        verify(this.modelBridge).create(spaceHomeReference);
        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
        verify(this.modelBridge, never()).removeLock(any(DocumentReference.class));
    }

    @Test
    public void createDocumentDeep() throws Throwable
    {
        DocumentReference spaceHomeReference = new DocumentReference("wiki", "Space", "WebHome");
        DocumentReference templateHomeReference =
            new DocumentReference("wiki", Arrays.asList("Code", "Template"), "WebHome");

        DocumentReference templateDocumentReference =
            new DocumentReference("Index", templateHomeReference.getLastSpaceReference());
        when(this.modelBridge.getDocumentReferences(templateHomeReference.getLastSpaceReference())).thenReturn(
            Arrays.asList(templateDocumentReference));
        when(this.modelBridge.exists(templateDocumentReference)).thenReturn(true);

        DocumentReference newDocumentReference =
            new DocumentReference("Index", spaceHomeReference.getLastSpaceReference());
        when(this.modelBridge.exists(newDocumentReference)).thenReturn(false);

        CreateRequest request = createRequest(spaceHomeReference, templateHomeReference);
        request.setCheckRights(false);
        request.setDeep(true);
        run(request);

        verify(this.modelBridge).copy(templateDocumentReference, newDocumentReference);
    }

    @Test
    public void createDocumentFromMissingTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference templateReference = new DocumentReference("wiki", "Code", "Template");
        when(this.modelBridge.exists(templateReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, templateReference);
        request.setCheckRights(false);
        run(request);

        verifyNoCreate();
        verify(this.mocker.getMockedLogger()).error("Template document [{}] does not exist.", templateReference);
    }

    @Test
    public void createDocumentFromRestrictedTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.EDIT, userReference, documentReference)).thenReturn(true);

        DocumentReference templateReference = new DocumentReference("wiki", "Code", "Template");
        when(this.authorization.hasAccess(Right.VIEW, userReference, templateReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, templateReference);
        request.setUserReference(userReference);
        request.setAuthorReference(userReference);
        run(request);

        verifyNoCreate();
        verify(this.mocker.getMockedLogger()).error("You are not allowed to view the template document [{}].",
            templateReference);
    }

    @Test
    public void createRestrictedDocument() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.EDIT, userReference, documentReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, null);
        request.setUserReference(userReference);
        run(request);

        verifyNoCreate();
        verify(this.mocker.getMockedLogger()).error("You are not allowed to create the document [{}].",
            documentReference);
    }

    @Test
    public void createExistingDocument() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        run(createRequest(documentReference, null));

        verifyNoCreate();
        verify(this.mocker.getMockedLogger()).warn("Skipping creation of document [{}] because it already exists.",
            documentReference);
    }

    @Test
    public void skipDocumentCreation() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");

        CreateRequest request = createRequest(documentReference, null);
        request.setSkippedEntities(Arrays.<EntityReference>asList(documentReference));
        run(request);

        verifyNoCreate();
        verify(this.mocker.getMockedLogger()).debug("Skipping creation of document [{}], as specified in the request.",
            documentReference);
    }

    private CreateRequest createRequest(EntityReference targetReference, EntityReference templateReference)
    {
        CreateRequest request = new CreateRequest();
        request.setEntityReferences(Collections.singletonList(targetReference));
        request.setTemplateReference(templateReference);
        return request;
    }

    private void verifyNoCreate()
    {
        verify(this.modelBridge, never()).create(any(DocumentReference.class));
        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
    }
}
