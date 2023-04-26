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
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.resource.internal.entity.EntityResourceActionLister;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.wiki.descriptor.WikiDescriptor;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;
import org.xwiki.wiki.manager.WikiManagerException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.test.MockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.InjectMockitoOldcore;
import com.xpn.xwiki.test.junit5.mockito.OldcoreTest;
import com.xpn.xwiki.test.reference.ReferenceComponentList;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link XWikiServletURLFactory}.
 * 
 * @version $Id$
 */
@OldcoreTest
@ReferenceComponentList
public class XWikiServletURLFactoryTest
{
    @MockComponent
    private WikiDescriptorManager descriptorManager;

    @MockComponent
    private EntityResourceActionLister actionLister;

    @InjectMockitoOldcore
    private MockitoOldcore oldcore;

    private XWikiServletURLFactory urlFactory;

    private XWikiRequest mockXWikiRequest;

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

    @BeforeEach
    public void beforeEach() throws Exception
    {
        when(this.actionLister.listActions()).thenReturn(Arrays.asList("view"));
        when(this.descriptorManager.getMainWikiDescriptor()).thenReturn(new WikiDescriptor("xwiki", "localhost"));

        doReturn("DefaultSpace").when(this.oldcore.getSpyXWiki()).getDefaultSpace(any(XWikiContext.class));

        // Request
        this.mockXWikiRequest = mock(XWikiRequest.class);
        prepareMockRequest("127.0.0.1", -1);

        // Response
        XWikiResponse xwikiResponse = mock(XWikiResponse.class);
        when(xwikiResponse.encodeURL(any())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return invocation.getArgument(0);
            }
        });
        this.oldcore.getXWikiContext().setResponse(xwikiResponse);

        // Create sub-wikis.
        createWiki("wiki1");
        createWiki("wiki2");

        this.urlFactory = new XWikiServletURLFactory();
        this.urlFactory.init(this.oldcore.getXWikiContext());
    }

    /**
     * Creates a new sub-wiki with the given name.
     * 
     * @param wikiName the wiki name
     * @throws XWikiException if creating the wiki fails
     */
    private void createWiki(String wikiName) throws XWikiException, WikiManagerException
    {
        WikiDescriptor wikidescriptor = new WikiDescriptor(wikiName, wikiName + "server");

        when(this.descriptorManager.getById(wikiName)).thenReturn(wikidescriptor);
    }

    private void prepareMockRequest(String host, int port)
    {
        when(this.mockXWikiRequest.getScheme()).thenReturn("http");
        when(this.mockXWikiRequest.getServerName()).thenReturn(host);
        when(this.mockXWikiRequest.getServerPort()).thenReturn(port);
        when(this.mockXWikiRequest.isSecure()).then(new Answer<Boolean>()
        {
            @Override
            public Boolean answer(InvocationOnMock invocation) throws Throwable
            {
                return secure;
            }
        });
        when(this.mockXWikiRequest.getServletPath()).thenReturn("");
        when(this.mockXWikiRequest.getContextPath()).thenReturn("/xwiki");
        when(this.mockXWikiRequest.getHeader(any())).then(new Answer<String>()
        {
            @Override
            public String answer(InvocationOnMock invocation) throws Throwable
            {
                return httpHeaders.get(invocation.getArgument(0));
            }
        });
        this.oldcore.getXWikiContext().setRequest(mockXWikiRequest);
    }

    private void initRequest(String host, int port)
    {
        prepareMockRequest(host, port);

        // Reinitialize the URL factory to take into account the new request URL.
        urlFactory.init(this.oldcore.getXWikiContext());
    }

    private void initDaemonRequest(String host, int port)
    {
        this.mockXWikiRequest = mock(XWikiServletRequestStub.class);
        when(((XWikiServletRequestStub) this.mockXWikiRequest).isDaemon()).thenReturn(true);
        when(((XWikiServletRequestStub) this.mockXWikiRequest).getHttpServletRequest())
            .thenReturn(this.mockXWikiRequest);

        initRequest(host, port);
    }

    // Tests

    @Test
    public void createURLOnMainWiki()
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
    }

    @Test
    public void createURLOnSubWiki()
    {
        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", url.toString());
    }

    @Test
    public void createURLOnMainWikiInPathMode()
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
    }

    @Test
    public void createURLOnSubWikiInPathMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", url.toString());

        verify(this.oldcore.getSpyXWiki(), times(0)).getServerURL("wiki1", this.oldcore.getXWikiContext());
        verify(this.oldcore.getSpyXWiki(), times(0)).getServerURL(this.oldcore.getXWikiContext().getMainXWiki(),
            this.oldcore.getXWikiContext());
    }

    @Test
    public void createURLOnSubWikiInPathModeDaemonThread() throws WikiManagerException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");
        initDaemonRequest("request", 8080);
        WikiDescriptor descriptor = new WikiDescriptor("mainwiki", "mainwiki");
        descriptor.setPort(42);
        when(this.descriptorManager.getById(this.oldcore.getXWikiContext().getMainXWiki())).thenReturn(descriptor);

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://mainwiki:42/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", url.toString());
    }

    @Test
    public void createURLOnSubWikiFromSubWikiInPathMode() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        // Initialize the URL factory based on the subwiki instead of the main one
        this.oldcore.getXWikiContext().setOriginalWikiId("wiki1");
        initRequest("origin", 42);

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://origin:42/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", url.toString());

        verify(this.oldcore.getSpyXWiki(), times(0)).getServerURL("wiki1", this.oldcore.getXWikiContext());
        verify(this.oldcore.getSpyXWiki(), times(0)).getServerURL(this.oldcore.getXWikiContext().getMainXWiki(),
            this.oldcore.getXWikiContext());
    }

    @Test
    public void createSecureURLOnSubWikiInPathMode()
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        secure = true;
        initRequest("localhost", 8080);

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("https://localhost:8080/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnMainWikiInDomainMode()
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
    }

    @Test
    public void createURLOnSubWikiInDomainMode()
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
    }

    /**
     * Checks the URLs created on the main wiki when XWiki is behind a reverse proxy.
     */
    @Test
    public void createURLOnMainWikiInDomainModeInReverseProxyMode()
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("https://www.xwiki.org/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiInDomainModeInReverseProxyMode()
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        // The URL remains absolute in this case.
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiModeInDomainModeInReverseProxyMode()
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        // The URL remains absolute in this case.
        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnMainWikiInPathModeInReverseProxyMode()
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("http://www.xwiki.org/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiInPathModeInReverseProxyHost()
    {
        secure = true;
        httpHeaders.put("x-forwarded-host", "www.xwiki.org");
        // Reinitialize the URL factory to take into account the new security level and HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("https://www.xwiki.org/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnSubWikiModeInPathModeInReverseProxyHostPort()
    {
        httpHeaders.put("x-forwarded-host", "www.xwiki.org:8080");
        // Reinitialize the URL factory to take into account the new HTTP headers.
        urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "wiki1",
            this.oldcore.getXWikiContext());
        assertEquals("http://www.xwiki.org:8080/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            url.toString());
        assertEquals("/xwiki/wiki/wiki1server/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    @Test
    public void createURLOnMainWikiInPathModeWithForcedProtocol()
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.url.protocol", "https");
        // Reinitialize the URL factory to take into account the configuration
        this.urlFactory.init(this.oldcore.getXWikiContext());

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "1");

        URL url = this.urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("https://127.0.0.1/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
    }

    /**
     * Tests how URLs are serialized when the request wiki (taken from the request URL) and the context wiki (explicitly
     * set from code on the XWiki context) are different.
     */
    @Test
    public void getURLWhenRequestWikiAndContextWikiAreDifferent() throws MalformedURLException
    {
        initRequest("wiki1server", -1);

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
        initRequest("wiki1server", -1);

        String url = this.urlFactory.getURL(new URL("http://wiki1server/"), this.oldcore.getXWikiContext());
        assertEquals("/", url);
    }

    /**
     * Make sure the right reference URL used in daemon mode.
     */
    @Test
    public void getURLWhenDeamonRequest() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.virtual.usepath", "0");

        // Set a deamon request
        initDaemonRequest("request", 42);

        this.oldcore.getXWikiContext().setWikiId("wiki1");

        assertEquals("/xwiki/bin/view/Space/Page",
            urlFactory.getURL(new URL("http://wiki1server/xwiki/bin/view/Space/Page"), this.oldcore.getXWikiContext()));

        assertEquals("http://wiki2server/xwiki/bin/view/Space/Page",
            urlFactory.getURL(new URL("http://wiki2server/xwiki/bin/view/Space/Page"), this.oldcore.getXWikiContext()));

        this.oldcore.getXWikiContext().setWikiId("wiki2");

        assertEquals("http://wiki1server/xwiki/bin/view/Space/Page",
            urlFactory.getURL(new URL("http://wiki1server/xwiki/bin/view/Space/Page"), this.oldcore.getXWikiContext()));

        assertEquals("/xwiki/bin/view/Space/Page",
            urlFactory.getURL(new URL("http://wiki2server/xwiki/bin/view/Space/Page"), this.oldcore.getXWikiContext()));
    }

    /**
     * When getServerURL is called on a resource from the main wiki, the user is in a subwiki, and xwiki.home is set,
     * xwiki.home should be returned. see: XWIKI-5981
     */
    @Test
    public void getServerURLFromSubWikiWithXWikiDotHomeEnabled() throws MalformedURLException
    {
        // This is called by XWiki#getXWiki() and is set to whatever the user asks for.
        // The test sets it to "xwiki" which is wrong for this test.
        this.oldcore.getXWikiContext().setOriginalWikiId("subwiki");

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        this.oldcore.getXWikiContext().setWikiId("subwiki");

        initRequest("virtual1.mywiki.tld", -1);

        assertEquals("http://mainwiki.mywiki.tld/",
            urlFactory.getServerURL("xwiki", this.oldcore.getXWikiContext()).toString());
    }

    /**
     * Proves that from a virtual wiki, URLs generated to point to the main wiki will use xwiki.home. see: XWIKI-5981
     */
    @Test
    public void createURLWhenWikiDotHomeParameterFromSubWiki()
    {
        this.oldcore.getXWikiContext().setWikiId("subwiki");

        // This is called by XWiki#getXWiki() and is set to whatever the user asks for.
        // The test sets it to "xwiki" which is wrong for this test.
        this.oldcore.getXWikiContext().setOriginalWikiId("subwiki");

        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");

        initRequest("virtual1.mywiki.tld", -1);

        // No wiki passed, assume same wiki. we should expect it to return http://virtual1.mywiki.tld/
        URL url =
            urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", null, this.oldcore.getXWikiContext());
        assertEquals("http://virtual1.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        // We are already in virtual1 so it should be a relative reference.
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));

        // Pass "xwiki" as the wiki, expect it to return the main wiki as set in the xwiki.home parameter.
        url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    /**
     * When getServerURL is called on a resource from the main wiki, the user is in the main wiki, and xwiki.home is
     * set, xwiki.home should be returned. see: XWIKI-5981
     */
    @Test
    public void getServerURLWithXWikiDotHomeEnabled() throws MalformedURLException
    {
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        initRequest("localhost", 8080);

        // TODO: Fix getServerURL() so that is is consistent about returning a trailing / or not.
        assertEquals("http://mainwiki.mywiki.tld/",
            urlFactory.getServerURL("xwiki", this.oldcore.getXWikiContext()).toString());
        assertEquals("http://mainwiki.mywiki.tld/",
            urlFactory.getServerURL(null, this.oldcore.getXWikiContext()).toString());
    }

    /**
     * Proves that in a single wiki instance, URLs are always generated using xwiki.home if present. see: XWIKI-5981
     */
    @Test
    public void createURLWhenXWikiDotHomeParameterNonVirtualMode()
    {
        // Some proxies will modify the host field without adding a x-forwarded-host field,
        // Using xwiki.home we should be able to make it work anyway.
        this.oldcore.getMockXWikiCfg().setProperty("xwiki.home", "http://mainwiki.mywiki.tld/");
        initRequest("localhost", 8080);

        // No wiki passed, assume main wiki. we should expect it to return mainwiki.mywiki.tld and not
        // xwiki.mywiki.tld.
        URL url =
            urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", null, this.oldcore.getXWikiContext());
        assertEquals("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));

        // Pass "xwiki" as the wiki, expect same result.
        url = urlFactory.createURL("Space", "Page", "view", "param1=1", "anchor", "xwiki",
            this.oldcore.getXWikiContext());
        assertEquals("http://mainwiki.mywiki.tld/xwiki/bin/view/Space/Page?param1=1#anchor", url.toString());
        assertEquals("/xwiki/bin/view/Space/Page?param1=1#anchor",
            urlFactory.getURL(url, this.oldcore.getXWikiContext()));
    }

    /**
     * Verify that jsessionid is removed from URL.
     */
    @Test
    public void normalizeURL() throws MalformedURLException
    {
        assertEquals("http://www.xwiki.org/xwiki/bin/view/Blog/Bug+Fixing+Day+35?language=en",
            XWikiServletURLFactory
                .normalizeURL("http://www.xwiki.org/xwiki/bin/view/Blog/Bug+Fixing+Day+35"
                    + ";jsessionid=0AF95AFB8997826B936C0397DF6A0C7F?language=en", this.oldcore.getXWikiContext())
                .toString());
    }

    @Test
    public void createURLWithNestedSpaces()
    {
        URL url = this.urlFactory.createURL("Space1.Space2", "Page", this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/bin/view/Space1/Space2/Page", url.toString());
    }

    /**
     * Check that if the attachment cannot be found a URL is still created with the right schema.
     */
    @Test
    public void createAttachmentURLFileNotAvailable()
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        xwikiContext.setDoc(new XWikiDocument(new DocumentReference("xwiki", "currentspace", "currentpage")));
        URL url =
            this.urlFactory.createAttachmentURL("file", "currentspace", "currentpage", "download", null, xwikiContext);
        assertEquals("http://127.0.0.1/xwiki/bin/download/currentspace/currentpage/file", url.toString());
    }

    /**
     * Check that the version of the attachment is looked for to create the URL
     */
    @Test
    public void createAttachmentURLFindRev()
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        XWikiDocument doc = new XWikiDocument(new DocumentReference("xwiki", "currentspace", "currentpage"));
        XWikiAttachment attachment = new XWikiAttachment(doc, "file");
        attachment.setVersion("1.3");
        doc.setAttachment(attachment);
        xwikiContext.setDoc(doc);
        URL url =
            this.urlFactory.createAttachmentURL("file", "currentspace", "currentpage", "download", null, xwikiContext);
        assertEquals("http://127.0.0.1/xwiki/bin/download/currentspace/currentpage/file?rev=1.3", url.toString());
    }

    /**
     * Checked that the nested spaces are correctly resolved for creating the URL
     */
    @Test
    public void createAttachmentURLFindRevNestedSpace()
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        XWikiDocument doc = new XWikiDocument(
            new DocumentReference("xwiki", Arrays.asList("currentspace", "nestedspace"), "currentpage"));
        XWikiAttachment attachment = new XWikiAttachment(doc, "file");
        attachment.setVersion("1.3");
        doc.setAttachment(attachment);
        xwikiContext.setDoc(doc);
        URL url = this.urlFactory.createAttachmentURL("file", "currentspace.nestedspace", "currentpage", "download",
            null, xwikiContext);
        assertEquals("http://127.0.0.1/xwiki/bin/download/currentspace/nestedspace/currentpage/file?rev=1.3",
            url.toString());
    }

    /**
     * Checked that the context doc is taken into account for finding the attachment to create the URL
     */
    @Test
    public void createAttachmentURLFindRevAnotherContextDoc() throws XWikiException, WikiManagerException
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        XWikiDocument contextDoc = new XWikiDocument(new DocumentReference("xwiki", "currentspace", "currentpage"));
        XWikiAttachment attachment = new XWikiAttachment(contextDoc, "file");
        attachment.setVersion("1.3");
        contextDoc.setAttachment(attachment);
        xwikiContext.setDoc(contextDoc);

        DocumentReference documentReference =
            new DocumentReference("anotherwiki", Arrays.asList("anotherspace", "nestedspace"), "anotherpage");
        XWikiDocument doc = new XWikiDocument(documentReference);
        attachment = new XWikiAttachment(doc, "anotherfile");
        attachment.setVersion("2.1");
        doc.setAttachment(attachment);

        when(xwikiContext.getWiki().getDocument(documentReference, xwikiContext)).thenReturn(doc);
        when(this.descriptorManager.getById("anotherwiki"))
            .thenReturn(new WikiDescriptor("anotherwiki", "anotherwiki"));

        URL url = this.urlFactory.createAttachmentURL("anotherfile", "anotherspace.nestedspace", "anotherpage",
            "download", null, "anotherwiki", xwikiContext);

        assertEquals("http://127.0.0.1/xwiki/wiki/anotherwiki/download/anotherspace/nestedspace/anotherpage/"
            + "anotherfile?rev=2.1", url.toString());
        // Ensure the reference is back at the end.
        assertEquals(new WikiReference("xwiki"), xwikiContext.getWikiReference());
    }

    /**
     * Checked that the translation is not mixed up with the document for getting the URL
     */
    @Test
    public void createAttachmentURLFindRevNestedSpaceTranslation() throws XWikiException
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        XWikiDocument contextDoc = new XWikiDocument(
            new DocumentReference("xwiki", Arrays.asList("currentspace", "nestedspace"), "translatedpage"),
            Locale.FRENCH);
        xwikiContext.setDoc(contextDoc);

        DocumentReference documentReference =
            new DocumentReference("xwiki", Arrays.asList("currentspace", "nestedspace"), "translatedpage");
        XWikiDocument doc = new XWikiDocument(documentReference);
        XWikiAttachment attachment = new XWikiAttachment(contextDoc, "file");
        attachment.setVersion("1.3");
        doc.setAttachment(attachment);
        when(xwikiContext.getWiki().getDocument(documentReference, xwikiContext)).thenReturn(doc);
        URL url = this.urlFactory.createAttachmentURL("file", "currentspace.nestedspace", "translatedpage", "download",
            null, xwikiContext);
        assertEquals("http://127.0.0.1/xwiki/bin/download/currentspace/nestedspace/translatedpage/file?rev=1.3",
            url.toString());
    }

    @Test
    public void createAttachmentURLWhenViewRevAndRevSpecifiedAndIsNotContextDoc()
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        xwikiContext.put("rev", "1.0");
        xwikiContext.setDoc(new XWikiDocument(new DocumentReference("currentwiki", "currentspace", "currentpage")));
        URL url = this.urlFactory.createAttachmentURL("file", "space", "page", "download", null, xwikiContext);
        assertEquals("http://127.0.0.1/xwiki/bin/download/space/page/file", url.toString());
    }

    @Test
    public void createAttachmentURLWhenViewRevAndRevSpecifiedAndIsContextDocAndAttachmentDoesntExist()
        throws XWikiException
    {
        XWikiContext xwikiContext = this.oldcore.getXWikiContext();
        xwikiContext.put("rev", "1.0");

        XWikiDocument document = new XWikiDocument(new DocumentReference("currentwiki", "currentspace", "currentpage"));
        this.oldcore.getSpyXWiki().saveDocument(document, this.oldcore.getXWikiContext());

        xwikiContext.setDoc(document);
        xwikiContext.setWikiId("currentwiki");

        URL url =
            this.urlFactory.createAttachmentURL("file", "currentspace", "currentpage", "download", null, xwikiContext);
        assertEquals("http://127.0.0.1/xwiki/bin/viewattachrev/currentspace/currentpage/file", url.toString());
    }

    @Test
    public void createURLWhenShowViewActionFalse()
    {
        doReturn(false).when(this.oldcore.getSpyXWiki()).showViewAction(any(XWikiContext.class));

        URL url = this.urlFactory.createURL("Space", "Page", "view", this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/bin/Space/Page", url.toString());
    }

    @Test
    public void createURLWhenShowViewActionFalseAndSpaceIsNamedAfterAnAction()
    {
        doReturn(false).when(this.oldcore.getSpyXWiki()).showViewAction(any(XWikiContext.class));

        URL url = this.urlFactory.createURL("view.space2", "page", "view", this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/bin/view/view/space2/page", url.toString());
    }

    @Test
    public void createResourceURL()
    {
        // Verify that the URL factory encodes each path segment.
        URL url = this.urlFactory.createResourceURL("o;ne/t?w&o/t=hr#e e", false, this.oldcore.getXWikiContext());
        assertEquals("http://127.0.0.1/xwiki/resources/o;ne/t%3Fw&o/t=hr%23e%20e", url.toString());

        Map<String, Object> queryParametersMap = new LinkedHashMap<>();
        queryParametersMap.put("cache-version", "11.1-SNAPSHOT#");
        queryParametersMap.put("anArrayOfInt", new int[] {42, 56});
        queryParametersMap.put("aListWithStringToEncode", Arrays.asList("épervier", "androïde"));
        queryParametersMap.put("aCustomObject", new Object()
        {
            @Override
            public String toString()
            {
                return "foo";
            }
        });
        queryParametersMap.put("paramètre", new String[] {"foo", "bâr"});

        url = this.urlFactory.createResourceURL("o;ne/t?w&o/t=hr#e e", false, this.oldcore.getXWikiContext(),
            queryParametersMap);
        assertEquals("http://127.0.0.1/xwiki/resources/o;ne/t%3Fw&o/t=hr%23e%20e?cache-version=11.1-SNAPSHOT%23"
            + "&anArrayOfInt=42&anArrayOfInt=56"
            + "&aListWithStringToEncode=%C3%A9pervier&aListWithStringToEncode=andro%C3%AFde" + "&aCustomObject=foo"
            + "&param%C3%A8tre=foo&param%C3%A8tre=b%C3%A2r", url.toString());
    }

    @Test
    public void createURLWhenCharactersNeedToBeEncoded() throws Exception
    {
        // Note: The query string is not encoded, and used as is. It's the responsibility of the caller to
        // url-encode it.
        // See XWikiServletURLFactory#encodeWithinPath() and XWikiServletURLFactory#encodeWithinQuery() for explanations
        // Note: We also verify that the single quote is encoded since otherwise that could cause problems in HTML when
        // the returned URL is used in the HREF attribute of a A tag (when using single quote delimiters).
        URL url = this.urlFactory.createURL("a b.c+d'", "e f", "view", "g=h%20i", "j+!$&'()*,;=:@/?k|",
            this.oldcore.getXWikiContext());

        // First check if the returned URL is a valid URI.
        url.toURI();

        // Notice that the following characters are allowed in the fragment: +!$&()*,;=:@/? (so they are not encoded).
        // Single quote (apostrophe) is technically also allowed but we encode it in order to avoid breaking HTML links.
        assertEquals("http://127.0.0.1/xwiki/bin/view/a%20b/c%2Bd%27/e%20f?g=h%20i#j+!$&%27()*,;=:@/?k%7C",
            url.toString());
    }
}
