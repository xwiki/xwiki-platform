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

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.test.annotation.BeforeComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.test.MockitoOldcoreRule;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.Assert.assertEquals;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiServletURLFactory}.
 * 
 * @version $Id$
 */
@ReferenceComponentList
public class XWikiServletURLFactoryTest
{
    private static final String MAIN_WIKI_NAME = "xwiki";

    @Rule
    public MockitoOldcoreRule oldcore = new MockitoOldcoreRule();

    private XWikiServletURLFactory urlFactory;

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
    private final Map<String, String> httpHeaders = new HashMap<>();

    @BeforeComponent
    public void beforeComponent() throws Exception
    {
        EntityResourceActionLister actionLister =
            this.oldcore.getMocker().registerMockComponent(EntityResourceActionLister.class);
        when(actionLister.listActions()).thenReturn(Arrays.asList("view"));
    }

    @Before
    public void before() throws Exception
    {
        doReturn("DefaultSpace").when(this.oldcore.getSpyXWiki()).getDefaultSpace(any(XWikiContext.class));

        // Request
        XWikiRequest mockXWikiRequest = mock(XWikiRequest.class);
        when(mockXWikiRequest.getScheme()).thenReturn("http");
        when(mockXWikiRequest.isSecure()).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return secure;
            }
        });
        when(mockXWikiRequest.getServletPath()).thenReturn("");
        when(mockXWikiRequest.getContextPath()).thenReturn("/xwiki");
        when(mockXWikiRequest.getHeader(anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return httpHeaders.get(invocation.getArgumentAt(0, String.class));
            }
        });
        this.oldcore.getXWikiContext().setRequest(mockXWikiRequest);

        // Response
        XWikiResponse xwikiResponse = mock(XWikiResponse.class);
        when(xwikiResponse.encodeURL(anyString())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgumentAt(0, String.class);
            }
        });
        this.oldcore.getXWikiContext().setResponse(xwikiResponse);

        // Create sub-wikis.
        createWiki("wiki1");
        createWiki("wiki2");

        this.oldcore.getXWikiContext().setURL(new URL("http://127.0.0.1/xwiki/view/InitialSpace/InitialPage"));

        this.urlFactory = new XWikiServletURLFactory();
        this.urlFactory.init(this.oldcore.getXWikiContext());
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
        XWikiDocument wikiDoc = this.oldcore.getSpyXWiki()
            .getDocument(new DocumentReference(MAIN_WIKI_NAME, "XWiki", wikiDocName), this.oldcore.getXWikiContext());
        BaseObject wikiObj =
            wikiDoc.newXObject(new LocalDocumentReference("XWiki", "XWikiServerClass"), this.oldcore.getXWikiContext());
        wikiObj.setStringValue("server", wikiName + "server");
        this.oldcore.getSpyXWiki().saveDocument(wikiDoc, this.oldcore.getXWikiContext());
    }

    // Tests

    @Test
    public void createURLOnMainWiki() throws MalformedURLException
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnSubWiki() throws MalformedURLException
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnSubWikiInVirtualMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnMainWikiInPathMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnSubWikiInPathMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnSubWikiInVirtualModeInPathMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        secure = true;
        // Change the context URL to include a port number and to use HTTPS.
        this.oldcore.getXWikiContext().setURL(new URL("https://localhost:8080/xwiki/view/Main/"));
        // Reinitialize the URL factory to take into account the new context URL.
        urlFactory.init(this.oldcore.getXWikiContext());

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("https://localhost:8080/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnMainWikiInDomainMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnSubWikiInDomainMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    @Test
    public void createURLOnSubWikiInVirtualModeInDomainMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
    }

    /**
     * Checks the URLs created on the main wiki when XWiki is behind a reverse proxy.
     * 
     * @throws MalformedURLException shouldn't happen
     */
    @Test
    public void createURLOnMainWikiInDomainModeInReverseProxyMode() throws MalformedURLException
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("https://www.xwiki.org/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiInDomainModeInReverseProxyMode() throws MalformedURLException
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        // The URL remains absolute in this case.
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiInVirtualModeInDomainModeInReverseProxyMode() throws MalformedURLException
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("https://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        // The URL remains absolute in this case.
        assertEquals("https://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnMainWikiInPathModeInReverseProxyMode() throws MalformedURLException
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://www.xwiki.org/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiInPathModeInReverseProxyMode() throws MalformedURLException
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("https://www.xwiki.org/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiInVirtualModeInPathModeInReverseProxyMode() throws MalformedURLException
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org:8080");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://www.xwiki.org:8080/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    /**
     * Tests how URLs are serialized when the request wiki (taken from the request URL) and the context wiki (explicitly
     * set from code on the XWiki context) are different.
     */
    @Test
    public void getURLWhenRequestWikiAndContextWikiAreDifferent() throws MalformedURLException
    {
        this.oldcore.getXWikiContext().setURL(new URL("http://wiki1server/xwiki/view/InitialSpace/InitialPage"));
        // Reinitialize the URL factory to take into account the new request URL.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getXWikiContext().setWikiId("wiki2");

        String url =
            urlFactory.getURL(new URL("http://wiki1server/xwiki/bin/view/Space/Page"), this.oldcore.getXWikiContext());
        assertEquals("/xwiki/bin/view/Space/Page", url);

        url =
            urlFactory.getURL(new URL("http://wiki2server/xwiki/bin/view/Space/Page"), this.oldcore.getXWikiContext());
        assertEquals("http://wiki2server/xwiki/bin/view/Space/Page", url);
    }

    /** When the URL contains only the hostname, without a path, / is returned instead of the empty string. */
    @Test
    public void getURLWithEmptyPathReturnsSlash() throws MalformedURLException
    {
        this.oldcore.getXWikiContext().setURL(new URL("http://wiki1server/xwiki/view/InitialSpace/InitialPage"));
        // Reinitialize the URL factory to take into account the new request URL.
        this.urlFactory.init(this.oldcore.getXWikiContext());

        String url = this.urlFactory.getURL(new URL("http://wiki1server/"), this.oldcore.getXWikiContext());
        assertEquals("/", url);
    }

    /**
     * When getServerURL is called on a resource from the main wiki, the user is in a subwiki, and xwiki.home is set,
     * xwiki.home should be returned. see: XWIKI-5981
     */
    @Test
    public void getServerURLFromVirtualWithXWikiDotHomeEnabled() throws MalformedURLException
    {
        this.oldcore.getXWikiContext()
            .setURL(new URL("http://virtual1.mywiki.tld/xwiki/view/InitialSpace/InitialPage"));
        this.oldcore.getXWikiContext().setWikiId("subwiki");

        // This is called by XWiki#getXWiki() and is set to whatever the user asks for.
        // The test sets it to "xwiki" which is wrong for this test.
        this.oldcore.getXWikiContext().setOriginalWikiId("subwiki");

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");
        urlFactory.init(this.oldcore.getXWikiContext());
        assertEquals("http://mainwiki.mywiki.tld/",
            urlFactory.getServerURL("xwiki", this.oldcore.getXWikiContext()).toString());
    }

    /**
     * Proves that from a virtual wiki, URLs generated to point to the main wiki will use xwiki.home. see: XWIKI-5981
     */
    @Test
    public void createURLWhenWikiDotHomeParameterFromVirtualWiki() throws MalformedURLException
    {
        this.oldcore.getXWikiContext()
            .setURL(new URL("http://virtual1.mywiki.tld/xwiki/view/InitialSpace/InitialPage"));
        this.oldcore.getXWikiContext().setWikiId("subwiki");

        // This is called by XWiki#getXWiki() and is set to whatever the user asks for.
        // The test sets it to "xwiki" which is wrong for this test.
        this.oldcore.getXWikiContext().setOriginalWikiId("subwiki");

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual", "1");

        // Reinitialize the URL factory to take into account the new request URL.
        urlFactory.init(this.oldcore.getXWikiContext());

        // No wiki passed, assume same wiki. we should expect it to return http://virtual1.mywiki.tld/
        URL url =
            urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", null, this.oldcore.getXWikiContext());
        assertEquals(new URL("http://virtual1.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        // We are already in virtual1 so it should be a relative reference.
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));

        // Pass "xwiki" as the wiki, expect it to return the main wiki as set in the xwiki.home parameter.
        url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    /**
     * When getServerURL is called on a resource from the main wiki, the user is in the main wiki, and xwiki.home is
     * set, xwiki.home should be returned. see: XWIKI-5981
     */
    @Test
    public void getServerURLNonVirtualModeWithXWikiDotHomeEnabled() throws MalformedURLException
    {
        this.oldcore.getXWikiContext().setURL(new URL("http://127.0.0.1:8080/xwiki/view/InitialSpace/InitialPage"));
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        urlFactory.init(this.oldcore.getXWikiContext());
        // TODO: Fix getServerURL() so that is is consistent about returning a trailing / or not.
        assertEquals("http://mainwiki.mywiki.tld",
            urlFactory.getServerURL("xwiki", this.oldcore.getXWikiContext()).toString());
        assertEquals("http://mainwiki.mywiki.tld",
            urlFactory.getServerURL(null, this.oldcore.getXWikiContext()).toString());
    }

    /**
     * Proves that in a single wiki instance, URLs are always generated using xwiki.home if present. see: XWIKI-5981
     */
    @Test
    public void createURLWhenXWikiDotHomeParameterNonVirtualMode() throws MalformedURLException
    {
        // Some proxies will modify the host field without adding a x-forwarded-host field,
        // Using xwiki.home we should be able to make it work anyway.
        this.oldcore.getXWikiContext().setURL(new URL("http://localhost:8080/xwiki/view/InitialSpace/InitialPage"));
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        // Reinitialize the URL factory to take into account the new request URL.
        urlFactory.init(this.oldcore.getXWikiContext());

        // No wiki passed, assume main wiki. we should expect it to return mainwiki.mywiki.tld and not
        // xwiki.mywiki.tld.
        URL url =
            urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", null, this.oldcore.getXWikiContext());
        assertEquals(new URL("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));

        // Pass "xwiki" as the wiki, expect same result.
        url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals(new URL("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor"), url);
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    /**
     * Verify that jsessionid is removed from URL.
     */
    @Test
    public void normalizeURL() throws Exception
    {
        assertEquals(new URL("http://www.xwiki.org/xwiki/bin/view/Blog/Bug+Fixing+Day+35?language=en"),
            XWikiServletURLFactory.normalizeURL("http://www.xwiki.org/xwiki/bin/view/Blog/Bug+Fixing+Day+35"
                + ";jsessionid=0AF95AFB8997826B936C0397DF6A0C7F?language=en", this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLWithNestedSpaces() throws Exception
    {
        URL url = this.urlFactory.createURL("Space1.Space2", "Page", this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/Space1/Space2/Page"), url);
    }

    @Test
    public void createAttachmentURLWhenViewRevAndRevSpecifiedAndIsNotContextDoc() throws Exception
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        xwikiContext.put("rev", "1.0");
        xwikiContext.setAction("viewrev");
        xwikiContext.setDoc(new XWikiDocument(new DocumentReference("currentwiki", "currentspace", "currentpage")));
        URL url = this.urlFactory.createAttachmentURL("file", "space", "page", "viewrev", null, xwikiContext);
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/viewrev/space/page/file"), url);
    }

    @Test
    public void createAttachmentURLWhenViewRevAndRevSpecifiedAndIsContextDocAndAttachmentDoesntExist()
        throws Exception
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        xwikiContext.put("rev", "1.0");
        xwikiContext.setAction("viewrev");

        XWikiDocument document = new XWikiDocument(new DocumentReference("currentwiki", "currentspace", "currentpage"));
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        xwikiContext.setDoc(document);
        xwikiContext.setWikiId("currentwiki");

        URL url =
            this.urlFactory.createAttachmentURL("file", "currentspace", "currentpage", "viewrev", null, xwikiContext);
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/viewattachrev/currentspace/currentpage/file"), url);
    }

    @Test
    public void createURLWhenShowViewActionFalse() throws Exception
    {
        doReturn(false).when(this.oldcore.getSpyXWiki()).showViewAction(any(XWikiContext.class));

        URL url = this.urlFactory.createURL("Space", "Page", "view", this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/Space/Page"), url);
    }

    @Test
    public void createURLWhenShowViewActionFalseAndSpaceIsNamedAfterAnAction() throws Exception
    {
        doReturn(false).when(this.oldcore.getSpyXWiki()).showViewAction(any(XWikiContext.class));

        URL url = this.urlFactory.createURL("view.space2", "page", "view", this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/bin/view/view/space2/page"), url);
    }

    @Test
    public void createResourceURL() throws Exception
    {
        // Verify that the URL factory encodes each path segment.
        URL url = this.urlFactory.createResourceURL("o;ne/t?w&o/t=hr#e e", false, this.oldcore.getXWikiContext());
        assertEquals(new URL("http://127.0.0.1/xwiki/resources/o;ne/t%3Fw&o/t=hr%23e%20e"), url);
    }

    @Test
    public void createURLWhenCharactersNeedToBeEncoded() throws Exception
    {
        // Note: The query string is not encoded, and used as is. It's the responsibility of the caller to
        // url-encode it.
        // See XWikiServletURLFactory#encodeWithinPath() and XWikiServletURLFactory#encodeWithinQuery() for explanations
        // Note: We also verify that the single quote is encoded since otherwise that could cause problems in HTML when
        // the returned URL is used in the HREF attribute of a A tag (when using single quote delimiters).
        URL url = this.urlFactory.createURL("a b.c+d'", "e f", "view", "g=h%20i", "j k+l", this.oldcore.getXWikiContext());

        assertEquals("http://127.0.0.1/xwiki/bin/view/a%20b/c%2Bd%27/e%20f?g=h%20i#j%20k%2Bl", url.toString());
    }
}
