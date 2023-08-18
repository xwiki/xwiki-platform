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
package com.xpn.xwiki.api;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.document.DocumentAuthors;
import org.xwiki.model.internal.document.SafeDocumentAuthors;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.observation.ObservationManager;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@OldcoreTest
@ReferenceComponentList
class DocumentTest
{
    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserReferenceUserReferenceResolver;

    @MockComponent
    private ObservationManager observationManager;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.INFO);

    @Test
    void toStringReturnsFullName()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));
        assertEquals("Space.Page", new Document(doc, new XWikiContext()).toString());
        assertEquals("Main.WebHome", new Document(new XWikiDocument(), new XWikiContext()).toString());
    }

    @Test
    void getObjects() throws XWikiException
    {
        XWikiContext context = new XWikiContext();
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));

        doc.getXClass().addNumberField("prop", "prop", 5, "long");
        BaseObject obj = (BaseObject) doc.getXClass().newObject(context);
        obj.setLongValue("prop", 1);
        doc.addObject(doc.getFullName(), obj);

        assertEquals(obj, doc.getObject(doc.getFullName(), "prop", "1"));
        assertNull(doc.getObject(doc.getFullName(), "prop", "2"));

        Document adoc = new Document(doc, context);
        List<Object> lst = adoc.getObjects(adoc.getFullName(), "prop", "1");
        assertEquals(1, lst.size());
        assertEquals(obj, lst.get(0).getBaseObject());

        lst = adoc.getObjects(adoc.getFullName(), "prop", "0");
        assertEquals(0, lst.size());

        lst = adoc.getObjects(adoc.getFullName());
        assertEquals(1, lst.size());
    }

    @Test
    void getObject()
    {
        XWikiContext context = new XWikiContext();
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));

        doc.getXClass().addNumberField("prop", "prop", 5, "long");

        Document apiDocument = new Document(doc, context);
        ObjectReference objectReference = new ObjectReference("Wiki.Space.Page[2]", doc.getDocumentReference());
        Object apiObject = apiDocument.getObject(objectReference, true);
        apiObject.set("prop", 20);

        assertEquals(apiObject, apiDocument.getObject("Wiki.Space.Page", 2));
        assertEquals(2, apiObject.getNumber());
    }

    @Test
    void removeObjectDoesntCauseDataLoss() throws XWikiException
    {
        // Setup comment class
        XWikiDocument commentDocument = new XWikiDocument(new DocumentReference("wiki", "XWiki", "XWikiComments"));
        commentDocument.getXClass().addTextAreaField("comment", "comment", 60, 20);
        this.oldcore.getSpyXWiki().saveDocument(commentDocument, this.oldcore.getXWikiContext());

        // Setup document
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));

        for (int i = 0; i < 10; ++i) {
            xdoc.newXObject(commentDocument.getDocumentReference(), this.oldcore.getXWikiContext());
        }

        Document adoc = xdoc.newDocument(this.oldcore.getXWikiContext());

        for (Object obj : adoc.getObjects("XWiki.XWikiComments")) {
            obj.set("comment", "Comment");
            if (obj.getNumber() == 4) {
                adoc.removeObject(obj);
            }
        }

        // Let's make sure the original document wasn't changed
        for (BaseObject obj : xdoc.getXObjects(commentDocument.getDocumentReference())) {
            assertNull(obj.get("comment"));
        }

        // Let's make sure the cloned document was changed everywhere
        for (BaseObject obj : adoc.getDoc().getXObjects(commentDocument.getDocumentReference())) {
            if (obj != null) {
                assertEquals("Comment", ((BaseProperty) obj.get("comment")).getValue());
            }
        }
    }

    @Test
    void saveAsAuthorUsesGuestIfDroppedPermissions() throws XWikiException
    {
        DocumentReference aliceReference = new DocumentReference("wiki", "XWiki", "Alice");
        DocumentReference bobReference = new DocumentReference("wiki", "XWiki", "Bob");

        XWikiDocument cdoc = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));
        XWikiDocument sdoc = new XWikiDocument(new DocumentReference("wiki", "Space", "AuthorPage"));

        when(this.oldcore.getMockAuthorizationManager().hasAccess(same(Right.EDIT), eq(aliceReference),
            eq(cdoc.getDocumentReference()))).thenReturn(true);
        when(this.oldcore.getMockAuthorizationManager().hasAccess(same(Right.EDIT), isNull(),
            eq(cdoc.getDocumentReference()))).thenReturn(false);

        this.oldcore.getXWikiContext().setDoc(cdoc);
        this.oldcore.getXWikiContext().put("sdoc", sdoc);

        // Alice is the author.
        sdoc.setAuthorReference(aliceReference);
        sdoc.setContentAuthorReference(sdoc.getAuthorReference());

        // Bob is the viewer
        this.oldcore.getXWikiContext().setUserReference(bobReference);

        Document doc = cdoc.newDocument(this.oldcore.getXWikiContext());

        doc.saveAsAuthor();

        this.oldcore.getXWikiContext().dropPermissions();

        Throwable exception = assertThrows(XWikiException.class, () -> doc.saveAsAuthor());
        assertTrue(
            exception.getMessage()
                .contains("Access denied; user null, acting through script in "
                    + "document Space.Page cannot save document Space.Page"),
            "Wrong error message when trying to save a document after calling dropPermissions()");

        assertEquals(bobReference, this.oldcore.getXWikiContext().getUserReference(),
            "After dropping permissions and attempting to save a document, "
                + "the user was permanently switched to guest.");
    }

    @Test
    void user()
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));
        Document document = xdoc.newDocument(this.oldcore.getXWikiContext());

        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, document.getCreator());
        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, document.getAuthor());
        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, document.getContentAuthor());
    }

    @Test
    void changeAuthorWhenModifyingDocumentContent()
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("wiki0", "Space", "Page"));
        xdoc.setAuthorReference(new DocumentReference("wiki1", "XWiki", "initialauthor"));
        xdoc.setContentAuthorReference(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"));
        xdoc.setCreatorReference(new DocumentReference("wiki1", "XWiki", "initialcreator"));

        this.oldcore.getXWikiContext().setUserReference(new DocumentReference("wiki2", "XWiki", "contextuser"));

        Document document = xdoc.newDocument(this.oldcore.getXWikiContext());

        assertEquals(new DocumentReference("wiki1", "XWiki", "initialauthor"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"),
            document.getContentAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcreator"), document.getCreatorReference());

        document.setContent("new content");

        assertEquals(new DocumentReference("wiki2", "XWiki", "contextuser"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki2", "XWiki", "contextuser"), document.getContentAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcreator"), document.getCreatorReference());
    }

    @Test
    void changeAuthorWhenModifyingObjectProperty() throws XWikiException
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("wiki0", "Space", "Page"));
        xdoc.setAuthorReference(new DocumentReference("wiki1", "XWiki", "initialauthor"));
        xdoc.setContentAuthorReference(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"));
        xdoc.setCreatorReference(new DocumentReference("wiki1", "XWiki", "initialcreator"));

        xdoc.getXClass().addTextField("key", "Key", 30);
        xdoc.newXObject(xdoc.getDocumentReference(), this.oldcore.getXWikiContext());

        xdoc.setContentDirty(false);
        this.oldcore.getSpyXWiki().saveDocument(xdoc, this.oldcore.getXWikiContext());

        this.oldcore.getXWikiContext().setUserReference(new DocumentReference("wiki2", "XWiki", "contextuser"));

        Document document = xdoc.newDocument(this.oldcore.getXWikiContext());

        assertEquals(new DocumentReference("wiki1", "XWiki", "initialauthor"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"),
            document.getContentAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcreator"), document.getCreatorReference());

        Object obj = document.getObject(xdoc.getPrefixedFullName());
        obj.set("key", "value");

        assertEquals(new DocumentReference("wiki2", "XWiki", "contextuser"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"),
            document.getContentAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcreator"), document.getCreatorReference());
    }

    @Test
    void changeAuthorWhenModifyingDocumentProperty() throws XWikiException
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("wiki0", "Space", "Page"));
        xdoc.setAuthorReference(new DocumentReference("wiki1", "XWiki", "initialauthor"));
        xdoc.setContentAuthorReference(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"));
        xdoc.setCreatorReference(new DocumentReference("wiki1", "XWiki", "initialcreator"));

        xdoc.getXClass().addTextField("key", "Key", 30);
        xdoc.newXObject(xdoc.getDocumentReference(), this.oldcore.getXWikiContext());

        xdoc.setContentDirty(false);
        this.oldcore.getSpyXWiki().saveDocument(xdoc, this.oldcore.getXWikiContext());

        this.oldcore.getXWikiContext().setUserReference(new DocumentReference("wiki2", "XWiki", "contextuser"));

        Document document = xdoc.newDocument(this.oldcore.getXWikiContext());

        assertEquals(new DocumentReference("wiki1", "XWiki", "initialauthor"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"),
            document.getContentAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcreator"), document.getCreatorReference());

        document.set("key", "value");

        assertEquals(new DocumentReference("wiki2", "XWiki", "contextuser"), document.getAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"),
            document.getContentAuthorReference());
        assertEquals(new DocumentReference("wiki1", "XWiki", "initialcreator"), document.getCreatorReference());
    }

    @Test
    void saveAsAuthorWhenNoPR(MockitoComponentManager componentManager) throws XWikiException, ComponentLookupException
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("wiki0", "Space", "Page"));
        xdoc.setAuthorReference(new DocumentReference("wiki1", "XWiki", "initialauthor"));
        xdoc.setContentAuthorReference(new DocumentReference("wiki1", "XWiki", "initialcontentauthor"));
        xdoc.setCreatorReference(new DocumentReference("wiki1", "XWiki", "initialcreator"));

        xdoc.setContentDirty(false);
        this.oldcore.getSpyXWiki().saveDocument(xdoc, this.oldcore.getXWikiContext());

        UserReferenceResolver<DocumentReference> userReferenceResolver = componentManager.getInstance(
            new DefaultParameterizedType(null, UserReferenceResolver.class, DocumentReference.class), "document");

        // Set context user
        DocumentReference contextUser = new DocumentReference("wiki2", "XWiki", "contextuser");
        this.oldcore.getXWikiContext().setUserReference(contextUser);
        UserReference userContextReference = userReferenceResolver.resolve(contextUser);
        // Set context author
        XWikiDocument contextDocument = new XWikiDocument("wiki1", "XWiki", "authordocument");
        DocumentReference authorReference = new DocumentReference("wiki3", "XWiki", "contextauthor");
        UserReference userAuthorReference = userReferenceResolver.resolve(authorReference);
        contextDocument.setContentAuthorReference(authorReference);
        this.oldcore.getSpyXWiki().saveDocument(xdoc, this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(contextDocument);

        when(this.oldcore.getMockAuthorizationManager().hasAccess(Right.EDIT, authorReference,
            xdoc.getDocumentReference())).thenReturn(true);
        when(this.oldcore.getMockRightService().hasProgrammingRights(this.oldcore.getXWikiContext())).thenReturn(false);

        Document document = xdoc.newDocument(this.oldcore.getXWikiContext());

        assertEquals(new DocumentReference("wiki1", "XWiki", "initialauthor"), document.getAuthorReference());

        when(this.oldcore.getMockRightService().hasAccessLevel("edit", this.oldcore.getXWikiContext().getUser(),
            document.getPrefixedFullName(), this.oldcore.getXWikiContext())).thenReturn(false);

        assertThrows(XWikiException.class, () -> document.save());

        when(this.oldcore.getMockRightService().hasAccessLevel("edit", this.oldcore.getXWikiContext().getUser(),
            document.getPrefixedFullName(), this.oldcore.getXWikiContext())).thenReturn(true);

        when(this.currentUserReferenceUserReferenceResolver.resolve(CurrentUserReference.INSTANCE))
            .thenReturn(userContextReference)
            .thenReturn(userContextReference)
            .thenReturn(userAuthorReference)
            .thenReturn(userContextReference);
        document.save();

        assertEquals(userContextReference, document.getAuthors().getOriginalMetadataAuthor());
        assertEquals(userAuthorReference, document.getAuthors().getEffectiveMetadataAuthor());

        when(this.oldcore.getMockRightService().hasProgrammingRights(this.oldcore.getXWikiContext())).thenReturn(true);

        document.save();

        assertEquals(userContextReference, document.getAuthors().getEffectiveMetadataAuthor());
    }

    @Test
    void getAuthors()
    {
        DocumentAuthors documentAuthors = mock(DocumentAuthors.class);
        AuthorizationManager mockAuthorizationManager = this.oldcore.getMockAuthorizationManager();
        XWikiContext context = this.oldcore.getXWikiContext();
        DocumentReference userReference = new DocumentReference("xwiki", "XWiki", "Foo");
        context.setUserReference(userReference);
        DocumentReference currentDocReference = mock(DocumentReference.class, "currentDocRef");
        XWikiDocument currentDoc = mock(XWikiDocument.class);
        when(currentDoc.getAuthors()).thenReturn(documentAuthors);
        Document document = new Document(currentDoc, context);

        when(currentDoc.getDocumentReference()).thenReturn(currentDocReference);

        when(mockAuthorizationManager.hasAccess(Right.PROGRAM, userReference, currentDocReference)).thenReturn(false);
        DocumentAuthors obtainedAuthors = document.getAuthors();
        assertTrue(obtainedAuthors instanceof SafeDocumentAuthors);
        assertEquals(new SafeDocumentAuthors(documentAuthors), obtainedAuthors);

        verify(mockAuthorizationManager).hasAccess(Right.PROGRAM, userReference, currentDocReference);

        when(mockAuthorizationManager.hasAccess(Right.PROGRAM, userReference, currentDocReference)).thenReturn(true);
        when(currentDoc.clone()).thenReturn(currentDoc);
        obtainedAuthors = document.getAuthors();
        assertSame(documentAuthors, obtainedAuthors);
        verify(mockAuthorizationManager, times(2)).hasAccess(Right.PROGRAM, userReference, currentDocReference);
        verify(currentDoc).clone();
    }

    @ParameterizedTest
    @ValueSource(booleans = { false, true })
    void getDocumentRevision(boolean allowAccess, MockitoComponentManager componentManager) throws Exception
    {
        DocumentReference documentReference = new DocumentReference("Wiki", "Space", "Page");
        XWikiDocument xWikiDocument = new XWikiDocument(documentReference);
        Document document = new Document(xWikiDocument, this.oldcore.getXWikiContext());
        DocumentRevisionProvider revisionProvider =
            componentManager.registerMockComponent(DocumentRevisionProvider.class);
        String revision = "42.1";
        XWikiDocument revisionDocument = mock(XWikiDocument.class);
        when(revisionProvider.getRevision(xWikiDocument, revision)).thenReturn(revisionDocument);
        String deniedMessage = "Denied";
        if (!allowAccess) {
            doThrow(new AuthorizationException(deniedMessage)).when(revisionProvider)
                .checkAccess(Right.VIEW, CurrentUserReference.INSTANCE, documentReference, revision);
            assertNull(document.getDocumentRevision(revision));
            assertEquals(1, this.logCapture.size());
            assertEquals(String.format("Access denied for loading revision [%s] of document [%s()]: "
                    + "[AuthorizationException: %s]", revision,
                documentReference, deniedMessage), this.logCapture.getMessage(0));
        } else {
            assertEquals(new Document(revisionDocument, this.oldcore.getXWikiContext()),
                document.getDocumentRevision(revision));
        }
        verify(revisionProvider).checkAccess(Right.VIEW, CurrentUserReference.INSTANCE, documentReference, revision);
    }
}
