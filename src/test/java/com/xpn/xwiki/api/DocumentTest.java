package com.xpn.xwiki.api;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

import junit.framework.TestCase;

public class DocumentTest extends TestCase
{
    public void testToStringReturnsFullName()
    {
        XWikiDocument doc = new XWikiDocument("Space", "Page");
        assertEquals("Space.Page", new Document(doc, new XWikiContext()).toString());
        assertEquals("Main.WebHome", new Document(new XWikiDocument(), new XWikiContext())
            .toString());
    }
}
