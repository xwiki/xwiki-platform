package com.xpn.xwiki.web;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.jmock.Mock;
import org.jmock.core.Invocation;
import org.jmock.core.stub.CustomStub;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

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

    private XWikiConfig config;

    private XWikiServletURLFactory urlFactory = new XWikiServletURLFactory();

    private Map<String, Map<String, XWikiDocument>> databases = new HashMap<String, Map<String, XWikiDocument>>();

    /**
     * Flag indicating if the request is secure. A request is secure if either its URL uses the HTTPS scheme or the
     * receiving web application is behind a secure reverse proxy (e.g. the request was send to the reverse proxy
     * through HTTPS).
     * <p>
     * Tests can set this flag to control the value returned by {@link XWikiRequest#isSecure()}.
     */
    private boolean secure;

    /**
     * The map of HTTP headers.
     * <p>
     * Tests can add values to this map to control the value returned by {@link XWikiRequest#getHeader(String)}.
     */
    private final Map<String, String> httpHeaders = new HashMap<String, String>();

    private Map<String, XWikiDocument> getDocuments(String database, boolean create) throws XWikiException
    {
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

    private XWikiDocument getDocument(DocumentReference documentReference) throws XWikiException
    {
        XWikiDocument document = new XWikiDocument(documentReference);

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

        XWiki xwiki = new XWiki()
        {
            public XWikiDocument getDocument(String fullname, XWikiContext context) throws XWikiException
            {
                return XWikiServletURLFactoryTest.this.getDocument(Utils.getComponent(DocumentReferenceResolver.class,
                    "currentmixed").resolve(fullname));
            }

            public XWikiDocument getDocument(DocumentReference documentReference, XWikiContext context)
                throws XWikiException
            {
                return XWikiServletURLFactoryTest.this.getDocument(documentReference);
            }

            public String getXWikiPreference(String prefname, String defaultValue, XWikiContext context)
            {
                return defaultValue;
            }

            protected void registerWikiMacros()
            {

            }
        };
        xwiki.setConfig((this.config = new XWikiConfig()));
        xwiki.setDatabase(getContext().getDatabase());

        Mock mockXWikiRequest = mock(XWikiRequest.class, new Class[] {}, new Object[] {});
        mockXWikiRequest.stubs().method("getScheme").will(returnValue("http"));
        mockXWikiRequest.stubs().method("isSecure").will(new CustomStub("Implements ServletRequest.isSecure")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                return secure;
            }
        });
        mockXWikiRequest.stubs().method("getServletPath").will(returnValue(""));
        mockXWikiRequest.stubs().method("getContextPath").will(returnValue("/xwiki"));
        mockXWikiRequest.stubs().method("getHeader").will(new CustomStub("Implements HttpServletRequest.getHeader")
        {
            public Object invoke(Invocation invocation) throws Throwable
            {
                String headerName = (String) invocation.parameterValues.get(0);
                return httpHeaders.get(headerName);
            }
        });

        getContext().setWiki(xwiki);
        getContext().setRequest((XWikiRequest) mockXWikiRequest.proxy());

        // Create sub-wikis.
        createWiki("wiki1");
        createWiki("wiki2");

        getContext().setURL(new URL("http://127.0.0.1/xwiki/view/InitialSpace/InitialPage"));

        this.urlFactory.init(getContext());
    }

    /**
     * Creates a new sub-wiki with the given name.
     * 
     * @param wikiName the wiki name
     * @throws Exception if creating the wiki fails
     */
    private void createWiki(String wikiName) throws Exception
    {
        String wikiDocName = "XWikiServer" + wikiName.substring(0, 1).toUpperCase() + wikiName.substring(1);
        XWikiDocument wikiDoc = getDocument(new DocumentReference(MAIN_WIKI_NAME, "XWiki", wikiDocName));
        BaseObject wikiObj = wikiDoc.newObject("XWiki.XWikiServerClass", getContext());
        wikiObj.setStringValue("server", wikiName + "server");
        saveDocument(wikiDoc);
    }

    public void testCreateURLOnMainWiki() throws MalformedURLException
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki", getContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWiki() throws MalformedURLException
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWikiInVirtualMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnMainWikiInPathMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki", getContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWikiInPathMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
    }

    public void testCreateURLOnSubWikiInVirtualModeInPathMode() throws MalformedURLException
    {
        this.config.setProperty("xwiki.virtual", "1");
        this.config.setProperty("xwiki.virtual.usepath", "1");

        secure = true;
        // Change the context URL to include a port number and to use HTTPS.
        getContext().setURL(new URL("https://localhost:8080/xwiki/view/Main/"));
        // Reinitialize the URL factory to take into account the new context URL.
        urlFactory.init(getContext());

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("https://localhost:8080/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", urlFactory.getURL(url, getContext()));
    }

    /**
     * Checks the URLs created on the main wiki when XWiki is behind a reverse proxy.
     * 
     * @throws MalformedURLException shouldn't happen
     */
    public void testCreateURLOnMainWikiInReverseProxyMode() throws MalformedURLException
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(getContext());

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki", getContext());
        assertEquals(new URL("https://www.xwiki.org/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor", urlFactory.getURL(url, getContext()));
    }

    public void testCreateURLOnSubWikiInReverseProxyMode() throws MalformedURLException
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(getContext());

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        // The URL remains absolute in this case.
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor", urlFactory.getURL(url,
            getContext()));
    }

    public void testCreateURLOnSubWikiInVirtualModeInReverseProxyMode() throws MalformedURLException
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(getContext());

        config.setProperty("xwiki.virtual", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("https://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        // The URL remains absolute in this case.
        assertEquals("https://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor", urlFactory.getURL(url,
            getContext()));
    }

    public void testCreateURLOnMainWikiInPathModeInReverseProxyMode() throws MalformedURLException
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(getContext());

        config.setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki", getContext());
        assertEquals(new URL("http://www.xwiki.org/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor", urlFactory.getURL(url, getContext()));
    }

    public void testCreateURLOnSubWikiInPathModeInReverseProxyMode() throws MalformedURLException
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(getContext());

        config.setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("https://www.xwiki.org/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", urlFactory.getURL(url, getContext()));
    }

    public void testCreateURLOnSubWikiInVirtualModeInPathModeInReverseProxyMode() throws MalformedURLException
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org:8080");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(getContext());

        config.setProperty("xwiki.virtual", "1");
        config.setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1", getContext());
        assertEquals(new URL("http://www.xwiki.org:8080/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", urlFactory.getURL(url, getContext()));
    }

    /**
     * Tests how URLs are serialized when the request wiki (taken from the request URL) and the context wiki (explicitly
     * set from code on the XWiki context) are different.
     */
    public void testGetURLWhenRequestWikiAndContextWikiAreDifferent() throws MalformedURLException
    {
        getContext().setURL(new URL("http://wiki1server/xwiki/view/InitialSpace/InitialPage"));
        // Reinitialize the URL factory to take into account the new request URL.
        urlFactory.init(getContext());

        getContext().setDatabase("wiki2");

        String url = urlFactory.getURL(new URL("http://wiki1server/xwiki/bin/view/Space/Page"), getContext());
        assertEquals("/xwiki/bin/view/Space/Page", url);

        url = urlFactory.getURL(new URL("http://wiki2server/xwiki/bin/view/Space/Page"), getContext());
        assertEquals("http://wiki2server/xwiki/bin/view/Space/Page", url);
    }
}
