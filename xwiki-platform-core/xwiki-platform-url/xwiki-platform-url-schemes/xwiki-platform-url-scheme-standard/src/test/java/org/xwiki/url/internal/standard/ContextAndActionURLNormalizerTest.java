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
package org.xwiki.url.internal.standard;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.url.internal.stadard.ContextAndActionURLNormalizer}.
 *
 * @version $Id$
 * @since 7.4M1
 */
public class ContextAndActionURLNormalizerTest
{
    private ServletEnvironment environment;

    private Container container;

    private ServletContext servletContext;

    private ConfigurationSource xwikiCfg;

    private ModelContext modelContext;

    private StandardURLConfiguration urlConfiguration;

    private ExtendedURL testURL = new ExtendedURL(Arrays.asList("one", "two"));

    @Rule
    public MockitoComponentMockingRule<URLNormalizer<ExtendedURL>> mocker =
        new MockitoComponentMockingRule<URLNormalizer<ExtendedURL>>(ContextAndActionURLNormalizer.class);

    @Before
    public void configure() throws Exception
    {
        // Configure the super class
        this.container = this.mocker.getInstance(Container.class);
        this.environment = mock(ServletEnvironment.class);
        this.mocker.registerComponent(Environment.class, this.environment);
        this.servletContext = mock(ServletContext.class);
        when(this.environment.getServletContext()).thenReturn(this.servletContext);
        ServletRegistration sr = mock(ServletRegistration.class);
        when(this.servletContext.getServletRegistration("action")).thenReturn(sr);
        when(sr.getMappings()).thenReturn(Arrays.asList("/bin/*", "/wiki/*", "/testbin/*"));

        // Configure the tested class
        this.xwikiCfg = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
        this.modelContext = this.mocker.getInstance(ModelContext.class);
        this.urlConfiguration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(this.urlConfiguration.getEntityPathPrefix()).thenReturn("bin");
        when(this.urlConfiguration.getWikiPathPrefix()).thenReturn("wiki");
    }

    @Test
    public void normalizeWithNoKnownContextPathThrowsException() throws Exception
    {
        this.mocker.registerMockComponent(Environment.class);
        try {
            this.mocker.getComponentUnderTest().normalize(this.testURL);
            fail("Should have thrown an exception");
        } catch (RuntimeException expected) {
            assertEquals("Failed to normalize the URL [/one/two] since the application's Servlet context couldn't be "
                + "computed.", expected.getMessage());
        }
    }

    @Test
    public void normalizeUsesTheSpecifiedConfiguration() throws Exception
    {
        when(this.xwikiCfg.getProperty("xwiki.webapppath")).thenReturn("good");
        when(this.servletContext.getContextPath()).thenReturn("/bad");

        assertEquals("/good/bin/one/two", this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizeRemovesLeadingAndTrailingSlashFromConfiguration() throws Exception
    {
        when(this.xwikiCfg.getProperty("xwiki.webapppath")).thenReturn("/xwiki/");

        assertEquals("/xwiki/bin/one/two", this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizeWithRootConfiguration() throws Exception
    {
        when(this.xwikiCfg.getProperty("xwiki.webapppath")).thenReturn("");
        when(this.servletContext.getContextPath()).thenReturn("/bad");

        assertEquals("/bin/one/two", this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizeWithSlashRootConfiguration() throws Exception
    {
        when(this.xwikiCfg.getProperty("xwiki.webapppath")).thenReturn("/");
        when(this.servletContext.getContextPath()).thenReturn("/bad");

        assertEquals("/bin/one/two", this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizeWithNoConfigurationUsesServletContext() throws Exception
    {
        when(this.servletContext.getContextPath()).thenReturn("/xwiki");

        assertEquals("/xwiki/bin/one/two", this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizeWithNoConfigurationAndRootServletContext() throws Exception
    {
        when(this.servletContext.getContextPath()).thenReturn("");

        assertEquals("/bin/one/two", this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizeFromVirtualWikiRequestPreservesWikiPath() throws Exception
    {
        when(this.xwikiCfg.getProperty("xwiki.webapppath")).thenReturn("xwiki");

        HttpServletRequest req = createMockRequest();
        when(req.getServletPath()).thenReturn("/wiki");
        when(this.modelContext.getCurrentEntityReference()).thenReturn(new WikiReference("dev"));

        assertEquals("/xwiki/wiki/dev/one/two",
            this.mocker.getComponentUnderTest().normalize(this.testURL).serialize());
    }

    @Test
    public void normalizePreservesParameters() throws Exception
    {
        when(this.xwikiCfg.getProperty("xwiki.webapppath")).thenReturn("xwiki");

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
