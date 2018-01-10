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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;
import com.xpn.xwiki.user.api.XWikiRightService;

import static com.xpn.xwiki.test.mockito.OldcoreMatchers.anyXWikiContext;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ReferenceComponentList
public class DocumentTest
{
    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    @Test
    public void testToStringReturnsFullName()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));
        assertEquals("Space.Page", new Document(doc, new XWikiContext()).toString());
        assertEquals("Main.WebHome", new Document(new XWikiDocument(), new XWikiContext()).toString());
    }

    @Test
    public void testGetObjects() throws XWikiException
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
    public void testRemoveObjectDoesntCauseDataLoss() throws XWikiException
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
    public void testSaveAsAuthorUsesGuestIfDroppedPermissions() throws XWikiException
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("wiki", "Space", "Page"));

        when(this.oldcore.getMockRightService().hasAccessLevel(eq("edit"), eq("XWiki.Alice"), any(),
            anyXWikiContext())).thenReturn(true);
        when(this.oldcore.getMockRightService().hasAccessLevel(eq("edit"), eq("XWikiGuest"), any(),
            anyXWikiContext())).thenReturn(false);

        Document doc = xdoc.newDocument(this.oldcore.getXWikiContext());
        this.oldcore.getXWikiContext().setDoc(xdoc);

        // Alice is the author.
        xdoc.setAuthorReference(new DocumentReference("wiki", "XWiki", "Alice"));
        xdoc.setContentAuthorReference(xdoc.getAuthorReference());

        // Bob is the viewer
        this.oldcore.getXWikiContext().setUser("XWiki.Bob");

        doc.saveAsAuthor();

        this.oldcore.getXWikiContext().dropPermissions();
        try {
            doc.saveAsAuthor();
            fail("saveAsAuthor did not throw an exception after dropPermissions() had been called.");
        } catch (XWikiException e) {
            assertTrue("Wrong error message when trying to save a document after calling dropPermissions()",
                e.getMessage().contains("Access denied; user XWikiGuest, acting through script in "
                    + "document Space.Page cannot save document Space.Page"));
        }

        assertEquals("After dropping permissions and attempting to save a document,"
            + "the user was permanantly switched to guest.", "XWiki.Bob", this.oldcore.getXWikiContext().getUser());
    }

    @Test
    public void testUser()
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));
        Document document = xdoc.newDocument(this.oldcore.getXWikiContext());

        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, document.getCreator());
        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, document.getAuthor());
        assertEquals(XWikiRightService.GUEST_USER_FULLNAME, document.getContentAuthor());
    }

    @Test
    public void testChangeAuthorWhenModifyingDocumentContent()
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
    public void testChangeAuthorWhenModifyingObjectProperty() throws XWikiException
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
    public void testChangeAuthorWhenModifyingDocumentProperty() throws XWikiException
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
    public void testHashCode()
    {
        XWikiDocument doc = new XWikiDcoument(new DocumentReference("test", "test", "test"));
        XWikiContext context = new XWikiContext();

        Document d1 = new Document(doc, context);
        Document d2 = new Document(doc, context);

        Assert.assertEquals(d1.hashCode(), d2.hashCode());
    }
}
