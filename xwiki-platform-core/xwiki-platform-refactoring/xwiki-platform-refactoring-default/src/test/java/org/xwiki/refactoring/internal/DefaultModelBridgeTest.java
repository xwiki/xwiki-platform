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
package org.xwiki.refactoring.internal;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.job.AbstractJobStatus;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.link.LinkStore;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.refactoring.internal.job.PermanentlyDeleteJob;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.parentchild.ParentChildConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.store.XWikiStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;
import com.xpn.xwiki.web.Utils;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultModelBridge}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultModelBridgeTest
{
    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension();

    @InjectMockComponents
    private DefaultModelBridge modelBridge;

    @MockComponent
    @Named("relative")
    private EntityReferenceResolver<String> relativeStringEntityReferenceResolver;

    @MockComponent
    @Named("compact")
    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    @MockComponent
    private JobProgressManager progressManager;

    @MockComponent
    private AuthorizationManager authorization;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private Provider<LinkStore> linkStoreProvider;

    @MockComponent
    private DocumentReferenceResolver<EntityReference> documentReferenceResolver;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    @Mock
    private XWikiContext xcontext;

    @Mock
    private XWiki xwiki;

    @Mock
    private XWikiRecycleBinStoreInterface recycleBin;

    @Mock
    private XWikiStoreInterface store;

    @Mock
    private AbstractCheckRightsRequest request;

    @Mock
    private XWikiRightService xWikiRightService;

    @Mock
    private LinkStore linkStore;

    @BeforeEach
    void configure(MockitoComponentManager componentManager) throws Exception
    {
        when(this.xcontext.getWiki()).thenReturn(this.xwiki);
        when(xwiki.getRecycleBinStore()).thenReturn(this.recycleBin);
        when(xwiki.getStore()).thenReturn(this.store);
        when(xwiki.getRightService()).thenReturn(this.xWikiRightService);

        Provider<XWikiContext> xcontextProvider = componentManager.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        Utils.setComponentManager(this.componentManager);

        EntityReferenceProvider entityReferenceProvider = componentManager.getInstance(EntityReferenceProvider.class);
        when(entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("what", "ever", "WebHome"));
        when(entityReferenceProvider.getDefaultReference(EntityType.SPACE))
            .thenReturn(new SpaceReference("whatever", "Main"));
        when(this.linkStoreProvider.get()).thenReturn(this.linkStore);
    }

    private void assertLog(Level level, String message, Object... arguments)
    {
        ILoggingEvent log = this.logCapture.getLogEvent(0);
        assertEquals(message, log.getMessage());
        assertEquals(level, log.getLevel());
        assertArrayEquals(arguments, log.getArgumentArray());
    }

    private void assertLog(int i, Level level, String message, Object... arguments)
    {
        ILoggingEvent log = this.logCapture.getLogEvent(i);
        assertEquals(level, log.getLevel());
        assertEquals(message, log.getMessage());
        assertArrayEquals(arguments, log.getArgumentArray());
    }

    @Test
    void create() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);

        this.modelBridge.create(documentReference);

        verify(this.xcontext.getWiki()).saveDocument(document, this.xcontext);
        assertLog(Level.INFO, "Document [{}] has been created.", documentReference);
    }

    @Test
    void createWithException() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenThrow(new XWikiException());

        this.modelBridge.create(documentReference);

        assertLog(Level.ERROR, "Failed to create document [{}].", documentReference);
    }

    @Test
    void copy() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        DocumentReference copyReference = new DocumentReference("wiki", "Space", "Copy");

        when(this.xcontext.getWiki().copyDocument(sourceReference, copyReference, "fr", false, true, true,
            this.xcontext)).thenReturn(true);

        assertTrue(this.modelBridge.copy(sourceReference, copyReference));
        assertLog(Level.INFO, "Document [{}] has been copied to [{}].", sourceReference, copyReference);
    }

    @Test
    void deleteTranslation() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(1);

        this.modelBridge.delete(sourceReference);

        verify(this.xcontext.getWiki()).deleteDocument(sourceDocument, true, this.xcontext);
        assertLog(Level.INFO, "Document [{}] has been deleted (to the recycle bin: [{}]).", sourceReference, true);
    }

    @Test
    void deleteAndSkipTheRecycleBin() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(1);

        this.modelBridge.delete(sourceReference, true);

        verify(this.xcontext.getWiki()).deleteDocument(sourceDocument, false, this.xcontext);
        assertLog(Level.INFO, "Document [{}] has been deleted (to the recycle bin: [{}]).", sourceReference, false);
    }

    @Test
    void deleteAndThrowException() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenThrow(new XWikiException());
        when(sourceDocument.getTranslation()).thenReturn(1);

        boolean actual = this.modelBridge.delete(sourceReference, false);

        assertFalse(actual);
        assertLog(Level.ERROR, "Failed to delete document [{}] (to the recycle bin: [{}]).", sourceReference, true);
    }

    @Test
    void deleteAllTranslations() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");

        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(0);

        this.modelBridge.delete(sourceReference);

        verify(this.xcontext.getWiki()).deleteAllDocuments(sourceDocument, true, this.xcontext);
        assertLog(Level.INFO, "Document [{}] has been deleted with all its translations (to the recycle bin: [{}]).",
            sourceReference, true);
    }

    @Test
    void createRedirect() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newReference = new DocumentReference("wiki", "Space", "New");

        DocumentReference redirectClassReference = new DocumentReference("wiki", "XWiki", "RedirectClass");
        when(this.xcontext.getWiki().exists(redirectClassReference, this.xcontext)).thenReturn(true);

        XWikiDocument oldDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldReference, this.xcontext)).thenReturn(oldDocument);
        when(oldDocument.getXObject(eq(redirectClassReference), anyInt())).thenReturn(mock(BaseObject.class));

        this.modelBridge.createRedirect(oldReference, newReference);

        verify(oldDocument).setHidden(true);
        verify(this.xcontext.getWiki()).saveDocument(oldDocument, "Create automatic redirect.", this.xcontext);
        assertLog(Level.INFO, "Created automatic redirect from [{}] to [{}].", oldReference, newReference);
    }

    @Test
    void getDocumentReferences(MockitoComponentManager componentManager) throws Exception
    {
        SpaceReference spaceReference = new SpaceReference("wiki", "Space");

        Query query = mock(Query.class);
        QueryManager queryManager = componentManager.getInstance(QueryManager.class);
        when(queryManager.createQuery(any(), any())).thenReturn(query);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            componentManager.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(spaceReference)).thenReturn("Space");

        when(query.execute()).thenReturn(Arrays.<Object>asList("Page"));

        DocumentReferenceResolver<String> explicitDocumentReferenceResolver =
            componentManager.getInstance(DocumentReferenceResolver.TYPE_STRING, "explicit");
        DocumentReference documentReference = new DocumentReference("Page", spaceReference);
        when(explicitDocumentReferenceResolver.resolve("Page", spaceReference)).thenReturn(documentReference);

        assertEquals(Arrays.asList(documentReference), this.modelBridge.getDocumentReferences(spaceReference));

        verify(query).setWiki(spaceReference.getWikiReference().getName());
        verify(query).bindValue("space", "Space");
        verify(query).bindValue("spacePrefix", "Space.%");
    }

    @Test
    void updateParentFields(MockitoComponentManager componentManager) throws Exception
    {
        DocumentReference oldParentReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newParentReference = new DocumentReference("wiki", "Space", "New");

        XWikiDocument oldParentDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldParentReference, this.xcontext)).thenReturn(oldParentDocument);

        DocumentReference child1Reference = new DocumentReference("wiki", "Space", "Child1");
        DocumentReference child2Reference = new DocumentReference("wiki", "Space", "Child2");
        when(oldParentDocument.getChildrenReferences(this.xcontext))
            .thenReturn(Arrays.asList(child1Reference, child2Reference));

        JobProgressManager mockProgressManager = componentManager.getInstance(JobProgressManager.class);

        XWikiDocument child1Document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(child1Reference, this.xcontext)).thenReturn(child1Document);
        XWikiDocument child2Document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(child2Reference, this.xcontext)).thenReturn(child2Document);

        this.modelBridge.updateParentField(oldParentReference, newParentReference);

        verify(mockProgressManager).pushLevelProgress(2, this.modelBridge);

        verify(child1Document).setParentReference(newParentReference);
        verify(this.xcontext.getWiki()).saveDocument(child1Document, "Updated parent field.", true, this.xcontext);

        verify(child2Document).setParentReference(newParentReference);
        verify(this.xcontext.getWiki()).saveDocument(child1Document, "Updated parent field.", true, this.xcontext);

        assertLog(Level.INFO, "Document parent fields updated from [{}] to [{}] for [{}] documents.",
            oldParentReference, newParentReference, 2);
    }

    @Test
    void updateParentFieldsNoChildren(MockitoComponentManager componentManager) throws Exception
    {
        DocumentReference oldParentReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newParentReference = new DocumentReference("wiki", "Space", "New");

        XWikiDocument oldParentDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldParentReference, this.xcontext)).thenReturn(oldParentDocument);

        when(oldParentDocument.getChildrenReferences(this.xcontext))
            .thenReturn(Collections.<DocumentReference>emptyList());

        JobProgressManager mockProgressManager = componentManager.getInstance(JobProgressManager.class);

        this.modelBridge.updateParentField(oldParentReference, newParentReference);

        verify(mockProgressManager, never()).pushLevelProgress(anyInt(), any());
        verify(this.xcontext.getWiki(), never()).saveDocument(any(XWikiDocument.class), eq("Updated parent field."),
            eq(true), eq(this.xcontext));
    }

    @Test
    void updateTitle() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        String serializedParent = "xwiki:Path.To.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference))
            .thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent);

        this.modelBridge.update(documentReference, Collections.singletonMap("title", "foo"));

        verify(document).setTitle("foo");
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        assertLog(Level.INFO, "Document [{}] has been updated.", documentReference);
    }

    @Test
    void updateParentWhenPageIsTerminal() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));
        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        String serializedParent = "xwiki:Path.To.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference))
            .thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.modelBridge.update(documentReference, Collections.emptyMap());

        verify(document).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        assertLog(Level.INFO, "Document [{}] has been updated.", documentReference);
    }

    @Test
    void dontUpdateParentDifferentWikiSameSpace() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        DocumentReference parentReference = new DocumentReference("subwiki", Arrays.asList("Path", "To"), "WebHome");
        when(document.getParentReference()).thenReturn(parentReference);
        when(document.getRelativeParentReference()).thenReturn(parentReference.getLocalDocumentReference());

        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        String serializedParent = "wiki:Path.To.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference))
            .thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.modelBridge.update(documentReference, Collections.emptyMap());

        // no need to update the parent: different wiki but same relative reference
        verify(document, never()).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki(), never()).saveDocument(document, "Update document after refactoring.", true,
            xcontext);
    }

    @Test
    void dontUpdateParentInCaseOfPageRename() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "Foo"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        DocumentReference parentReference = new DocumentReference("wiki", Arrays.asList("Path"), "WebHome");
        when(document.getParentReference()).thenReturn(parentReference);
        when(document.getRelativeParentReference()).thenReturn(parentReference.getLocalDocumentReference());

        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path"), "WebHome");
        String serializedParent = "wiki:Path.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference))
            .thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.modelBridge.update(documentReference, Collections.emptyMap());

        // no need to update the parent: different name but same parents
        verify(document, never()).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki(), never()).saveDocument(document, "Update document after refactoring.", true,
            xcontext);
    }

    @Test
    void updateParentWhenPageIsNested() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));
        DocumentReference hierarchicalParent = new DocumentReference("wiki", "Path", "WebHome");
        String serializedParent = "xwiki:Path.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference))
            .thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.modelBridge.update(documentReference, Collections.emptyMap());

        verify(document).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        assertLog(Level.INFO, "Document [{}] has been updated.", documentReference);
    }

    @Test
    void updateParentWhenPageIsTopLevel() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Path", "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));

        DocumentReference hierarchicalParent = new DocumentReference("wiki", "Main", "WebHome");
        String serializedParent = "xwiki:Main.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference))
            .thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.modelBridge.update(documentReference, Collections.emptyMap());

        verify(document).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        assertLog(Level.INFO, "Document [{}] has been updated.", documentReference);
    }

    @Test
    void dontUpdateParentWhenLegacyMode(MockitoComponentManager componentManager) throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));

        ParentChildConfiguration parentChildConfiguration =
            componentManager.getInstance(ParentChildConfiguration.class);
        when(parentChildConfiguration.isParentChildMechanismEnabled()).thenReturn(true);

        this.modelBridge.update(documentReference, Collections.emptyMap());

        verify(document, never()).setParentReference(any(DocumentReference.class));
        verify(this.xcontext.getWiki(), never()).saveDocument(any(XWikiDocument.class), anyString(), anyBoolean(),
            any(XWikiContext.class));
    }

    @Test
    void restoreDeletedDocument() throws Exception
    {
        long deletedDocumentId = 42;
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(false);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertTrue(this.modelBridge.restoreDeletedDocument(deletedDocumentId, request));

        verify(xwiki).restoreFromRecycleBin(deletedDocumentId, "Restored from recycle bin", xcontext);
        assertLog(Level.INFO, "Document [{}] has been restored", documentReference);
    }

    @Test
    void permanentlyDeleteDeletedDocument() throws Exception
    {
        long deletedDocumentId = 42;
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(false);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertTrue(this.modelBridge.permanentlyDeleteDocument(deletedDocumentId, request));

        verify(recycleBin).deleteFromRecycleBin(deletedDocumentId, xcontext, true);
        assertLog(Level.INFO, "Document [{}] has been permanently deleted.", documentReference);
    }

    @Test
    void permanentlyDeleteDeletedDocumentWrongRights() throws Exception
    {
        long deletedDocumentId = 42;
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        String documentFullName = "fullName";
        when(deletedDocument.getFullName()).thenReturn("");
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(false);
        when(request.isCheckAuthorRights()).thenReturn(true);
        when(request.isCheckRights()).thenReturn(false);
        String username = "myUser";
        when(this.xcontext.getUser()).thenReturn(username);
        when(this.xWikiRightService.hasAccessLevel("delete", username, documentFullName, this.xcontext))
            .thenReturn(false);

        assertFalse(this.modelBridge.permanentlyDeleteDocument(deletedDocumentId, request));

        verify(recycleBin, never()).deleteFromRecycleBin(deletedDocumentId, xcontext, true);

        assertLog(1, Level.ERROR, "The author [{}] of this script is not allowed to permanently deleted document [{}] "
            + "with id [{}]", null, documentReference, deletedDocumentId);

        // this could be improved later: right now we don't get the rights in the test because the components are not
        // all properly loaded from oldcore.
        assertLog(0, Level.WARN, "Exception while checking if entry [{}] can be removed from the recycle bin",
            deletedDocumentId);
    }

    @Test
    void restoreDeletedDocumentInvalidId() throws Exception
    {
        long deletedDocumentId = 42;

        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(null);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertFalse(this.modelBridge.restoreDeletedDocument(deletedDocumentId, request));

        assertLog(Level.ERROR, "Deleted document with ID [{}] does not exist.", deletedDocumentId);

        verify(xwiki, never()).restoreFromRecycleBin(any(), any(), any());
    }

    @Test
    void permanentlyDeleteDeletedDocumentInvalidId() throws Exception
    {
        long deletedDocumentId = 42;

        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(null);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertFalse(this.modelBridge.permanentlyDeleteDocument(deletedDocumentId, request));

        assertLog(Level.ERROR, "Deleted document with ID [{}] does not exist.", deletedDocumentId);

        verify(recycleBin, never()).deleteFromRecycleBin(anyLong(), any(), anyBoolean());
    }

    @Test
    void restoreDeletedDocumentAlreadyExists() throws Exception
    {
        long deletedDocumentId = 42;
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        String fullName = "space.page";

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(deletedDocument.getFullName()).thenReturn(fullName);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(true);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertFalse(this.modelBridge.restoreDeletedDocument(deletedDocumentId, request));

        assertLog(Level.ERROR, "Document [{}] with ID [{}] can not be restored. Document already exists", fullName,
            deletedDocumentId);

        verify(xwiki, never()).restoreFromRecycleBin(any(), any(), any());
    }

    /**
     * @see "XWIKI-9567: Cannot restore document translations from recycle bin"
     */
    @Test
    void restoreDocumentTranslation() throws Exception
    {
        long deletedDocumentId = 42;
        Locale locale = new Locale("ro");
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentReference translationDocumentReference = new DocumentReference(documentReference, locale);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(translationDocumentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(translationDocumentReference, xcontext)).thenReturn(false);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertTrue(this.modelBridge.restoreDeletedDocument(deletedDocumentId, request));

        verify(xwiki).restoreFromRecycleBin(deletedDocumentId, "Restored from recycle bin", xcontext);
        assertLog(Level.INFO, "Document [{}] has been restored", translationDocumentReference);

        // Make sure that the main document is not checked for existence, but the translated document which we actually
        // want to restore.
        verify(xwiki, never()).exists(documentReference, xcontext);
        verify(xwiki).exists(translationDocumentReference, xcontext);
    }

    @Test
    void permanentlyDeleteDocumentTranslation() throws Exception
    {
        long deletedDocumentId = 42;
        Locale locale = new Locale("ro");
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        DocumentReference translationDocumentReference = new DocumentReference(documentReference, locale);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(translationDocumentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(translationDocumentReference, xcontext)).thenReturn(false);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertTrue(this.modelBridge.permanentlyDeleteDocument(deletedDocumentId, request));

        verify(recycleBin).deleteFromRecycleBin(deletedDocumentId, xcontext, true);
        assertLog(Level.INFO, "Document [{}] has been permanently deleted.", translationDocumentReference);
    }

    @Test
    void canRestoreDeletedDocument() throws Exception
    {
        long deletedDocumentId = 42;
        String deletedDocumentFullName = "Space.DeletedDocument";

        DocumentReference userReferenceToCheck = new DocumentReference("wiki", "Space", "User");

        DocumentReference currentUserReference = new DocumentReference("wiki", "Space", "CurrentUser");

        when(xcontext.getUserReference()).thenReturn(currentUserReference);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getFullName()).thenReturn(deletedDocumentFullName);

        XWikiRightService rightService = mock(XWikiRightService.class);
        when(xwiki.getRightService()).thenReturn(rightService);
        when(recycleBin.hasAccess(any(), any(), any())).thenReturn(true);

        assertTrue(this.modelBridge.canRestoreDeletedDocument(deletedDocument, userReferenceToCheck));

        // Verify that the rights were checked with the specified user as context user.
        verify(xcontext).setUserReference(userReferenceToCheck);
        // Verify that the context user was restored. Note: We don`t know the order here, but maybe we don`t care that
        // much.
        verify(xcontext).setUserReference(currentUserReference);
    }

    @Test
    void restoreDeletedDocumentNoRights() throws Exception
    {
        long deletedDocumentId = 42;
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        String deletedDocumentFullName = "space.page";

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(deletedDocument.getFullName()).thenReturn(deletedDocumentFullName);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(false);
        when(recycleBin.getDeletedDocument(deletedDocumentId, xcontext, true)).thenReturn(deletedDocument);

        // No rights.
        XWikiRightService rightService = mock(XWikiRightService.class);
        when(xwiki.getRightService()).thenReturn(rightService);
        when(recycleBin.hasAccess(any(), any(), any())).thenReturn(false);
        DocumentReference authorReference = new DocumentReference("wiki", "user", "Alice");
        when(xcontext.getAuthorReference()).thenReturn(authorReference);
        when(request.isCheckRights()).thenReturn(true);
        when(request.isCheckAuthorRights()).thenReturn(true);

        assertFalse(this.modelBridge.restoreDeletedDocument(deletedDocumentId, request));

        assertLog(Level.ERROR, "The author [{}] of this script is not allowed to restore document [{}] with ID [{}]",
            authorReference, documentReference, deletedDocumentId);
        verify(xwiki, never()).restoreFromRecycleBin(any(), any(), any());
    }

    @Test
    void permanentlyDeleteDeletedDocumentNoRights() throws Exception
    {
        long deletedDocumentId = 42;
        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        String deletedDocumentFullName = "space.page";

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(deletedDocument.getId()).thenReturn(deletedDocumentId);
        when(deletedDocument.getFullName()).thenReturn(deletedDocumentFullName);
        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(false);
        when(recycleBin.getDeletedDocument(deletedDocumentId, xcontext, true)).thenReturn(deletedDocument);

        // No rights.
        XWikiRightService rightService = mock(XWikiRightService.class);
        when(xwiki.getRightService()).thenReturn(rightService);
        when(rightService.hasAccessLevel(any(), any(), any(), any())).thenReturn(false);
        when(request.isCheckRights()).thenReturn(true);
        when(request.isCheckAuthorRights()).thenReturn(true);

        assertFalse(this.modelBridge.permanentlyDeleteDocument(deletedDocumentId, request));

        assertLog(0, Level.WARN, "Exception while checking if entry [{}] can be removed from the recycle bin",
            deletedDocumentId);
        assertLog(1, Level.ERROR, "You are not allowed to permanently delete document [{}] with ID [{}]",
            documentReference, deletedDocumentId);
        verify(recycleBin, never()).deleteFromRecycleBin(anyLong(), any(), anyBoolean());
    }

    @Test
    void getDeletedDocumentIds() throws Exception
    {
        String batchId = "abc123";
        long id1 = 1;
        long id2 = 2;
        XWikiDeletedDocument deletedDocument1 = mock(XWikiDeletedDocument.class);
        when(deletedDocument1.getId()).thenReturn(id1);

        XWikiDeletedDocument deletedDocument2 = mock(XWikiDeletedDocument.class);
        when(deletedDocument2.getId()).thenReturn(id2);

        XWikiDeletedDocument[] deletedDocuments = { deletedDocument1, deletedDocument2 };

        when(recycleBin.getAllDeletedDocuments(batchId, false, xcontext, true)).thenReturn(deletedDocuments);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        List<Long> result = this.modelBridge.getDeletedDocumentIds(batchId);

        assertNotNull(result);
        assertEquals(deletedDocuments.length, result.size());
        assertThat(result, containsInAnyOrder(id1, id2));
    }

    @Test
    void canOverwriteSilently() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xwiki.getDocument(documentReference, this.xcontext)).thenReturn(document);

        assertFalse(this.modelBridge.canOverwriteSilently(documentReference));

        DocumentReference redirectClassReference = new DocumentReference("wiki", "XWiki", "RedirectClass");
        when(document.getXObject(redirectClassReference)).thenReturn(mock(BaseObject.class));

        assertTrue(this.modelBridge.canOverwriteSilently(documentReference));
    }

    @Test
    void permanentlyDeleteAllDocuments() throws Exception
    {
        int nbDocs = 12;
        PermanentlyDeleteJob deleteJob = mock(PermanentlyDeleteJob.class);
        AbstractJobStatus jobStatus = mock(AbstractJobStatus.class);
        when(jobStatus.isCanceled()).thenReturn(false);
        when(deleteJob.getStatus()).thenReturn(jobStatus);

        DocumentReference documentReference = new DocumentReference("wiki", "space", "page");
        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getDocumentReference()).thenReturn(documentReference);
        when(xwiki.getDeletedDocument(anyLong(), eq(xcontext))).thenReturn(deletedDocument);

        when(xwiki.exists(documentReference, xcontext)).thenReturn(false);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        when(recycleBin.getNumberOfDeletedDocuments(any())).thenReturn((long) nbDocs);
        when(recycleBin.getAllDeletedDocumentsIds(eq(this.xcontext), anyInt()))
            .thenReturn(new Long[]{ 1L, 2L, 3L, 4L, 5L, 6L, 7L, 8L, 9L, 10L }).thenReturn(new Long[]{ 11L, 12L });
        when(request.isCheckRights()).thenReturn(false);
        when(request.isCheckAuthorRights()).thenReturn(false);

        assertTrue(this.modelBridge.permanentlyDeleteAllDocuments(deleteJob, request));
        verify(this.progressManager).pushLevelProgress(nbDocs, deleteJob);
        verify(this.progressManager, times(12)).startStep(deleteJob);
        verify(this.progressManager, times(12)).endStep(deleteJob);
        for (int i = 1; i <= 12; i++) {
            verify(recycleBin).deleteFromRecycleBin(i, xcontext, true);
            assertLog(i - 1, Level.INFO, "Document [{}] has been permanently deleted.", documentReference);
        }
    }
    
    @Test
    void rename() throws Exception
    {
        DocumentReference source = new DocumentReference("wiki", "space", "sourcePage");
        DocumentReference target = new DocumentReference("wiki", "space", "targetPage");

        when(this.xwiki.renameDocument(source, target, true, List.of(), List.of(), this.xcontext)).thenReturn(true);
        assertTrue(this.modelBridge.rename(source, target));

        verify(this.xwiki).renameDocument(source, target, true, List.of(), List.of(), this.xcontext);
    }

    @Test
    void getBackLinkedDocuments() throws Exception
    {
        EntityReference source = mock(EntityReference.class);
        EntityReference ref1 = mock(EntityReference.class, "ref1");
        EntityReference ref2 = mock(EntityReference.class, "ref2");
        EntityReference ref3 = mock(EntityReference.class, "ref3");
        when(this.linkStore.resolveBackLinkedEntities(source)).thenReturn(Set.of(ref1, ref2, ref3));

        DocumentReference docRef1 = mock(DocumentReference.class, "docRef1");
        DocumentReference docRef2 = mock(DocumentReference.class, "docRef2");
        DocumentReference docRef3 = mock(DocumentReference.class, "docRef3");
        when(this.documentReferenceResolver.resolve(ref1, this.xcontext)).thenReturn(docRef1);
        when(this.documentReferenceResolver.resolve(ref2, this.xcontext)).thenReturn(docRef2);
        when(this.documentReferenceResolver.resolve(ref3, this.xcontext)).thenReturn(docRef3);

        assertEquals(Set.of(docRef1, docRef2, docRef3), this.modelBridge.getBackLinkedDocuments(source));
    }
}
