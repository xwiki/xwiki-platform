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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.web.XWikiRequest;

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
    @Rule
    public MockitoComponentMockingRule<ExtendedURLURLNormalizer> mocker =
        new MockitoComponentMockingRule<>(ExtendedURLURLNormalizer.class);

    @Test
    public void normalizeWhenConfigurationPropertyDefined() throws Exception
    {
        ConfigurationSource configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        when(configurationSource.getProperty("xwiki.webapppath")).thenReturn("xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenConfigurationPropertyDefinedButWithLeadingAndTrailingSlash() throws Exception
    {
        ConfigurationSource configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        when(configurationSource.getProperty("xwiki.webapppath")).thenReturn("/xwiki/");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndRequest() throws Exception
    {
        ConfigurationSource configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        when(configurationSource.getProperty("xwiki.webapppath")).thenReturn(null);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);
        XWikiRequest xwikiRequest = mock(XWikiRequest.class);
        when(xwikiContext.getRequest()).thenReturn(xwikiRequest);
        when(xwikiRequest.getContextPath()).thenReturn("xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoRequestButURL() throws Exception
    {
        ConfigurationSource configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        when(configurationSource.getProperty("xwiki.webapppath")).thenReturn(null);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);
        when(xwikiContext.getRequest()).thenReturn(null);
        when(xwikiContext.getURL()).thenReturn(new URL("http://localhost:8080/xwiki/bin/view/space/page"));

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoRequestButURLWithNoTrailingSlash() throws Exception
    {
        ConfigurationSource configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        when(configurationSource.getProperty("xwiki.webapppath")).thenReturn(null);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);
        when(xwikiContext.getRequest()).thenReturn(null);
        when(xwikiContext.getURL()).thenReturn(new URL("http://localhost:8080/xwiki"));

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoRequestAndNoURL() throws Exception
    {
        ConfigurationSource configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        when(configurationSource.getProperty("xwiki.webapppath")).thenReturn(null);

        Execution execution = this.mocker.getInstance(Execution.class);
        ExecutionContext executionContext = mock(ExecutionContext.class);
        when(execution.getContext()).thenReturn(executionContext);
        XWikiContext xwikiContext = mock(XWikiContext.class);
        when(executionContext.getProperty("xwikicontext")).thenReturn(xwikiContext);
        when(xwikiContext.getRequest()).thenReturn(null);
        when(xwikiContext.getURL()).thenReturn(null);

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        try {
            this.mocker.getComponentUnderTest().normalize(extendedURL);
            fail("Should have thrown an exception");
        } catch (RuntimeException expected) {
            assertEquals("Failed to normalize the URL [one/two] since the application's Servlet context couldn't be "
                + "computed.", expected.getMessage());
        }
    }
}
