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
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
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
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultIOService}, including that it persists the temporary uploaded files (e.g. images inserted
 * in an annotation comment) on the very document instance that is saved with the annotation. See XWIKI-24546.
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

    private XWikiDocument cachedDocument;

    private XWikiDocument clonedDocument;

    private BaseObject annotationObject;

    private DocumentReference annotationClassReference;

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
        this.cachedDocument = mock(XWikiDocument.class);
        this.clonedDocument = mock(XWikiDocument.class);
        when(this.cachedDocument.clone()).thenReturn(this.clonedDocument);
        when(this.xwiki.getDocument(anyString(), eq(this.xcontext))).thenReturn(this.cachedDocument);

        this.annotationClassReference = new DocumentReference("xwiki", "XWiki", "AnnotationClass");
        when(this.configuration.getAnnotationClassReference()).thenReturn(this.annotationClassReference);

        this.annotationObject = mock(BaseObject.class);
        when(this.clonedDocument.newXObject(any(EntityReference.class), eq(this.xcontext)))
            .thenReturn(this.annotationObject);
        when(this.clonedDocument.getXObject(this.annotationClassReference, 0)).thenReturn(this.annotationObject);
    }

    @Test
    void addAnnotationAttachesTemporaryUploadedFilesToTheSavedDocument() throws Exception
    {
        Annotation annotation = new Annotation("selection", "left", "right");
        annotation.setAuthor("xwiki:XWiki.Admin");
        annotation.set("uploadedFiles", "image1.png,image2.png");

        this.ioService.addAnnotation(TARGET, annotation);

        // The attachments are added to the exact instance that is saved.
        verify(this.temporaryAttachmentSessionsManager)
            .attachTemporaryAttachmentsInDocument(this.clonedDocument, Arrays.asList("image1.png", "image2.png"));
        verify(this.xwiki).saveDocument(eq(this.clonedDocument), anyString(), eq(true), eq(this.xcontext));
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

        verify(this.temporaryAttachmentSessionsManager)
            .attachTemporaryAttachmentsInDocument(this.clonedDocument, Arrays.asList("image.png"));
        verify(this.xwiki).saveDocument(eq(this.clonedDocument), anyString(), eq(true), eq(this.xcontext));
    }

    @Test
    void addAnnotationSetsStateDateAuthorAndSkipsTargetForSameDocument() throws Exception
    {
        when(this.annotationObject.getDocumentReference())
            .thenReturn(new DocumentReference("xwiki", "Space", "Page"));
        when(this.xcontext.getUser()).thenReturn("xwiki:XWiki.Author");

        Annotation annotation = new Annotation("selection", "", "");
        annotation.setAuthor("xwiki:XWiki.Author");

        this.ioService.addAnnotation(TARGET, annotation);

        verify(this.annotationObject).set(Annotation.STATE_FIELD, "SAFE", this.xcontext);
        verify(this.annotationObject).set(eq(Annotation.DATE_FIELD), any(Date.class), eq(this.xcontext));
        verify(this.annotationObject).set(Annotation.AUTHOR_FIELD, annotation.getAuthor(), this.xcontext);
        // The target is not stored when it points to the very document the annotation object is stored in.
        verify(this.annotationObject, never()).set(eq(Annotation.TARGET_FIELD), any(), any());
        // The clone (not the cached document) is the one updated and saved.
        verify(this.cachedDocument, never()).setAuthor(anyString());
        verify(this.clonedDocument).setAuthor("xwiki:XWiki.Author");
    }

    @Test
    void getAnnotationsReturnsOnlyThoseMatchingTarget() throws Exception
    {
        // An annotation on the document content is stored with a blank target: it matches a document-type target.
        BaseObject blankTargetObject = mock(BaseObject.class);
        when(blankTargetObject.getStringValue(Annotation.TARGET_FIELD)).thenReturn("");
        when(blankTargetObject.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(blankTargetObject.getPropertyNames()).thenReturn(new String[] {});

        BaseObject matchingObject = mock(BaseObject.class);
        when(matchingObject.getStringValue(Annotation.TARGET_FIELD)).thenReturn("Space.Page");
        when(matchingObject.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(matchingObject.getPropertyNames()).thenReturn(new String[] {});

        BaseObject otherTargetObject = mock(BaseObject.class);
        when(otherTargetObject.getStringValue(Annotation.TARGET_FIELD)).thenReturn("OtherSpace.Page");

        when(this.cachedDocument.getXObjects(this.annotationClassReference))
            .thenReturn(List.of(blankTargetObject, matchingObject, otherTargetObject));

        assertEquals(2, this.ioService.getAnnotations(TARGET).size());
    }

    @Test
    void getAnnotationsForObjectPropertyDoesNotReturnDocumentContentAnnotations() throws Exception
    {
        // The target points to an object property, not to the document itself.
        String target = "OBJECT_PROPERTY://xwiki:Space.Page^XWiki.MyClass[0].myProperty";

        // A document-content annotation (blank target): it must NOT be returned for an object property target,
        // otherwise content annotations would leak into every object property (regression guarded by this test).
        BaseObject documentContentAnnotation = mock(BaseObject.class);
        when(documentContentAnnotation.getStringValue(Annotation.TARGET_FIELD)).thenReturn("");

        // An annotation actually targeting the requested object property: it must be returned.
        BaseObject objectPropertyAnnotation = mock(BaseObject.class);
        when(objectPropertyAnnotation.getStringValue(Annotation.TARGET_FIELD))
            .thenReturn("Space.Page^XWiki.MyClass[0].myProperty");
        when(objectPropertyAnnotation.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(objectPropertyAnnotation.getPropertyNames()).thenReturn(new String[] {});

        when(this.cachedDocument.getXObjects(this.annotationClassReference))
            .thenReturn(List.of(documentContentAnnotation, objectPropertyAnnotation));

        assertEquals(1, this.ioService.getAnnotations(target).size());
    }

    @Test
    void getAnnotationNotMatchingTargetReturnsNull() throws Exception
    {
        BaseObject object = mock(BaseObject.class);
        when(this.cachedDocument.getXObject(this.annotationClassReference, 1)).thenReturn(object);
        when(object.getStringValue(Annotation.TARGET_FIELD)).thenReturn("OtherSpace");

        assertNull(this.ioService.getAnnotation(TARGET, "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "Space.Page" })
    void getAnnotationMatchingBlankOrExactTargetReturnsTargetField(String xObjectTarget) throws Exception
    {
        BaseObject object = mock(BaseObject.class);
        when(this.cachedDocument.getXObject(this.annotationClassReference, 1)).thenReturn(object);
        when(object.getStringValue(Annotation.TARGET_FIELD)).thenReturn(xObjectTarget);
        when(object.getStringValue(Annotation.STATE_FIELD)).thenReturn("SAFE");
        when(object.getPropertyNames()).thenReturn(new String[] { "target" });
        BaseProperty targetProperty = mock(BaseProperty.class);
        when(object.get("target")).thenReturn(targetProperty);
        when(targetProperty.getValue()).thenReturn(xObjectTarget);

        Annotation annotation = this.ioService.getAnnotation(TARGET, "1");
        // A blank stored target falls back to the requested target; a non-blank one is kept as-is.
        String expectedTarget = xObjectTarget.isEmpty() ? TARGET : xObjectTarget;
        assertEquals(expectedTarget, annotation.get("target"));
    }

    @Test
    void removeAnnotationWithBlankTargetOnDocumentRemovesIt() throws Exception
    {
        // An annotation on the document content is stored with a blank target; removing it through the document
        // target must still delete the object.
        when(this.clonedDocument.getXObject(this.annotationClassReference, 1)).thenReturn(this.annotationObject);
        when(this.annotationObject.getStringValue(Annotation.TARGET_FIELD)).thenReturn("");
        when(this.xcontext.getUser()).thenReturn("xwiki:XWiki.Author");

        this.ioService.removeAnnotation(TARGET, "1");

        verify(this.clonedDocument).removeObject(this.annotationObject);
        verify(this.xwiki).saveDocument(eq(this.clonedDocument), anyString(), eq(true), eq(this.xcontext));
    }
}
