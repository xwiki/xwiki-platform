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
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.objects.BaseProperty;
import com.xpn.xwiki.objects.classes.BaseClass;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

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

        doc.getxWikiClass().addNumberField("prop", "prop", 5, "long");
        BaseObject obj = (BaseObject) doc.getxWikiClass().newObject(context);
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
}
