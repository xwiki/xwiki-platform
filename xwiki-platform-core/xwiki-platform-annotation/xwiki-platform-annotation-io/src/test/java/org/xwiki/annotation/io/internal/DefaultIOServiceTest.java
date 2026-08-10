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

import java.util.Collection;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.xwiki.annotation.Annotation;
import org.xwiki.annotation.AnnotationConfiguration;
import org.xwiki.annotation.internal.AnnotationClassDocumentInitializer;
import org.xwiki.annotation.reference.internal.DefaultTypedStringEntityReferenceResolver;
import org.xwiki.component.internal.ContextComponentManagerProvider;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.properties.internal.DefaultConverterManager;
import org.xwiki.properties.internal.converter.ConvertUtilsConverter;
import org.xwiki.properties.internal.converter.EnumConverter;
import org.xwiki.sheet.SheetBinder;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.mandatory.XWikiCommentsDocumentInitializer;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.annotation.Annotation.AUTHOR_FIELD;
import static org.xwiki.annotation.Annotation.DATE_FIELD;
import static org.xwiki.annotation.Annotation.SELECTION_FIELD;
import static org.xwiki.annotation.Annotation.STATE_FIELD;
import static org.xwiki.annotation.Annotation.TARGET_FIELD;

/**
 * Tests that {@link DefaultIOService} attaches the temporary uploaded files (e.g. images inserted in an annotation
 * comment) to the exact document instance that is saved with the annotation, and does so before that document is saved
 * (otherwise the attachment would not be persisted). This is a wiring test: the
 * {@link TemporaryAttachmentSessionsManager} collaborator is mocked, so it verifies the delegation and its ordering but
 * not the end-to-end attachment persistence, which is exercised by the functional tests.
 * <p>
 * Also test that annotations attached to the current document are saved with an empty target field.
 *
 * @version $Id$
 */
@OldcoreTest
@ComponentList({
    XWikiCommentsDocumentInitializer.class,
    AnnotationClassDocumentInitializer.class,
    DefaultTypedStringEntityReferenceResolver.class,
    DefaultConverterManager.class,
    ContextComponentManagerProvider.class,
    EnumConverter.class,
    ConvertUtilsConverter.class
})
@ReferenceComponentList
class DefaultIOServiceTest
{
    private static final String WIKI_ID = "xwiki";

    private static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference(WIKI_ID, "Space", "Page");

    private static final String TARGET = "xwiki:Space.Page";

    private static final LocalDocumentReference COMMENTS_DOCUMENT_REFERENCE =
        XWikiCommentsDocumentInitializer.LOCAL_REFERENCE;

    @InjectMockComponents
    private DefaultIOService ioService;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private AnnotationConfiguration configuration;

    @MockComponent
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @MockComponent
    private JobProgressManager jobProgressManager;

    @MockComponent
    private ObservationManager observationManager;

    @MockComponent
    @Named("document")
    private SheetBinder sheetBinder;

    @MockComponent
    private ContextualLocalizationManager contextualLocalizationManager;

    private DocumentReference annotationClassReference;

    @BeforeEach
    void setUp()
    {
        this.annotationClassReference = new DocumentReference(COMMENTS_DOCUMENT_REFERENCE, new WikiReference(WIKI_ID));
        when(this.configuration.getAnnotationClassReference()).thenReturn(this.annotationClassReference);
        // Must be before initializeMandatoryDocuments because AnnotationClassDocumentInitializer only creates the
        // annotation class when the application reports itself as installed.
        when(this.configuration.isInstalled()).thenReturn(true);
        this.oldcore.getSpyXWiki().initializeMandatoryDocuments(this.oldcore.getXWikiContext());
    }

    @Test
    void addAnnotationAttachesTemporaryUploadedFilesToTheSavedDocument() throws Exception
    {
        saveNewDocument();
        XWikiContext context = this.oldcore.getXWikiContext();

        Annotation annotation = new Annotation("selection", "left", "right");
        annotation.setAuthor("xwiki:XWiki.Admin");
        annotation.set("uploadedFiles", "image1.png,image2.png");

        this.ioService.addAnnotation(TARGET, annotation);

        // The attachments must be added to the exact instance that is saved, and before it is saved (otherwise they
        // would not be persisted).
        ArgumentCaptor<XWikiDocument> documentCaptor = ArgumentCaptor.forClass(XWikiDocument.class);
        XWiki xwiki = this.oldcore.getSpyXWiki();
        InOrder inOrder = inOrder(this.temporaryAttachmentSessionsManager, xwiki);
        inOrder.verify(this.temporaryAttachmentSessionsManager)
            .attachTemporaryAttachmentsInDocument(documentCaptor.capture(), eq(List.of("image1.png", "image2.png")));
        inOrder.verify(xwiki).saveDocument(same(documentCaptor.getValue()), anyString(), eq(true), eq(context));

        // The uploaded files list must not be written as a (bogus) annotation object property.
        XWikiDocument savedDocument = xwiki.getDocument(DOCUMENT_REFERENCE, context);
        BaseObject annotationObject = savedDocument.getXObjects(this.annotationClassReference).get(0);
        assertEquals("", annotationObject.getStringValue("uploadedFiles"));
    }

    @Test
    void addAnnotationWithoutUploadedFilesDoesNotAttachAnything() throws Exception
    {
        saveNewDocument();
        XWikiContext context = this.oldcore.getXWikiContext();

        Annotation annotation = new Annotation("selection", "left", "right");
        annotation.setAuthor("xwiki:XWiki.Admin");

        this.ioService.addAnnotation(TARGET, annotation);

        verify(this.temporaryAttachmentSessionsManager, never()).attachTemporaryAttachmentsInDocument(any(), any());

        XWikiDocument savedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, context);
        assertEquals(1, savedDocument.getXObjects(this.annotationClassReference).size());
    }

    @Test
    void updateAnnotationsAttachesAndSavesEvenWhenNoOtherFieldChanged() throws Exception
    {
        XWikiDocument document = saveNewDocument();
        XWikiContext context = this.oldcore.getXWikiContext();
        document.newXObject(this.annotationClassReference, context);
        XWiki xwiki = this.oldcore.getSpyXWiki();
        xwiki.saveDocument(document, context);

        // An update whose only change is an uploaded image: updateObject would report no change, but the attachment
        // must still be persisted, forcing a save.
        Annotation annotation = new Annotation("0");
        annotation.set("uploadedFiles", "image.png");

        this.ioService.updateAnnotations(TARGET, List.of(annotation));

        ArgumentCaptor<XWikiDocument> documentCaptor = ArgumentCaptor.forClass(XWikiDocument.class);
        InOrder inOrder = inOrder(this.temporaryAttachmentSessionsManager, xwiki);
        inOrder.verify(this.temporaryAttachmentSessionsManager)
            .attachTemporaryAttachmentsInDocument(documentCaptor.capture(), eq(List.of("image.png")));
        inOrder.verify(xwiki)
            .saveDocument(same(documentCaptor.getValue()), anyString(), eq(true), eq(context));
    }

    @Test
    void addAnnotationSkipsTargetForSameDocument() throws Exception
    {

        saveNewDocument();
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setUser("xwiki:XWiki.Author");

        Annotation annotation = new Annotation("selection", "", "");
        annotation.setAuthor("xwiki:XWiki.Author");

        this.ioService.addAnnotation(TARGET, annotation);

        XWikiDocument savedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, context);
        BaseObject annotationObject = savedDocument.getXObjects(this.annotationClassReference).get(0);
        assertEquals("SAFE", annotationObject.getStringValue(STATE_FIELD));
        assertNotNull(annotationObject.getDateValue(DATE_FIELD));
        assertEquals("xwiki:XWiki.Author", annotationObject.getStringValue(AUTHOR_FIELD));
        // The target is not stored when it points to the very document the annotation object is stored in.
        assertEquals("", annotationObject.getStringValue(TARGET_FIELD));
        // The clone (not the cached document) is the one updated and saved.
        assertEquals("XWiki.Author", savedDocument.getAuthor());
    }

    @Test
    void getAnnotationsReturnsOnlyThoseMatchingTarget() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);

        // An annotation on the document content is stored with a blank target: it matches a document-type target.
        BaseObject blankTargetObject = document.newXObject(this.annotationClassReference, context);
        blankTargetObject.setStringValue(TARGET_FIELD, "");
        blankTargetObject.setStringValue(SELECTION_FIELD, "annotated text");
        blankTargetObject.setStringValue(STATE_FIELD, "SAFE");

        BaseObject matchingObject = document.newXObject(this.annotationClassReference, context);
        matchingObject.setStringValue(TARGET_FIELD, "Space.Page");
        matchingObject.setStringValue(STATE_FIELD, "SAFE");

        BaseObject otherTargetObject = document.newXObject(this.annotationClassReference, context);
        otherTargetObject.setStringValue(TARGET_FIELD, "OtherSpace.Page");

        this.oldcore.getSpyXWiki().saveDocument(document, context);

        assertEquals(2, this.ioService.getAnnotations(TARGET).size());
    }

    @Test
    void getAnnotationsIgnoresOrdinaryComments() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);

        // An ordinary comment: it is stored in the annotation class too, and also leaves the target blank, but it has
        // no selection and no state. Returning it would both list comments among the annotations and fail to
        // deserialize its empty state.
        BaseObject comment = document.newXObject(this.annotationClassReference, context);
        comment.setStringValue(TARGET_FIELD, "");

        BaseObject annotation = document.newXObject(this.annotationClassReference, context);
        annotation.setStringValue(TARGET_FIELD, "");
        annotation.setStringValue(SELECTION_FIELD, "annotated text");
        annotation.setStringValue(STATE_FIELD, "SAFE");

        this.oldcore.getSpyXWiki().saveDocument(document, context);

        Collection<Annotation> annotations = this.ioService.getAnnotations(TARGET);
        assertEquals(1, annotations.size());
        assertEquals("annotated text", annotations.iterator().next().getSelection());
    }

    @Test
    void getAnnotationsForObjectPropertyDoesNotReturnDocumentContentAnnotations() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        // The target points to an object property, not to the document itself.
        String target = "OBJECT_PROPERTY://xwiki:Space.Page^XWiki.MyClass[0].myProperty";

        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);

        // A document-content annotation (blank target): it must NOT be returned for an object property target,
        // otherwise content annotations would leak into every object property.
        BaseObject documentContentAnnotation = document.newXObject(this.annotationClassReference, context);
        documentContentAnnotation.setStringValue(TARGET_FIELD, "");

        // An annotation actually targeting the requested object property: it must be returned.
        BaseObject objectPropertyAnnotation = document.newXObject(this.annotationClassReference, context);
        objectPropertyAnnotation.setStringValue(TARGET_FIELD, "Space.Page^XWiki.MyClass[0].myProperty");
        objectPropertyAnnotation.setStringValue(STATE_FIELD, "SAFE");

        this.oldcore.getSpyXWiki().saveDocument(document, context);

        assertEquals(1, this.ioService.getAnnotations(target).size());
    }

    @Test
    void getAnnotationNotMatchingTargetReturnsNull() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        // Object 0 is unused and only here to push the object under test to index 1 (the annotation id used below).
        document.newXObject(this.annotationClassReference, context);
        BaseObject object = document.newXObject(this.annotationClassReference, context);
        object.setStringValue(TARGET_FIELD, "OtherSpace");
        this.oldcore.getSpyXWiki().saveDocument(document, context);

        assertNull(this.ioService.getAnnotation(TARGET, "1"));
    }

    @ParameterizedTest
    @ValueSource(strings = { "", "Space.Page" })
    void getAnnotationMatchingBlankOrExactTargetReturnsTargetField(String xObjectTarget) throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        // Object 0 is unused and only here to push the object under test to index 1 (the annotation id used below).
        document.newXObject(this.annotationClassReference, context);
        BaseObject object = document.newXObject(this.annotationClassReference, context);
        object.setStringValue(TARGET_FIELD, xObjectTarget);
        object.setStringValue(SELECTION_FIELD, "annotated text");
        object.setStringValue(STATE_FIELD, "SAFE");
        this.oldcore.getSpyXWiki().saveDocument(document, context);

        Annotation annotation = this.ioService.getAnnotation(TARGET, "1");
        // A blank stored target falls back to the requested target; a non-blank one is kept as-is.
        String expectedTarget = xObjectTarget.isEmpty() ? TARGET : xObjectTarget;
        assertEquals(expectedTarget, annotation.get("target"));
    }

    @Test
    void removeAnnotationWithBlankTargetOnDocumentRemovesIt() throws Exception
    {
        XWikiContext context = this.oldcore.getXWikiContext();
        context.setUser("xwiki:XWiki.Author");

        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        // Object 0 is unused and only here to push the object under test to index 1 (the annotation id used below).
        document.newXObject(this.annotationClassReference, context);
        // An annotation on the document content is stored with a blank target; removing it through the document
        // target must still delete the object.
        BaseObject object = document.newXObject(this.annotationClassReference, context);
        object.setStringValue(TARGET_FIELD, "");
        object.setStringValue(SELECTION_FIELD, "annotated text");
        this.oldcore.getSpyXWiki().saveDocument(document, context);

        this.ioService.removeAnnotation(TARGET, "1");

        XWikiDocument savedDocument = this.oldcore.getSpyXWiki().getDocument(DOCUMENT_REFERENCE, context);
        assertNull(savedDocument.getXObject(this.annotationClassReference, 1));
    }

    private XWikiDocument saveNewDocument() throws Exception
    {
        XWikiDocument document = new XWikiDocument(DOCUMENT_REFERENCE);
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());
        return document;
    }
}
