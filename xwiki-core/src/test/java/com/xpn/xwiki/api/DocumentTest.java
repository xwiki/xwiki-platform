package com.xpn.xwiki.api;

import java.util.List;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public class DocumentTest extends AbstractBridgedXWikiComponentTestCase
{
    public void testToStringReturnsFullName()
    {
        XWikiDocument doc = new XWikiDocument("Space", "Page");
        assertEquals("Space.Page", new Document(doc, new XWikiContext()).toString());
        assertEquals("Main.WebHome", new Document(new XWikiDocument(), new XWikiContext())
            .toString());
    }

    public void testGetObjects() throws XWikiException
    {
        XWikiContext context = new XWikiContext();        
        XWikiDocument doc = new XWikiDocument("Space", "Page");

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
    }
}
