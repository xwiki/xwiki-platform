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

import javax.inject.Named;
import javax.inject.Provider;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.job.api.AbstractCheckRightsRequest;
import org.xwiki.job.event.status.JobProgressManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceProvider;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.query.Query;
import org.xwiki.query.QueryManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDeletedDocument;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.parentchild.ParentChildConfiguration;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.store.XWikiRecycleBinStoreInterface;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link DefaultModelBridge}.
 *
 * @version $Id$
 */
public class DefaultModelBridgeTest
{
    @Rule
    public MockitoComponentMockingRule<ModelBridge> mocker =
        new MockitoComponentMockingRule<>(DefaultModelBridge.class);

    private XWikiContext xcontext = mock(XWikiContext.class);

    private XWiki xwiki = mock(XWiki.class);

    private EntityReferenceResolver<String> relativeStringEntityReferenceResolver;

    private EntityReferenceSerializer<String> compactEntityReferenceSerializer;

    private AbstractCheckRightsRequest request = mock(AbstractCheckRightsRequest.class);;

    @Before
    public void configure() throws Exception
    {
        when(this.xcontext.getWiki()).thenReturn(xwiki);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(this.xcontext);

        this.relativeStringEntityReferenceResolver = this.mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "relative");
        this.compactEntityReferenceSerializer = this.mocker.getInstance(EntityReferenceResolver.TYPE_STRING, "compact");

        EntityReferenceProvider entityReferenceProvider = this.mocker.getInstance(EntityReferenceProvider.class);
        when(entityReferenceProvider.getDefaultReference(EntityType.DOCUMENT))
            .thenReturn(new DocumentReference("what", "ever", "WebHome"));
        when(entityReferenceProvider.getDefaultReference(EntityType.SPACE))
            .thenReturn(new SpaceReference("whatever", "Main"));
    }

    @Test
    public void create() throws Exception
    {
        XWikiDocument document = mock(XWikiDocument.class);
        DocumentReference documentReference = new DocumentReference("wiki", "Space", "Page");
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);

        this.mocker.getComponentUnderTest().create(documentReference);

        verify(this.xcontext.getWiki()).saveDocument(document, this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been created.", documentReference);
    }

    @Test
    public void copy() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        DocumentReference copyReference = new DocumentReference("wiki", "Space", "Copy");

        when(this.xcontext.getWiki().copyDocument(sourceReference, copyReference, "fr", false, true, true,
            this.xcontext)).thenReturn(true);

        assertTrue(this.mocker.getComponentUnderTest().copy(sourceReference, copyReference));

        verify(this.mocker.getMockedLogger()).info("Document [{}] has been copied to [{}].", sourceReference,
            copyReference);
    }

    @Test
    public void deleteTranslation() throws Exception
    {
        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page", Locale.FRENCH);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(1);

        this.mocker.getComponentUnderTest().delete(sourceReference);

        verify(this.xcontext.getWiki()).deleteDocument(sourceDocument, this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been deleted.", sourceReference);
    }

    @Test
    public void deleteAllTranslations() throws Exception
    {
        DocumentReference sourceReference = new DocumentReference("wiki", "Space", "Page");

        XWikiDocument sourceDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(sourceReference, this.xcontext)).thenReturn(sourceDocument);
        when(sourceDocument.getTranslation()).thenReturn(0);

        this.mocker.getComponentUnderTest().delete(sourceReference);

        verify(this.xcontext.getWiki()).deleteAllDocuments(sourceDocument, this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been deleted with all its translations.",
            sourceReference);
    }

    @Test
    public void createRedirect() throws Exception
    {
        DocumentReference oldReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newReference = new DocumentReference("wiki", "Space", "New");

        DocumentReference redirectClassReference = new DocumentReference("wiki", "XWiki", "RedirectClass");
        when(this.xcontext.getWiki().exists(redirectClassReference, this.xcontext)).thenReturn(true);

        XWikiDocument oldDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldReference, this.xcontext)).thenReturn(oldDocument);
        when(oldDocument.getXObject(eq(redirectClassReference), anyInt())).thenReturn(mock(BaseObject.class));

        this.mocker.getComponentUnderTest().createRedirect(oldReference, newReference);

        verify(oldDocument).setHidden(true);
        verify(this.xcontext.getWiki()).saveDocument(oldDocument, "Create automatic redirect.", this.xcontext);
        verify(this.mocker.getMockedLogger()).info("Created automatic redirect from [{}] to [{}].", oldReference,
            newReference);
    }

    @Test
    public void getDocumentReferences() throws Exception
    {
        SpaceReference spaceReference = new SpaceReference("wiki", "Space");

        Query query = mock(Query.class);
        QueryManager queryManager = this.mocker.getInstance(QueryManager.class);
        when(queryManager.createQuery(any(), any())).thenReturn(query);

        EntityReferenceSerializer<String> localEntityReferenceSerializer =
            this.mocker.getInstance(EntityReferenceSerializer.TYPE_STRING, "local");
        when(localEntityReferenceSerializer.serialize(spaceReference)).thenReturn("Space");

        when(query.execute()).thenReturn(Arrays.<Object>asList("Page"));

        DocumentReferenceResolver<String> explicitDocumentReferenceResolver =
            this.mocker.getInstance(DocumentReferenceResolver.TYPE_STRING, "explicit");
        DocumentReference documentReference = new DocumentReference("Page", spaceReference);
        when(explicitDocumentReferenceResolver.resolve("Page", spaceReference)).thenReturn(documentReference);

        assertEquals(Arrays.asList(documentReference),
            this.mocker.getComponentUnderTest().getDocumentReferences(spaceReference));

        verify(query).setWiki(spaceReference.getWikiReference().getName());
        verify(query).bindValue("space", "Space");
        verify(query).bindValue("spacePrefix", "Space.%");
    }

    @Test
    public void updateParentFields() throws Exception
    {
        DocumentReference oldParentReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newParentReference = new DocumentReference("wiki", "Space", "New");

        XWikiDocument oldParentDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldParentReference, this.xcontext)).thenReturn(oldParentDocument);

        DocumentReference child1Reference = new DocumentReference("wiki", "Space", "Child1");
        DocumentReference child2Reference = new DocumentReference("wiki", "Space", "Child2");
        when(oldParentDocument.getChildrenReferences(this.xcontext))
            .thenReturn(Arrays.asList(child1Reference, child2Reference));

        JobProgressManager mockProgressManager = mocker.getInstance(JobProgressManager.class);

        XWikiDocument child1Document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(child1Reference, this.xcontext)).thenReturn(child1Document);
        XWikiDocument child2Document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(child2Reference, this.xcontext)).thenReturn(child2Document);

        this.mocker.getComponentUnderTest().updateParentField(oldParentReference, newParentReference);

        verify(mockProgressManager).pushLevelProgress(2, this.mocker.getComponentUnderTest());

        verify(child1Document).setParentReference(newParentReference);
        verify(this.xcontext.getWiki()).saveDocument(child1Document, "Updated parent field.", true, this.xcontext);

        verify(child2Document).setParentReference(newParentReference);
        verify(this.xcontext.getWiki()).saveDocument(child1Document, "Updated parent field.", true, this.xcontext);
    }

    @Test
    public void updateParentFieldsNoChildren() throws Exception
    {
        DocumentReference oldParentReference = new DocumentReference("wiki", "Space", "Old");
        DocumentReference newParentReference = new DocumentReference("wiki", "Space", "New");

        XWikiDocument oldParentDocument = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(oldParentReference, this.xcontext)).thenReturn(oldParentDocument);

        when(oldParentDocument.getChildrenReferences(this.xcontext))
            .thenReturn(Collections.<DocumentReference>emptyList());

        JobProgressManager mockProgressManager = mocker.getInstance(JobProgressManager.class);

        this.mocker.getComponentUnderTest().updateParentField(oldParentReference, newParentReference);

        verify(mockProgressManager, never()).pushLevelProgress(anyInt(), any());
        verify(this.xcontext.getWiki(), never()).saveDocument(any(XWikiDocument.class), eq("Updated parent field."),
            eq(true), eq(this.xcontext));
    }

    @Test
    public void updateTitle() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        String serializedParent = "xwiki:Path.To.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference)).thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent);

        this.mocker.getComponentUnderTest().update(documentReference, Collections.singletonMap("title", "foo"));

        verify(document).setTitle("foo");
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been updated.", documentReference);
    }

    @Test
    public void updateParentWhenPageIsTerminal() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));
        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        String serializedParent = "xwiki:Path.To.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference)).thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.mocker.getComponentUnderTest().update(documentReference, Collections.emptyMap());

        verify(document).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been updated.", documentReference);
    }

    @Test
    public void dontUpdateParentDifferentWikiSameSpace() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        DocumentReference parentReference = new DocumentReference("subwiki", Arrays.asList("Path", "To"), "WebHome");
        when(document.getParentReference()).thenReturn(parentReference);
        when(document.getRelativeParentReference()).thenReturn(parentReference.getLocalDocumentReference());

        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        String serializedParent = "wiki:Path.To.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference)).thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.mocker.getComponentUnderTest().update(documentReference, Collections.emptyMap());

        // no need to update the parent: different wiki but same relative reference
        verify(document, never()).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki(), never()).saveDocument(document, "Update document after refactoring.", true, xcontext);
    }

    @Test
    public void dontUpdateParentInCaseOfPageRename() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "Foo"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);

        DocumentReference parentReference = new DocumentReference("wiki", Arrays.asList("Path"), "WebHome");
        when(document.getParentReference()).thenReturn(parentReference);
        when(document.getRelativeParentReference()).thenReturn(parentReference.getLocalDocumentReference());

        DocumentReference hierarchicalParent = new DocumentReference("wiki", Arrays.asList("Path"), "WebHome");
        String serializedParent = "wiki:Path.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference)).thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.mocker.getComponentUnderTest().update(documentReference, Collections.emptyMap());

        // no need to update the parent: different name but same parents
        verify(document, never()).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki(), never()).saveDocument(document, "Update document after refactoring.", true, xcontext);
    }

    @Test
    public void updateParentWhenPageIsNested() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));
        DocumentReference hierarchicalParent = new DocumentReference("wiki", "Path", "WebHome");
        String serializedParent = "xwiki:Path.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference)).thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.mocker.getComponentUnderTest().update(documentReference, Collections.emptyMap());

        verify(document).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been updated.", documentReference);
    }

    @Test
    public void updateParentWhenPageIsTopLevel() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", "Path", "WebHome");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));

        DocumentReference hierarchicalParent = new DocumentReference("wiki", "Main", "WebHome");
        String serializedParent = "xwiki:Main.WebHome";
        when(this.compactEntityReferenceSerializer.serialize(hierarchicalParent, documentReference)).thenReturn(serializedParent);
        when(this.relativeStringEntityReferenceResolver.resolve(serializedParent, EntityType.DOCUMENT))
            .thenReturn(hierarchicalParent.getLocalDocumentReference());

        this.mocker.getComponentUnderTest().update(documentReference, Collections.emptyMap());

        verify(document).setParentReference(hierarchicalParent.getLocalDocumentReference());
        verify(this.xcontext.getWiki()).saveDocument(document, "Update document after refactoring.", true, xcontext);
        verify(this.mocker.getMockedLogger()).info("Document [{}] has been updated.", documentReference);
    }

    @Test
    public void dontUpdateParentWhenLegacyMode() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("wiki", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, xcontext)).thenReturn(document);
        when(document.getParentReference()).thenReturn(new DocumentReference("wiki", "What", "Ever"));

        ParentChildConfiguration parentChildConfiguration = mocker.getInstance(ParentChildConfiguration.class);
        when(parentChildConfiguration.isParentChildMechanismEnabled()).thenReturn(true);

        this.mocker.getComponentUnderTest().update(documentReference, Collections.emptyMap());

        verify(document, never()).setParentReference(any(DocumentReference.class));
        verify(this.xcontext.getWiki(), never()).saveDocument(any(XWikiDocument.class), anyString(), anyBoolean(),
            any(XWikiContext.class));
    }

    @Test
    public void getBackLinkedReferences() throws Exception
    {
        DocumentReference documentReference = new DocumentReference("alice", Arrays.asList("Path", "To"), "Page");
        XWikiDocument document = mock(XWikiDocument.class);
        when(this.xcontext.getWiki().getDocument(documentReference, this.xcontext)).thenReturn(document);

        List<DocumentReference> backLinks = Arrays.asList(new DocumentReference("bob", "One", "Two"));
        when(document.getBackLinkedReferences(this.xcontext)).thenReturn(backLinks);

        this.xcontext.setWikiId("carol");

        assertEquals(backLinks, this.mocker.getComponentUnderTest().getBackLinkedReferences(documentReference, "bob"));

        verify(this.xcontext).setWikiId("bob");
        verify(this.xcontext).setWikiId("carol");
    }

    @Test
    public void restoreDeletedDocument() throws Exception
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

        assertTrue(mocker.getComponentUnderTest().restoreDeletedDocument(deletedDocumentId, request));

        verify(xwiki).restoreFromRecycleBin(deletedDocumentId, "Restored from recycle bin", xcontext);
    }

    @Test
    public void restoreDeletedDocumentInvalidId() throws Exception
    {
        long deletedDocumentId = 42;

        when(xwiki.getDeletedDocument(deletedDocumentId, xcontext)).thenReturn(null);
        when(request.isCheckAuthorRights()).thenReturn(false);
        when(request.isCheckRights()).thenReturn(false);

        assertFalse(mocker.getComponentUnderTest().restoreDeletedDocument(deletedDocumentId, request));

        verify(mocker.getMockedLogger()).error("Deleted document with ID [{}] does not exist.", deletedDocumentId);

        verify(xwiki, never()).restoreFromRecycleBin(any(), any(), any());
    }

    @Test
    public void restoreDeletedDocumentAlreadyExists() throws Exception
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

        assertFalse(mocker.getComponentUnderTest().restoreDeletedDocument(deletedDocumentId, request));

        verify(mocker.getMockedLogger()).error(
            "Document [{}] with ID [{}] can not be restored. Document already exists", fullName, deletedDocumentId);

        verify(xwiki, never()).restoreFromRecycleBin(any(), any(), any());
    }

    /**
     * @see "XWIKI-9567: Cannot restore document translations from recycle bin"
     */
    @Test
    public void restoreDocumentTranslation() throws Exception
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

        assertTrue(mocker.getComponentUnderTest().restoreDeletedDocument(deletedDocumentId, request));

        verify(xwiki).restoreFromRecycleBin(deletedDocumentId, "Restored from recycle bin", xcontext);

        // Make sure that the main document is not checked for existence, but the translated document which we actually
        // want to restore.
        verify(xwiki, never()).exists(documentReference, xcontext);
        verify(xwiki).exists(translationDocumentReference, xcontext);
    }

    @Test
    public void canRestoreDeletedDocument() throws Exception
    {
        long deletedDocumentId = 42;
        String deletedDocumentFullName = "Space.DeletedDocument";

        DocumentReference userReferenceToCheck = new DocumentReference("wiki", "Space", "User");

        DocumentReference currentUserReference = new DocumentReference("wiki", "Space", "CurrentUser");

        when(xcontext.getUserReference()).thenReturn(currentUserReference);

        XWikiDeletedDocument deletedDocument = mock(XWikiDeletedDocument.class);
        when(deletedDocument.getFullName()).thenReturn(deletedDocumentFullName);

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(recycleBin.getDeletedDocument(deletedDocumentId, xcontext, true)).thenReturn(deletedDocument);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        XWikiRightService rightService = mock(XWikiRightService.class);
        when(xwiki.getRightService()).thenReturn(rightService);
        when(rightService.hasAccessLevel(any(), any(), any(), any())).thenReturn(true);

        assertTrue(((DefaultModelBridge)mocker.getComponentUnderTest()).canRestoreDeletedDocument(deletedDocument, userReferenceToCheck));

        // Verify that the rights were checked with the specified user as context user.
        verify(xcontext).setUserReference(userReferenceToCheck);
        // Verify that the context user was restored. Note: We don`t know the order here, but maybe we don`t care that
        // much.
        verify(xcontext).setUserReference(currentUserReference);
    }

    @Test
    public void restoreDeletedDocumentNoRights() throws Exception
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

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(recycleBin.getDeletedDocument(deletedDocumentId, xcontext, true)).thenReturn(deletedDocument);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        // No rights.
        XWikiRightService rightService = mock(XWikiRightService.class);
        when(xwiki.getRightService()).thenReturn(rightService);
        when(rightService.hasAccessLevel(any(), any(), any(), any())).thenReturn(false);
        DocumentReference authorReference = new DocumentReference("wiki", "user", "Alice");
        when(xcontext.getAuthorReference()).thenReturn(authorReference);
        when(request.isCheckAuthorRights()).thenReturn(true);
        when(request.isCheckRights()).thenReturn(true);

        assertFalse(mocker.getComponentUnderTest().restoreDeletedDocument(deletedDocumentId, request));

        verify(mocker.getMockedLogger()).error(
            "The author [{}] of this script is not allowed to restore document [{}] with ID [{}]",
            authorReference, documentReference, deletedDocumentId);

        verify(xwiki, never()).restoreFromRecycleBin(any(), any(), any());
    }

    @Test
    public void getDeletedDocumentIds() throws Exception
    {
        String batchId = "abc123";
        long id1 = 1;
        long id2 = 2;
        XWikiDeletedDocument deletedDocument1 = mock(XWikiDeletedDocument.class);
        when(deletedDocument1.getId()).thenReturn(id1);

        XWikiDeletedDocument deletedDocument2 = mock(XWikiDeletedDocument.class);
        when(deletedDocument2.getId()).thenReturn(id2);

        XWikiDeletedDocument[] deletedDocuments = {deletedDocument1, deletedDocument2};

        XWikiRecycleBinStoreInterface recycleBin = mock(XWikiRecycleBinStoreInterface.class);
        when(recycleBin.getAllDeletedDocuments(batchId, false, xcontext, true)).thenReturn(deletedDocuments);
        when(xwiki.getRecycleBinStore()).thenReturn(recycleBin);

        List<Long> result = mocker.getComponentUnderTest().getDeletedDocumentIds(batchId);

        assertNotNull(result);
        assertEquals(deletedDocuments.length, result.size());
        assertThat(result, containsInAnyOrder(id1, id2));
    }
}
