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
package org.xwiki.annotation.io.internal;

import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.reference.internal.DefaultTypedStringEntityReferenceResolver;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests that {@link DefaultIOService} attaches the temporary uploaded files (e.g. images inserted in an annotation
 * comment) to the exact document instance that is saved with the annotation, and does so before that document is
 * saved (otherwise the attachment would not be persisted). This is a wiring test: the
 * {@link TemporaryAttachmentSessionsManager} collaborator is mocked, so it verifies the delegation and its ordering
 * but not the end-to-end attachment persistence, which is exercised by the functional tests.
 *
 * @version $Id$
 */
@ComponentTest
@ComponentList({ DefaultTypedStringEntityReferenceResolver.class })
@ReferenceComponentList
class DefaultIOServiceTest
{
    private static final String TARGET = "xwiki:Space.Page";

    @InjectMockComponents
    private DefaultIOService ioService;

    @MockComponent
    private Execution execution;

    @MockComponent
    private AnnotationConfiguration configuration;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    private XWiki xwiki;

    private XWikiContext xcontext;

    private XWikiDocument clonedDocument;

    private BaseObject annotationObject;

    @BeforeEach
    void setUp() throws Exception
    {
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(this.execution.getContext()).thenReturn(executionContext);
        this.xcontext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(this.xcontext);

        this.xwiki = mock(XWiki.class);
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);

        // getDocument returns the (shared) cached instance; the service clones it before modifying it.
        XWikiDocument cachedDocument = mock(XWikiDocument.class);
        this.clonedDocument = mock(XWikiDocument.class);
        when(cachedDocument.clone()).thenReturn(this.clonedDocument);
        when(this.xwiki.getDocument(anyString(), eq(this.xcontext))).thenReturn(cachedDocument);

        DocumentReference annotationClassReference = new DocumentReference("xwiki", "XWiki", "AnnotationClass");
        when(this.configuration.getAnnotationClassReference()).thenReturn(annotationClassReference);

        this.annotationObject = mock(BaseObject.class);
        when(this.clonedDocument.createXObject(any(EntityReference.class), eq(this.xcontext))).thenReturn(0);
        when(this.clonedDocument.getXObject(annotationClassReference, 0)).thenReturn(this.annotationObject);
    }

    @Test
    void addAnnotationAttachesTemporaryUploadedFilesToTheSavedDocument() throws Exception
    {
        Annotation annotation = new Annotation("selection", "left", "right");
        annotation.setAuthor("xwiki:XWiki.Admin");
        annotation.set("uploadedFiles", "image1.png,image2.png");

        this.ioService.addAnnotation(TARGET, annotation);

        // The attachments must be added to the exact instance that is saved, and before it is saved (otherwise they
        // would not be persisted).
        InOrder inOrder = inOrder(this.temporaryAttachmentSessionsManager, this.xwiki);
        inOrder.verify(this.temporaryAttachmentSessionsManager)
            .attachTemporaryAttachmentsInDocument(this.clonedDocument, Arrays.asList("image1.png", "image2.png"));
        inOrder.verify(this.xwiki).saveDocument(eq(this.clonedDocument), anyString(), eq(true), eq(this.xcontext));
        // The uploaded files list must not be written as a (bogus) annotation object property.
        verify(this.annotationObject, never()).set(eq("uploadedFiles"), any(), any());
    }

    @Test
    void addAnnotationWithoutUploadedFilesDoesNotAttachAnything() throws Exception
    {
        Annotation annotation = new Annotation("selection", "left", "right");
        annotation.setAuthor("xwiki:XWiki.Admin");

        this.ioService.addAnnotation(TARGET, annotation);

        verify(this.temporaryAttachmentSessionsManager, never())
            .attachTemporaryAttachmentsInDocument(any(), any());
        verify(this.xwiki).saveDocument(eq(this.clonedDocument), anyString(), eq(true), eq(this.xcontext));
    }

    @Test
    void updateAnnotationsAttachesAndSavesEvenWhenNoOtherFieldChanged() throws Exception
    {
        // An update whose only change is an uploaded image: updateObject would report no change, but the attachment
        // must still be persisted, forcing a save.
        Annotation annotation = new Annotation("0");
        annotation.set("uploadedFiles", "image.png");

        this.ioService.updateAnnotations(TARGET, List.of(annotation));

        InOrder inOrder = inOrder(this.temporaryAttachmentSessionsManager, this.xwiki);
        inOrder.verify(this.temporaryAttachmentSessionsManager)
            .attachTemporaryAttachmentsInDocument(this.clonedDocument, Arrays.asList("image.png"));
        inOrder.verify(this.xwiki).saveDocument(eq(this.clonedDocument), anyString(), eq(true), eq(this.xcontext));
    }
}
