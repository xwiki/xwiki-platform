package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiConfig;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.AbstractBridgedXWikiComponentTestCase;

public class XWikiServletURLFactoryTest extends AbstractBridgedXWikiComponentTestCase
{
    private static final String MAIN_WIKI_NAME = "xwiki";

    private XWiki xwiki;

    private XWikiConfig config;

    private XWikiContext context;

    private XWikiServletURLFactory urlFactory = new XWikiServletURLFactory();

    private Map<String, Map<String, XWikiDocument>> databases = new HashMap<String, Map<String, XWikiDocument>>();

    private Map<String, XWikiDocument> getDocuments(String database, boolean create) throws XWikiException
    {
        if (database == null) {
            database = this.context.getDatabase();
        }

        if (database == null || database.length() == 0) {
            database = MAIN_WIKI_NAME;
        }

        if (!this.databases.containsKey(database)) {
            if (create) {
                this.databases.put(database, new HashMap<String, XWikiDocument>());
            } else {
                throw new XWikiException(XWikiException.MODULE_XWIKI_STORE, XWikiException.ERROR_XWIKI_UNKNOWN,
                    "Database " + database + " does not exists.");
            }
        }

        return this.databases.get(database);
    }

    private XWikiDocument getDocument(String documentFullName) throws XWikiException
    {
        XWikiDocument document = new XWikiDocument();
        document.setFullName(documentFullName);

        Map<String, XWikiDocument> docs = getDocuments(document.getDatabase(), false);

        if (docs.containsKey(document.getFullName())) {
            return docs.get(document.getFullName());
        } else {
            return document;
        }
    }

    private void saveDocument(XWikiDocument document) throws XWikiException
    {
        document.setNew(false);
        Map<String, XWikiDocument> database = getDocuments(document.getDatabase(), true);
        database.remove(document.getFullName());
        database.put(document.getFullName(), document);
    }

    /**
     * {@inheritDoc}
     * 
     * @see junit.framework.TestCase#setUp()
     */
    @Override
    protected void setUp() throws Exception
    {
        super.setUp();

        this.databases.put(MAIN_WIKI_NAME, new HashMap<String, XWikiDocument>());

        this.xwiki = new XWiki()
        {
            @Override
            public XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException
            {
                return XWikiServletURLFactoryTest.this.getDocument(fullname);
            }
        };
        this.xwiki.setConfig((this.config = new XWikiConfig()));

        Mock mockXWikiResquest = mock(XWikiRequest.class, new Class[] {}, new Object[] {});
        mockXWikiResquest.stubs().method("getServletPath").will(returnValue(""));
        mockXWikiResquest.stubs().method("getContextPath").will(returnValue("/xwiki"));
        mockXWikiResquest.stubs().method("getHeader").will(returnValue(null));

        this.context = new XWikiContext();
        this.context.setMainXWiki("xwiki");
        this.context.setDatabase("xwiki");
        this.context.setWiki(this.xwiki);
        this.context.setRequest((XWikiRequest) mockXWikiResquest.proxy());

        XWikiDocument wiki1Doc = getDocument("XWiki.XWikiServerWiki1");
        BaseObject wiki1Obj = wiki1Doc.newObject("XWiki.XWikiServerClass", this.context);
        wiki1Obj.setStringValue("server", "wiki1server");
        saveDocument(wiki1Doc);

        this.context.setURL(new URL("http://127.0.0.1/xwiki/view/InitialSpace/InitialPage"));

        this.urlFactory.init(context);
    }

    public void testCreateURLOnMainWiki() throws MalformedURLException
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki", this.context);
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWiki() throws MalformedURLException
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", this.context);
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWikiInVirtualMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", this.context);
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnMainWikiInPathMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki", this.context);
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWikiInPathMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", this.context);
        assertEquals(new URL("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWikiInVirtualModeInPathMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual", "1");
        this.config.setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", this.context);
        assertEquals(new URL("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
    }
}
