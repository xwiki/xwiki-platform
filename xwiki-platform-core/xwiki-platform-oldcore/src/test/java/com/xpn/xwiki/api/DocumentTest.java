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

import junit.framework.Assert;

import org.jmock.Mock;
import org.jmock.core.stub.CustomStub;
import org.jmock.core.Invocation;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;
import com.xpn.xwiki.user.api.XWikiRightService;

public class DocumentTest extends AbstractBridgedXWikiComponentTestCase
{
    public void testToStringReturnsFullName()
    {
        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));
        assertEquals("Space.Page", new Document(doc, new XWikiContext()).toString());
        assertEquals("Main.WebHome", new Document(new XWikiDocument(), new XWikiContext())
            .toString());
    }

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

    public void testRemoveObjectDoesntCauseDataLoss() throws XWikiException
    {
        Mock mockXWiki = mock(XWiki.class);
        BaseClass c = new BaseClass();
        c.setDocumentReference(new DocumentReference("xwiki", "XWiki", "XWikiComments"));
        c.addTextAreaField("comment", "comment", 60, 20);
        mockXWiki.stubs().method("getXClass").will(returnValue(c));
        getContext().setWiki((XWiki) mockXWiki.proxy());

        XWikiDocument doc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));

        for (int i = 0; i < 10; ++i) {
            doc.newObject("XWiki.XWikiComments", getContext());
        }

        Document adoc = new Document(doc, getContext());

        for (Object obj : adoc.getObjects("XWiki.XWikiComments")) {
            obj.set("comment", "Comment");
            if (obj.getNumber() == 4) {
                adoc.removeObject(obj);
            }
        }

        // Let's make sure the original document wasn't changed
        for (BaseObject obj : doc.getObjects("XWiki.XWikiComments")) {
            Assert.assertNull(obj.get("comment"));
        }

        // Let's make sure the cloned document was changed everywhere
        for (BaseObject obj : adoc.getDoc().getObjects("XWiki.XWikiComments")) {
            if (obj != null) {
                Assert.assertEquals("Comment", ((BaseProperty) obj.get("comment")).getValue());
            }
        }
    }

    public void testSaveAsAuthorUsesGuestIfDroppedPermissions() throws XWikiException
    {
        final XWikiDocument xdoc = new XWikiDocument("Space", "Page");

        final Mock mockRightService = mock(XWikiRightService.class);

        mockRightService.expects(once())
            .method("hasAccessLevel").with(eq("edit"),
                                           eq("XWiki.Alice"),
                                           ANYTHING,
                                           ANYTHING).will(returnValue(true));

        mockRightService.expects(once())
            .method("hasAccessLevel").with(eq("edit"),
                                           eq("XWikiGuest"),
                                           ANYTHING,
                                           ANYTHING).will(returnValue(false));

        final Mock mockXWiki = mock(XWiki.class);
        mockXWiki.stubs().method("isVirtualMode")
        .will(returnValue(false));
        mockXWiki.stubs().method("getRightService")
            .will(returnValue(mockRightService.proxy()));
        mockXWiki.expects(once()).method("saveDocument").with(ANYTHING, ANYTHING, ANYTHING, ANYTHING)
            .will(new CustomStub("Make sure the contentAuthor is Alice") {
                @Override
                public Object invoke(Invocation invocation) throws Throwable
                {
                    assertEquals("Saving a document before calling dropPermissions() did not save as "
                                 + "the author.",
                                 "XWiki.Alice",
                                 ((XWikiContext) invocation.parameterValues.get(3)).getUser());
                    return null;
                }
            });
        this.getContext().setWiki((XWiki) mockXWiki.proxy());

        final Document doc = new Document(xdoc, this.getContext());
        this.getContext().setDoc(xdoc);

        // Alice is the author.
        xdoc.setContentAuthor("XWiki.Alice");

        // Bob is the viewer
        this.getContext().setUser("XWiki.Bob");

        doc.saveAsAuthor();

        this.getContext().dropPermissions();
        try {
            doc.saveAsAuthor();
            fail("saveAsAuthor did not throw an exception after dropPermissions() had been called.");
        } catch (XWikiException e) {
            assertTrue("Wrong error message when trying to save a document after calling dropPermissions()",
                       e.getMessage().contains("Access denied; user XWikiGuest, acting through script in "
                                             + "document Space.Page cannot save document Space.Page"));
        }

        assertEquals("After dropping permissions and attempting to save a document, the user was "
                     + "perminantly switched to guest.", "XWiki.Bob", this.getContext().getUser());
    }
    
    public void testUser()
    {
        XWikiDocument xdoc = new XWikiDocument(new DocumentReference("Wiki", "Space", "Page"));
        Document document = new Document(xdoc, getContext());
        
        assertEquals("", document.getCreator());
        assertEquals("", document.getAuthor());
        assertEquals("", document.getContentAuthor());
    }
}
