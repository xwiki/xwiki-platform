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

import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.xwiki.job.Job;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import ch.qos.logback.classic.Level;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link CreateJob}.
 * 
 * @version $Id$
 */
@ComponentTest
class CreateJobTest extends AbstractEntityJobTest
{
    @InjectMockComponents
    private CreateJob createJob;

    @Override
    protected Job getJob()
    {
        return this.createJob;
    }

    @Test
    void createDocumentFromTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference templateReference = new DocumentReference("wiki", "Code", "Template");
        when(this.modelBridge.exists(templateReference)).thenReturn(true);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        CreateRequest request = createRequest(documentReference, templateReference);
        request.setUserReference(userReference);
        request.setCheckRights(false);
        request.setAuthorReference(userReference);
        request.setCheckAuthorRights(false);
        run(request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).copy(templateReference, documentReference);
        verify(this.modelBridge).removeLock(documentReference);
    }

    @Test
    void createSpaceFromTemplate() throws Throwable
    {
        SpaceReference newSpaceReference = new SpaceReference("wiki", "Space");
        SpaceReference templateSpaceReference = new SpaceReference("wiki", "Code", "Template");

        DocumentReference templateDocumentReference = new DocumentReference("Index", templateSpaceReference);
        when(this.modelBridge.getDocumentReferences(templateSpaceReference)).thenReturn(
            List.of(templateDocumentReference));
        when(this.modelBridge.exists(templateDocumentReference)).thenReturn(true);

        DocumentReference newDocumentReference = new DocumentReference("Index", newSpaceReference);
        when(this.modelBridge.exists(newDocumentReference)).thenReturn(false);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");

        CreateRequest request = createRequest(newSpaceReference, templateSpaceReference);
        request.setUserReference(userReference);
        request.setCheckRights(false);
        request.setAuthorReference(userReference);
        request.setCheckAuthorRights(false);
        run(request);

        verify(this.modelBridge).setContextUserReference(userReference);
        verify(this.modelBridge).copy(templateDocumentReference, newDocumentReference);
        verify(this.modelBridge, never()).removeLock(any(DocumentReference.class));
    }

    @Test
    void createDocumentWithoutTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, null);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        run(request);

        verify(this.modelBridge).create(documentReference);
        verify(this.modelBridge).removeLock(documentReference);
        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
    }

    @Test
    void createSpaceWithoutTemplate() throws Throwable
    {
        SpaceReference spaceReference = new SpaceReference("wiki", "Space");
        DocumentReference spaceHomeReference = new DocumentReference("WebHome", spaceReference);
        when(this.modelBridge.exists(spaceHomeReference)).thenReturn(false);

        CreateRequest request = createRequest(spaceReference, null);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        run(request);

        verify(this.modelBridge).create(spaceHomeReference);
        verify(this.modelBridge, never()).copy(any(DocumentReference.class), any(DocumentReference.class));
        verify(this.modelBridge, never()).removeLock(any(DocumentReference.class));
    }

    @Test
    void createDocumentDeep() throws Throwable
    {
        DocumentReference spaceHomeReference = new DocumentReference("wiki", "Space", "WebHome");
        DocumentReference templateHomeReference =
            new DocumentReference("wiki", List.of("Code", "Template"), "WebHome");

        DocumentReference templateDocumentReference =
            new DocumentReference("Index", templateHomeReference.getLastSpaceReference());
        when(this.modelBridge.getDocumentReferences(templateHomeReference.getLastSpaceReference())).thenReturn(
            List.of(templateDocumentReference));
        when(this.modelBridge.exists(templateHomeReference)).thenReturn(true);
        when(this.modelBridge.exists(templateDocumentReference)).thenReturn(true);
        DocumentReference newDocumentReference =
            new DocumentReference("Index", spaceHomeReference.getLastSpaceReference());
        when(this.modelBridge.exists(newDocumentReference)).thenReturn(false);

        CreateRequest request = createRequest(spaceHomeReference, templateHomeReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        request.setDeep(true);
        run(request);

        verify(this.modelBridge).copy(templateDocumentReference, newDocumentReference);
    }

    @Test
    void createDocumentFromMissingTemplate() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference templateReference = new DocumentReference("wiki", "Code", "Template");
        when(this.modelBridge.exists(templateReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, templateReference);
        request.setCheckRights(false);
        request.setCheckAuthorRights(false);
        run(request);

        verifyNoCreate();
        assertEquals("Template document [wiki:Code.Template] does not exist.", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void createDocumentFromRestrictedTemplate() throws Throwable
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
        assertEquals("You are not allowed to view the template document [wiki:Code.Template].", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void createRestrictedDocument() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.EDIT, userReference, documentReference)).thenReturn(false);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.EDIT, authorReference, documentReference)).thenReturn(true);

        CreateRequest request = createRequest(documentReference, null);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        run(request);

        verifyNoCreate();
        assertEquals("You are not allowed to create the document [wiki:Space.Page].", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void createRestrictedDocumentAuthor() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(false);

        DocumentReference userReference = new DocumentReference("wiki", "Users", "Alice");
        when(this.authorization.hasAccess(Right.EDIT, userReference, documentReference)).thenReturn(true);

        DocumentReference authorReference = new DocumentReference("wiki", "Users", "Bob");
        when(this.authorization.hasAccess(Right.EDIT, authorReference, documentReference)).thenReturn(false);

        CreateRequest request = createRequest(documentReference, null);
        request.setUserReference(userReference);
        request.setAuthorReference(authorReference);
        request.setCheckRights(true);
        request.setCheckAuthorRights(true);
        run(request);

        verifyNoCreate();
        assertEquals("You are not allowed to create the document [wiki:Space.Page].", getLogCapture().getMessage(0));
        assertEquals(Level.ERROR, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void createExistingDocument() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.modelBridge.exists(documentReference)).thenReturn(true);

        run(createRequest(documentReference, null));

        verifyNoCreate();
        assertEquals("Skipping creation of document [wiki:Space.Page] because it already exists.",
            getLogCapture().getMessage(0));
        assertEquals(Level.WARN, getLogCapture().getLogEvent(0).getLevel());
    }

    @Test
    void skipDocumentCreation() throws Throwable
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");

        CreateRequest request = createRequest(documentReference, null);
        request.setSkippedEntities(List.of(documentReference));
        run(request);

        verifyNoCreate();
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
