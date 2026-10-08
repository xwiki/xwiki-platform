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
package com.xpn.xwiki.web;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.suigeneris.jrcs.rcs.Version;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.Request;
import org.xwiki.context.Execution;
import org.xwiki.diff.Conflict;
import org.xwiki.diff.ConflictDecision;
import org.xwiki.job.Job;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.validation.EntityNameValidation;
import org.xwiki.model.validation.EntityNameValidationConfiguration;
import org.xwiki.model.validation.EntityNameValidationManager;
import org.xwiki.refactoring.job.CreateRequest;
import org.xwiki.refactoring.script.RefactoringScriptService;
import org.xwiki.refactoring.script.RequestFactory;
import org.xwiki.script.service.ScriptService;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.DocumentRevisionConflictException;
import org.xwiki.store.merge.MergeConflictDecisionsManager;
import org.xwiki.store.merge.MergeDocumentResult;
import org.xwiki.store.merge.MergeManager;
import org.xwiki.test.annotation.ComponentList;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.doc.XWikiLock;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link SaveAction}.
 *
 * @version $Id$
 */
@ComponentList
@ReferenceComponentList
@OldcoreTest(mockXWiki = false)
class SaveActionTest
{
    private static final DocumentReference USER_REFERENCE = new DocumentReference("xwiki", "XWiki", "FooBar");

    private static final DocumentReference DOCUMENT_REFERENCE =
        new DocumentReference(new DocumentReference("xwiki", "Space", "Page"), Locale.ROOT);

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @MockComponent
    private EntityNameValidationManager entityNameValidationManager;

    @MockComponent
    private EntityNameValidationConfiguration entityNameValidationConfiguration;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("xwikiproperties")
    private ConfigurationSource propertiesConf;

    @MockComponent
    private ContextualAuthorizationManager autorization;

    @MockComponent
    private DocumentRevisionProvider documentRevisionProvider;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @MockComponent
    private MergeManager mergeManager;

    @MockComponent
    private MergeConflictDecisionsManager conflictDecisionsManager;

    @MockComponent
    private Container container;

    private XWikiContext context;

    @InjectMockComponents
    private SaveAction saveAction;

    private XWikiRequest mockRequest;

    private XWikiResponse mockResponse;

    private XWikiDocument mockDocument;

    private XWikiDocument mockClonedDocument;

    private EditForm mockForm;

    private XWiki xWiki;

    @Mock(name = "currentUser")
    private UserReference currentUserReference;

    @Mock(name = "effectiveAuthor")
    private UserReference effectiveAuthor;

    private DocumentAuthors mockAuthors;

    @BeforeEach
    void setup()
    {
        this.context = this.oldcore.getXWikiContext();

        this.xWiki = mock(XWiki.class);
        this.context.setWiki(this.xWiki);

        this.mockRequest = mock(XWikiRequest.class);
        when(this.mockRequest.getEffectiveAuthor()).thenReturn(Optional.of(this.effectiveAuthor));
        this.context.setRequest(this.mockRequest);
        Request containerRequest = mock();
        when(this.container.getRequest()).thenReturn(containerRequest);
        when(containerRequest.getParameter(any())).then(
            invocation -> this.mockRequest.getParameter(invocation.getArgument(0)));

        this.mockResponse = mock(XWikiResponse.class);
        this.context.setResponse(this.mockResponse);

        this.mockDocument = mock(XWikiDocument.class);
        this.context.setDoc(this.mockDocument);

        this.mockClonedDocument = mock(XWikiDocument.class);
        when(this.mockDocument.clone()).thenReturn(this.mockClonedDocument);

        this.mockForm = mock(EditForm.class);
        this.context.setForm(this.mockForm);
        when(this.entityNameValidationConfiguration.useValidation()).thenReturn(false);

        this.context.setUserReference(USER_REFERENCE);
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(this.currentUserReference);

        this.mockAuthors = mock(DocumentAuthors.class);
        when(this.mockClonedDocument.getAuthors()).thenReturn(mockAuthors);
    }

    @Test
    void newDocumentInvalidName() throws Exception
    {
        when(mockDocument.isNew()).thenReturn(true);
        DocumentReference documentReference = new DocumentReference("XWiki", "Foo", "Bar");
        when(mockDocument.getDocumentReference()).thenReturn(documentReference);
        when(this.entityNameValidationConfiguration.useValidation()).thenReturn(true);
        EntityNameValidation entityNameValidation = mock(EntityNameValidation.class);
        when(this.entityNameValidationManager.getEntityReferenceNameStrategy()).thenReturn(entityNameValidation);
        when(entityNameValidation.isValid(documentReference)).thenReturn(false);

        assertTrue(saveAction.save(this.context));
        assertEquals("entitynamevalidation.create.invalidname", context.get("message"));
        assertArrayEquals(new Object[] {"Foo.Bar"}, (Object[]) context.get("messageParameters"));
    }

    @Test
    void validSave() throws Exception
    {
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        XWikiLock xWikiLockMock = mock(XWikiLock.class);
        when(mockClonedDocument.getLock(this.context)).thenReturn(xWikiLockMock);
        when(mockForm.getTemplate()).thenReturn("");

        assertFalse(saveAction.save(this.context));
        assertEquals(Map.of("newVersion", "1.2"), saveAction.getJSONAnswer(context));

        verify(mockAuthors).setOriginalMetadataAuthor(this.currentUserReference);
        verify(mockAuthors).setEffectiveMetadataAuthor(this.effectiveAuthor);
        verify(mockClonedDocument).setMetaDataDirty(true);
        verify(this.xWiki).checkSavingDocument(USER_REFERENCE, mockClonedDocument, "My Changes", false, this.context);
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
        verify(mockClonedDocument).removeLock(this.context);
    }

    @Test
    void validSaveNewTranslation() throws Exception
    {
        when(mockForm.getLanguage()).thenReturn("fr");
        when(mockClonedDocument.getTranslatedDocument("fr", this.context)).thenReturn(mockClonedDocument);
        when(mockClonedDocument.getDocumentReference()).thenReturn(new DocumentReference("xwiki", "My", "Page"));
        when(mockClonedDocument.getStore()).thenReturn(this.oldcore.getMockStore());
        when(xWiki.getStore()).thenReturn(this.oldcore.getMockStore());
        context.put("ajax", true);
        when(xWiki.isMultiLingual(this.context)).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.1");
        when(mockRequest.getParameter("isNew")).thenReturn("true");
        assertFalse(saveAction.save(this.context));
        assertEquals(Map.of("newVersion", "1.1"), saveAction.getJSONAnswer(context));
        verify(this.xWiki).checkSavingDocument(eq(USER_REFERENCE), any(XWikiDocument.class), eq(""), eq(false),
            eq(this.context));
        verify(this.xWiki).saveDocument(any(XWikiDocument.class), eq(""), eq(false), eq(this.context));
    }

    @Test
    void validSaveOldTranslation() throws Exception
    {
        when(mockForm.getLanguage()).thenReturn("fr");
        XWikiDocument translation = mock(XWikiDocument.class);
        when(mockForm.getTemplate()).thenReturn("");
        when(mockClonedDocument.getTranslatedDocument("fr", this.context)).thenReturn(translation);
        when(translation.clone()).thenReturn(mockClonedDocument);
        when(xWiki.getStore()).thenReturn(this.oldcore.getMockStore());
        context.put("ajax", true);
        when(xWiki.isMultiLingual(this.context)).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.3");
        when(mockRequest.getParameter("editingVersionDate")).thenReturn("1000");
        when(translation.getRCSVersion()).thenReturn(new Version("1.3"));
        when(translation.getDate()).thenReturn(new Date(0));
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.4"));
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        assertFalse(saveAction.save(this.context));
        assertEquals(Map.of("newVersion", "1.4"), saveAction.getJSONAnswer(context));
        verify(this.xWiki).checkSavingDocument(USER_REFERENCE, mockClonedDocument, "My Changes", false, this.context);
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
    }

    /**
     * This tests aims at checking the usecase when uploading an image in the WYSIWYG editor before the first save and
     * saving afterwards.
     */
    @Test
    void validSaveRequestImageUploadAndConflictCheck() throws Exception
    {
        when(mockDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        XWikiLock xWikiLockMock2 = mock(XWikiLock.class);
        when(mockClonedDocument.getLock(this.context)).thenReturn(xWikiLockMock2);
        when(mockForm.getTemplate()).thenReturn("");
        when(this.propertiesConf.getProperty("edit.conflictChecking.enabled")).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.1");
        context.put("ajax", true);
        when(mockRequest.getParameter("forceSave")).thenReturn("");
        when(mockRequest.getParameter("isNew")).thenReturn("true");

        when(mockDocument.getDate()).thenReturn(new Date(42));
        when(mockRequest.getParameter("editingVersionDate")).thenReturn("43");
        XWikiDocument xWikiDocumentMock = mock(XWikiDocument.class);
        when(this.documentRevisionProvider.getRevision(mockDocument, "1.1")).thenReturn(xWikiDocumentMock);
        when(mockDocument.getContentDiff("1.1", "1.2", context)).thenReturn(Collections.emptyList());
        when(mockDocument.getMetaDataDiff("1.1", "1.2", context)).thenReturn(Collections.emptyList());
        when(mockDocument.getObjectDiff("1.1", "1.2", context)).thenReturn(Collections.emptyList());

        assertFalse(saveAction.save(this.context));
        assertEquals(Map.of("newVersion", "1.2"), saveAction.getJSONAnswer(context));

        verify(mockAuthors).setOriginalMetadataAuthor(this.currentUserReference);
        verify(mockAuthors).setEffectiveMetadataAuthor(this.effectiveAuthor);
        verify(mockClonedDocument).setMetaDataDirty(true);
        verify(this.xWiki).checkSavingDocument(USER_REFERENCE, mockClonedDocument, "My Changes", false, this.context);
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
        verify(mockClonedDocument).removeLock(this.context);
    }

    @Test
    void saveFromTemplate() throws Exception
    {
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("3.2"));
        when(this.mockForm.getTemplate()).thenReturn("TemplateSpace.TemplateDocument");
        DocumentReference templateReference =
            new DocumentReference(context.getWikiId(), "TemplateSpace", "TemplateDocument");

        when(this.autorization.hasAccess(Right.VIEW, templateReference)).thenReturn(false);

        assertFalse(this.saveAction.save(this.context));

        verify(this.mockClonedDocument, never()).readFromTemplate(templateReference, this.context);

        when(this.autorization.hasAccess(Right.VIEW, templateReference)).thenReturn(true);
        RefactoringScriptService refactoring = mock(RefactoringScriptService.class);
        RequestFactory requestFactory = mock(RequestFactory.class);
        when(refactoring.getRequestFactory()).thenReturn(requestFactory);
        CreateRequest request = mock(CreateRequest.class);
        when(requestFactory.createCreateRequest(any())).thenReturn(request);
        Job job = mock(Job.class);
        when(refactoring.create(request)).thenReturn(job);
        this.componentManager.registerComponent(ScriptService.class, "refactoring", refactoring);

        assertFalse(this.saveAction.save(this.context));

        verify(this.mockClonedDocument).readFromTemplate(templateReference, this.context);
    }

    @Test
    void saveSectionWithAttachmentUpload() throws Exception
    {
        when(mockRequest.getParameter("section")).thenReturn("2");
        when(xWiki.hasSectionEdit(context)).thenReturn(true);
        XWikiDocument sectionDoc = mock(XWikiDocument.class);
        when(mockClonedDocument.clone()).thenReturn(sectionDoc);
        String sectionContent = "Some content from the section";
        when(sectionDoc.getContent()).thenReturn(sectionContent);
        String fullContent = "Some previous content " + sectionContent + " some after content";
        when(mockClonedDocument.updateDocumentSection(2, sectionContent + "\n")).thenReturn(fullContent);
        List<XWikiAttachment> attachmentList = mock(List.class);
        when(sectionDoc.getAttachmentList()).thenReturn(attachmentList);
        String comment = "Some comment";
        when(sectionDoc.getComment()).thenReturn(comment);
        when(sectionDoc.isMinorEdit()).thenReturn(true);
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("4.1"));

        assertFalse(this.saveAction.save(this.context));

        verify(sectionDoc).readFromForm(any(), eq(context));
        verify(mockClonedDocument).setAttachmentList(attachmentList);
        verify(mockClonedDocument).setContent(fullContent);
        verify(mockClonedDocument).setComment(comment);
        verify(mockClonedDocument).setMinorEdit(true);
        verify(mockClonedDocument, never()).readFromForm(any(), any());
    }

    @Test
    void saveAgainAfterConcurrentSave() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        when(mockDocument.getDocumentReference()).thenReturn(documentReference);
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        when(mockForm.getTemplate()).thenReturn("");
        // Another request saves the document after this request loaded it.
        doThrow(new DocumentRevisionConflictException(documentReference, "1.2", "1.3")).when(this.xWiki)
            .saveDocument(mockClonedDocument, "My Changes", false, this.context);
        XWikiDocument latestDocument = mock();
        XWikiDocument latestClonedDocument = mock();
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(latestDocument);
        when(latestDocument.clone()).thenReturn(latestClonedDocument);
        when(latestClonedDocument.getAuthors()).thenReturn(this.mockAuthors);
        when(latestClonedDocument.getComment()).thenReturn("My Changes");
        when(latestClonedDocument.getRCSVersion()).thenReturn(new Version("1.4"));
        XWikiLock lock = mock();
        when(latestClonedDocument.getLock(this.context)).thenReturn(lock);

        assertFalse(saveAction.save(this.context));

        assertEquals(Map.of("newVersion", "1.4"), saveAction.getJSONAnswer(context));
        // The request is applied again on the latest version of the document.
        verify(latestClonedDocument).readFromForm(this.mockForm, this.context);
        verify(this.xWiki).saveDocument(latestClonedDocument, "My Changes", false, this.context);
        verify(latestClonedDocument).removeLock(this.context);
    }

    @Test
    void saveConflictingWithConcurrentSave() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        DocumentReference documentReferenceWithLocale = new DocumentReference(documentReference, Locale.ROOT);
        when(mockDocument.getDocumentReference()).thenReturn(documentReference);
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(mockDocument);
        when(mockClonedDocument.getDocumentReferenceWithLocale()).thenReturn(documentReferenceWithLocale);
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        when(mockForm.getTemplate()).thenReturn("");
        context.put("ajax", true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.2");
        when(mockRequest.getParameter("editingVersionDate")).thenReturn("1000");
        // The document loaded by this request is the one the user edited, so the conflict check before saving passes.
        when(mockDocument.getRCSVersion()).thenReturn(new Version("1.2"));
        when(mockDocument.getDate()).thenReturn(new Date(500));
        // Other requests save the document each time after this request loaded it.
        doThrow(new DocumentRevisionConflictException(documentReference, "1.2", "1.3")).when(this.xWiki)
            .saveDocument(mockClonedDocument, "My Changes", false, this.context);
        XWikiDocument latestDocument = mock();
        when(latestDocument.getRCSVersion()).thenReturn(new Version("1.3"));
        when(latestDocument.getDate()).thenReturn(new Date(2000));
        when(this.xWiki.getDocument(documentReferenceWithLocale, this.context)).thenReturn(latestDocument);
        StringWriter responseContent = mockResponseContent();

        assertTrue(saveAction.save(this.context));

        verify(this.xWiki, times(5)).saveDocument(mockClonedDocument, "My Changes", false, this.context);
        verify(this.mockResponse).setStatus(409);
        assertEquals(String.format("{\"previousVersion\":\"1.2\",\"previousVersionDate\":\"%s\","
            + "\"latestVersion\":\"1.3\",\"latestVersionDate\":\"%s\"}", new Date(1000), new Date(2000)),
            responseContent.toString());
        verify(mockClonedDocument, never()).removeLock(this.context);
    }

    @Test
    void saveConflictingWithConcurrentSaveWithoutAjax() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("xwiki", "Space", "Page");
        when(mockDocument.getDocumentReference()).thenReturn(documentReference);
        when(this.xWiki.getDocument(documentReference, this.context)).thenReturn(mockDocument);
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        when(mockForm.getTemplate()).thenReturn("");
        DocumentRevisionConflictException conflict =
            new DocumentRevisionConflictException(documentReference, "1.2", "1.3");
        doThrow(conflict).when(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);

        assertSame(conflict, assertThrows(DocumentRevisionConflictException.class,
            () -> saveAction.save(this.context)));
        verify(this.xWiki, times(5)).saveDocument(mockClonedDocument, "My Changes", false, this.context);
    }

    @Test
    void overrideWithLatestConflictVersion() throws Exception
    {
        mockConflictDecision("override", "1.3");

        assertFalse(saveAction.save(this.context));

        assertEquals(Map.of("newVersion", "1.4"), saveAction.getJSONAnswer(context));
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
        verify(this.mergeManager, never()).mergeDocument(any(), any(), any(), any());
    }

    @Test
    void overrideWithOutdatedConflictVersion() throws Exception
    {
        mockConflictDecision("override", "1.2");
        XWikiDocument conflictDocument = mockRevision("1.2");
        MergeDocumentResult mergeResult = mock();
        when(this.mergeManager.mergeDocument(same(conflictDocument), same(mockDocument), same(mockClonedDocument),
            any())).thenReturn(mergeResult);

        assertFalse(saveAction.save(this.context));

        // The changes saved after the version the user overrides are merged.
        assertEquals(Map.of("mergedDocument", "true", "newVersion", "1.4"), saveAction.getJSONAnswer(context));
        verify(this.mergeManager).mergeDocument(any(), any(), any(), any());
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
    }

    @Test
    void mergeWithOutdatedConflictVersion() throws Exception
    {
        mockConflictDecision("merge", "1.2");
        when(mockRequest.getParameterValues("mergeChoices")).thenReturn(new String[] {"42=current"});
        XWikiDocument previousDocument = mockRevision("1.1");
        XWikiDocument conflictDocument = mockRevision("1.2");
        // The decisions solve the conflicts with the version the user saw.
        MergeDocumentResult decisionsResult = mock();
        when(decisionsResult.hasConflicts()).thenReturn(true);
        when(this.mergeManager.mergeDocument(same(previousDocument), same(conflictDocument),
            same(mockClonedDocument), any())).thenReturn(decisionsResult);
        MergeDocumentResult mergeResult = mock();
        when(this.mergeManager.mergeDocument(same(conflictDocument), same(mockDocument), same(mockClonedDocument),
            any())).thenReturn(mergeResult);

        assertFalse(saveAction.save(this.context));

        assertEquals(Map.of("mergedDocument", "true", "newVersion", "1.4"), saveAction.getJSONAnswer(context));
        InOrder inOrder = inOrder(this.conflictDecisionsManager, this.mergeManager);
        inOrder.verify(this.conflictDecisionsManager).recordDecision(DOCUMENT_REFERENCE, USER_REFERENCE, "42",
            ConflictDecision.DecisionType.CURRENT, null);
        inOrder.verify(this.mergeManager).mergeDocument(same(previousDocument), same(conflictDocument),
            same(mockClonedDocument), any());
        inOrder.verify(this.conflictDecisionsManager).removeConflictDecisionList(DOCUMENT_REFERENCE,
            USER_REFERENCE);
        inOrder.verify(this.mergeManager).mergeDocument(same(conflictDocument), same(mockDocument),
            same(mockClonedDocument), any());
        verify(this.xWiki).saveDocument(mockClonedDocument, "My Changes", false, this.context);
    }

    @Test
    void mergeConflictingWithChangesAfterConflictVersion() throws Exception
    {
        mockConflictDecision("merge", "1.2");
        XWikiDocument previousDocument = mockRevision("1.1");
        XWikiDocument conflictDocument = mockRevision("1.2");
        XWikiDocument unmergedDocument = mock();
        when(mockClonedDocument.clone()).thenReturn(unmergedDocument);
        MergeDocumentResult decisionsResult = mock();
        when(this.mergeManager.mergeDocument(same(previousDocument), same(conflictDocument),
            same(mockClonedDocument), any())).thenReturn(decisionsResult);
        MergeDocumentResult conflictingResult = mock();
        when(conflictingResult.hasConflicts()).thenReturn(true);
        when(this.mergeManager.mergeDocument(same(conflictDocument), same(mockDocument), same(mockClonedDocument),
            any())).thenReturn(conflictingResult);
        // The conflicts the user has to solve now, between the version the user started from and the latest one.
        MergeDocumentResult newConflictsResult = mock();
        Conflict<String> newConflict = mock();
        List<Conflict<?>> newConflicts = List.of(newConflict);
        when(newConflictsResult.getConflicts(MergeDocumentResult.DocumentPart.CONTENT)).thenReturn(newConflicts);
        when(this.mergeManager.mergeDocument(same(previousDocument), same(mockDocument), same(unmergedDocument),
            any())).thenReturn(newConflictsResult);
        StringWriter responseContent = mockResponseContent();

        assertTrue(saveAction.save(this.context));

        verify(this.conflictDecisionsManager).recordConflicts(DOCUMENT_REFERENCE, USER_REFERENCE,
            newConflicts);
        verify(this.mockResponse).setStatus(409);
        assertEquals(String.format("{\"previousVersion\":\"1.1\",\"previousVersionDate\":\"%s\","
            + "\"latestVersion\":\"1.3\",\"latestVersionDate\":\"%s\"}", new Date(1000), new Date(2000)),
            responseContent.toString());
        verify(this.xWiki, never()).saveDocument(any(), any(), anyBoolean(), any());
    }

    @Test
    void mergeWithDeletedConflictVersion() throws Exception
    {
        mockConflictDecision("merge", "1.2");
        XWikiDocument previousDocument = mockRevision("1.1");
        XWikiDocument unmergedDocument = mock();
        when(mockClonedDocument.clone()).thenReturn(unmergedDocument);
        MergeDocumentResult newConflictsResult = mock();
        when(this.mergeManager.mergeDocument(same(previousDocument), same(mockDocument), same(unmergedDocument),
            any())).thenReturn(newConflictsResult);
        StringWriter responseContent = mockResponseContent();

        assertTrue(saveAction.save(this.context));

        verify(this.mockResponse).setStatus(409);
        assertTrue(responseContent.toString().contains("\"latestVersion\":\"1.3\""));
        verify(this.xWiki, never()).saveDocument(any(), any(), anyBoolean(), any());
    }

    /**
     * Mocks a request saving the user changes after the user decided how to solve the merge conflicts with the given
     * version, while the latest version of the document is 1.3 and the user started editing version 1.1.
     */
    private void mockConflictDecision(String forceSave, String conflictVersion)
    {
        when(mockClonedDocument.getDocumentReferenceWithLocale()).thenReturn(DOCUMENT_REFERENCE);
        when(mockClonedDocument.getComment()).thenReturn("My Changes");
        when(mockClonedDocument.getRCSVersion()).thenReturn(new Version("1.4"));
        when(mockForm.getTemplate()).thenReturn("");
        context.put("ajax", true);
        when(this.propertiesConf.getProperty("edit.conflictChecking.enabled", true)).thenReturn(true);
        when(mockRequest.getParameter("previousVersion")).thenReturn("1.1");
        when(mockRequest.getParameter("editingVersionDate")).thenReturn("1000");
        when(mockRequest.getParameter("forceSave")).thenReturn(forceSave);
        when(mockRequest.getParameter("conflictVersion")).thenReturn(conflictVersion);
        when(mockDocument.getVersion()).thenReturn("1.3");
        when(mockDocument.getRCSVersion()).thenReturn(new Version("1.3"));
        when(mockDocument.getDate()).thenReturn(new Date(2000));
    }

    private XWikiDocument mockRevision(String version) throws Exception
    {
        XWikiDocument revision = mock();
        when(this.documentRevisionProvider.getRevision(mockDocument, version)).thenReturn(revision);
        return revision;
    }

    private StringWriter mockResponseContent() throws Exception
    {
        when(this.xWiki.getEncoding()).thenReturn("UTF-8");
        StringWriter responseContent = new StringWriter();
        when(this.mockResponse.getWriter()).thenReturn(new PrintWriter(responseContent));
        return responseContent;
    }
}
