/*
 * Copyright 2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
 *
 * @author ravenees
 */
package com.xpn.xwiki.web;

import java.util.ListResourceBundle;
import java.util.Date;
import java.util.List;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.doc.XWikiDocument;
import org.jmock.cglib.MockObjectTestCase;
import org.jmock.Mock;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.XWikiMessageTool} class.
 *
 * @version $Id: $
 */
public class XWikiMessageToolTest extends MockObjectTestCase
{
    private Mock mockXWiki;
    private XWikiMessageTool tool;

    protected void setUp()
    {
        this.tool = new XWikiMessageTool(new TestResources(), createXWikiContext());
    }

    public class TestResources extends ListResourceBundle
    {
        private final Object[][] contents = {
            {"key", "value"}
        };

        public Object[][] getContents()
        {
         return contents;
        }
     }

    /**
     * When no preference exist the returned value is the value of the key.
     */
    public void testGetWhenPreferenceDoesNotExist()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        assertEquals("invalid", this.tool.get("invalid"));
    }

    public void testGetWhenNoTranslationAvailable()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue(null));

        assertEquals("value", this.tool.get("key"));
    }

    /**
     * When the key is null the returned value is null. 
     */
    public void testGetWhenKeyIsNull()
    {
        assertNull(this.tool.get(null));
    }

    public void testGetWhenInXWikiPreferences()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(
            returnValue("Space1.Doc1, Space2.Doc2"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(createDocument(111111L, "somekey=somevalue", false)));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space2.Doc2"), ANYTHING)
            .will(returnValue(createDocument(222222L, "someKey=someValue\n"
                + "keyInXWikiPreferences=eureka", false)));

        assertEquals("eureka", this.tool.get("keyInXWikiPreferences"));
    }

    public void testGetWhenInXWikiConfigurationFile()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue("Space1.Doc1"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(createDocument(111111L, "keyInXWikiCfg=gotcha", false)));

        assertEquals("gotcha", this.tool.get("keyInXWikiCfg"));
    }

    /**
     * Verify that a document listed as a bundle document that doesn't exist is not returned as
     * a bundle document. 
     */
    public void testGetDocumentBundlesWhenDocumentDoesNotExist()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(createDocument(111111L, "", true)));
        List docs = this.tool.getDocumentBundles();
        assertEquals(0, docs.size());
    }

    public void testGetReturnsFromCacheWhenCalledTwice()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));

        Mock document = createMockDocument(11111L, "key=value", false);

        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(document.proxy()));

        // After this call, the value should be in cache.
        this.tool.get("key");

        // We verify that the second time the getContent method is NOT called as the value is
        // returned from cache
        document.expects(never()).method("getContent");
        this.tool.get("key");
    }

    public void testGetWhenDocumentModifiedAfterItIsInCache()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));

        Mock document = createMockDocument(11111L, "key=value", false);

        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(document.proxy()));

        // First time get any key just to put the doc properties in cache
        assertEquals("modifiedKey", this.tool.get("modifiedKey"));

        // Now modify the document content to add a new key and change the document's date
        document.stubs().method("getContent").will(returnValue("modifiedKey=found"));
        document.stubs().method("getDate").will(returnValue(new Date()));

        // Even though the document has been cached it's reloaded because it's date has changed
        assertEquals("found", this.tool.get("modifiedKey"));
    }

    private XWikiDocument createDocument(long id, String content, boolean isNew)
    {
        return (XWikiDocument) createMockDocument(id, content, isNew).proxy();
    }

    private Mock createMockDocument(long id, String content, boolean isNew)
    {
        Mock mockDocument = mock(XWikiDocument.class);
        XWikiDocument document = (XWikiDocument) mockDocument.proxy();
        mockDocument.stubs().method("getTranslatedDocument").will(returnValue(document));
        mockDocument.stubs().method("isNew").will(returnValue(isNew));
        mockDocument.stubs().method("getId").will(returnValue(new Long(id)));
        mockDocument.stubs().method("getDate").will(returnValue(new Date()));
        mockDocument.stubs().method("getContent").will(returnValue(content));
        return mockDocument;
    }

    private XWikiContext createXWikiContext()
    {
        XWikiContext context = new XWikiContext();
        XWikiConfig config = new XWikiConfig();
        this.mockXWiki = mock(XWiki.class,
            new Class[]{XWikiConfig.class, XWikiContext.class}, new Object[]{config, context});
        context.setWiki((XWiki) this.mockXWiki.proxy());
        return context;
    }
}
