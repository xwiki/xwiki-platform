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

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.ListResourceBundle;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

/**
 * Unit tests for the {@link com.xpn.xwiki.web.XWikiMessageTool} class.
 * 
 * @version $Id$
 */
public class XWikiMessageToolTest extends AbstractBridgedXWikiComponentTestCase
{
    private Mock mockXWiki;

    private XWikiMessageTool tool;

    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.mockXWiki = mock(XWiki.class, new Class[] {}, new Object[] {});
        getContext().setWiki((XWiki) this.mockXWiki.proxy());
        
        this.mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));

        this.tool = new XWikiMessageTool(new TestResources(), getContext());
    }

    public class TestResources extends ListResourceBundle
    {
        private final Object[][] contents = {{"key", "value"}};

        @Override
        public Object[][] getContents()
        {
            return contents;
        }
    }

    private XWikiDocument createDocument(String name, String content, String language, String defaultLanguage,
        boolean isNew)
    {
        XWikiDocument doc = new XWikiDocument();

        doc.setFullName(name);
        doc.setContent(content);
        doc.setLanguage(language);
        doc.setDefaultLanguage(defaultLanguage);
        doc.setNew(isNew);

        return doc;
    }

    private XWikiDocument createDocument(long id, String name, String content, boolean isNew)
    {
        return (XWikiDocument) createMockDocument(id, name, content, isNew).proxy();
    }

    private Mock createMockDocument(long id, String name, String content, boolean isNew)
    {
        Mock mockDocument = mock(XWikiDocument.class);
        XWikiDocument document = (XWikiDocument) mockDocument.proxy();
        mockDocument.stubs().method("getTranslatedDocument").will(returnValue(document));
        mockDocument.stubs().method("isNew").will(returnValue(isNew));
        mockDocument.stubs().method("getId").will(returnValue(new Long(id)));
        mockDocument.stubs().method("getDate").will(returnValue(new Date()));
        mockDocument.stubs().method("getContent").will(returnValue(content));
        mockDocument.stubs().method("getFullName").will(returnValue(name));
        mockDocument.stubs().method("getRealLanguage").will(returnValue("en"));
        return mockDocument;
    }

    private XWikiDocument createDocumentWithTrans(long id, String name, String content, String transContent,
        boolean isNew)
    {
        return (XWikiDocument) createMockDocumentWithTrans(id, name, content, transContent, isNew).proxy();
    }

    private Mock createMockDocumentWithTrans(long id, String name, String content, String transContent, boolean isNew)
    {
        Mock mockDocument = mock(XWikiDocument.class);
        final XWikiDocument document = (XWikiDocument) mockDocument.proxy();
        final XWikiDocument transdocument = createDocument(name, transContent, "fr", "", false);
        mockDocument.stubs().method("getTranslatedDocument").will(new CustomStub("Implements getTranslatedDocument")
        {
            @Override
            public Object invoke(Invocation invocation) throws Throwable
            {
                if (invocation.parameterValues.size() == 1) {
                    XWikiContext context = (XWikiContext) invocation.parameterValues.get(0);
                    String lang = context.getLanguage();
                    if ("fr".equals(lang))
                        return transdocument;
                    else
                        return document;
                } else {
                    String lang = (String) invocation.parameterValues.get(0);
                    if ("fr".equals(lang))
                        return transdocument;
                    else
                        return document;
                }
            }
        });
        mockDocument.stubs().method("isNew").will(returnValue(isNew));
        mockDocument.stubs().method("getId").will(returnValue(new Long(id)));
        mockDocument.stubs().method("getDate").will(returnValue(new Date()));
        mockDocument.stubs().method("getContent").will(returnValue(content));
        mockDocument.stubs().method("getFullName").will(returnValue(name));
        mockDocument.stubs().method("getLanguage").will(returnValue(""));
        mockDocument.stubs().method("getDefaultLanguage").will(returnValue("en"));
        mockDocument.stubs().method("getRealLanguage").will(returnValue("en"));
        return mockDocument;
    }

    // Tests

    /**
     * When no preference exist the returned value is the value of the key.
     */
    public void testGetWhenPreferenceDoesNotExist()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue(null));
        this.mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));

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
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1, Space2.Doc2"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(createDocument(111111L, "Space1.Doc1", "somekey=somevalue", false)));
        this.mockXWiki
            .stubs()
            .method("getDocument")
            .with(eq("Space2.Doc2"), ANYTHING)
            .will(
                returnValue(createDocument(222222L, "Space2.Doc2", "someKey=someValue\n"
                    + "keyInXWikiPreferences=eureka", false)));

        assertEquals("eureka", this.tool.get("keyInXWikiPreferences"));
    }

    public void testGetWhenInXWikiConfigurationFile()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue("Space1.Doc1"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(createDocument(111111L, "Space1.Doc1", "keyInXWikiCfg=gotcha", false)));

        assertEquals("gotcha", this.tool.get("keyInXWikiCfg"));
    }

    /**
     * Validate usage of parameters in bundles
     */
    public void testGetWithParameters()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue(null));
        this.mockXWiki.stubs().method("Param").will(returnValue("Space1.Doc1"));
        this.mockXWiki
            .stubs()
            .method("getDocument")
            .with(eq("Space1.Doc1"), ANYTHING)
            .will(
                returnValue(createDocument(111111L, "Space1.Doc1",
                    "key=We have {0} new documents with {1} objects. {2}", false)));

        List<String> params = new ArrayList<String>();
        params.add("12");
        params.add("3");

        assertEquals("We have 12 new documents with 3 objects. {2}", this.tool.get("key", params));
    }

    /**
     * Verify that a document listed as a bundle document that doesn't exist is not returned as a bundle document.
     */
    public void testGetDocumentBundlesWhenDocumentDoesNotExist()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));
        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(createDocument(111111L, "Space1.Doc1", "", true)));
        List<XWikiDocument> docs = this.tool.getDocumentBundles();
        assertEquals(0, docs.size());
    }

    public void testGetReturnsFromCacheWhenCalledTwice()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));

        Mock document = createMockDocument(11111L, "Space1.Doc1", "key=value", false);

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

        Mock document = createMockDocument(11111L, "Space1.Doc1", "key=value", false);

        this.mockXWiki.stubs().method("getDocument").with(eq("Space1.Doc1"), ANYTHING)
            .will(returnValue(document.proxy()));

        // First time get any key just to put the doc properties in cache
        assertEquals("modifiedKey", this.tool.get("modifiedKey"));

        // Now modify the document content to add a new key and change the document's date. We add
        // one second to ensure the new date is definitely newer than the old one.
        document.stubs().method("getContent").will(returnValue("modifiedKey=found"));
        document.stubs().method("getDate").will(returnValue(new Date(System.currentTimeMillis() + 1000L)));

        // Even though the document has been cached it's reloaded because its date has changed
        assertEquals("found", this.tool.get("modifiedKey"));
    }

    public void testGetWhenWithTranslation()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));
        this.mockXWiki
            .stubs()
            .method("getDocument")
            .with(eq("Space1.Doc1"), ANYTHING)
            .will(
                returnValue(createDocumentWithTrans(111111L, "Space1.Doc1", "somekey=somevalue\nsomekey2=somevalue2",
                    "somekey=somevaluetrans", false)));

        getContext().setLanguage("en");
        assertEquals("somevalue", this.tool.get("somekey"));
        assertEquals("somevalue2", this.tool.get("somekey2"));

        // Switch to french
        getContext().setLanguage("fr");
        this.mockXWiki.stubs().method("getDefaultLanguage").will(returnValue("en"));
        assertEquals("somevaluetrans", this.tool.get("somekey"));
        assertEquals("somevalue2", this.tool.get("somekey2"));
    }

    public void testGetWhenWithUTF8Translation()
    {
        this.mockXWiki.stubs().method("getXWikiPreference").will(returnValue("Space1.Doc1"));
        this.mockXWiki
            .stubs()
            .method("getDocument")
            .with(eq("Space1.Doc1"), ANYTHING)
            .will(
                returnValue(createDocumentWithTrans(111111L, "Space1.Doc1", "somekey=some\u00E9value\nsomekey2=some\\u00E9value2",
                    "somekey=somevaluetrans", false)));

        assertEquals("some\u00E9value", this.tool.get("somekey"));
        assertEquals("some\u00E9value2", this.tool.get("somekey2"));
    }
}
