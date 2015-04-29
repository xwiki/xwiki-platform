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
package org.xwiki.url.internal.container;

import java.net.URL;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Provider;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link org.xwiki.url.internal.container.ExtendedURLURLNormalizer}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class ExtendedURLURLNormalizerTest
{
    private ConfigurationSource configurationSource;

    private Container container;

    private XWikiContext xcontext = mock(XWikiContext.class);

    @Rule
    public MockitoComponentMockingRule<ExtendedURLURLNormalizer> mocker =
        new MockitoComponentMockingRule<>(ExtendedURLURLNormalizer.class);

    @Before
    public void configure() throws Exception
    {
        this.configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        this.container = this.mocker.getInstance(Container.class);

        Provider<XWikiContext> xcontextProvider = this.mocker.getInstance(XWikiContext.TYPE_PROVIDER);
        when(xcontextProvider.get()).thenReturn(xcontext);
    }

    @Test
    public void normalizeWhenConfigurationPropertyDefined() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenConfigurationPropertyDefinedButWithLeadingAndTrailingSlash() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("/xwiki/");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndRequest() throws Exception
    {
        HttpServletRequest request = createMockRequest();
        when(request.getContextPath()).thenReturn("/xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoRequestButURL() throws Exception
    {
        when(this.xcontext.getURL()).thenReturn(new URL("http://localhost:8080/xwiki/bin/view/space/page"));

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoRequestButURLWithNoTrailingSlash() throws Exception
    {
        when(this.xcontext.getURL()).thenReturn(new URL("http://localhost:8080/xwiki"));

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoRequestAndNoURL() throws Exception
    {
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        try {
            this.mocker.getComponentUnderTest().normalize(extendedURL);
            fail("Should have thrown an exception");
        } catch (RuntimeException expected) {
            assertEquals("Failed to normalize the URL [/one/two] since the application's Servlet context couldn't be "
                + "computed.", expected.getMessage());
        }
    }

    @Test
    public void normalizeExtendedURLWithParameters() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("xwiki");

        Map<String, List<String>> params = new HashMap<>();
        params.put("age", Arrays.asList("32"));
        params.put("colors", Arrays.asList("red", "blue"));

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"), params);
        assertSame(params, this.mocker.getComponentUnderTest().normalize(extendedURL).getParameters());
    }

    private HttpServletRequest createMockRequest()
    {
        ServletRequest request = mock(ServletRequest.class);
        when(this.container.getRequest()).thenReturn(request);

        HttpServletRequest httpRequest = mock(HttpServletRequest.class);
        when(request.getHttpServletRequest()).thenReturn(httpRequest);

        return httpRequest;
    }
}
