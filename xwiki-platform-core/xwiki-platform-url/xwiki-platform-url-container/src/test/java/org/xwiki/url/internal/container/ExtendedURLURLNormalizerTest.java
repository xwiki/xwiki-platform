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

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.ServletContext;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;
import org.xwiki.url.URLNormalizer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link org.xwiki.url.internal.container.ExtendedURLURLNormalizer}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class ExtendedURLURLNormalizerTest
{
    private ConfigurationSource configurationSource;

    @Rule
    public MockitoComponentMockingRule<URLNormalizer<ExtendedURL>> mocker =
        new MockitoComponentMockingRule<URLNormalizer<ExtendedURL>>(ExtendedURLURLNormalizer.class);

    @Before
    public void configure() throws Exception
    {
        this.configurationSource = this.mocker.getInstance(ConfigurationSource.class, "xwikicfg");
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
    public void normalizeWhenNoConfigurationPropertyButEnvironment() throws Exception
    {
        ServletContext sc = mock(ServletContext.class);
        ServletEnvironment environment = mock(ServletEnvironment.class);
        this.mocker.registerComponent(Environment.class, environment);
        when(environment.getServletContext()).thenReturn(sc);
        when(sc.getContextPath()).thenReturn("/xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyButEnvironmentWithRootContext() throws Exception
    {
        ServletContext sc = mock(ServletContext.class);
        ServletEnvironment environment = mock(ServletEnvironment.class);
        this.mocker.registerComponent(Environment.class, environment);
        when(environment.getServletContext()).thenReturn(sc);
        when(sc.getContextPath()).thenReturn("");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }

    @Test
    public void normalizeWhenNoConfigurationPropertyAndNoServletEnvironment() throws Exception
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

    @Test
    public void normalizeWhenRootWebapp() throws Exception
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/one/two", this.mocker.getComponentUnderTest().normalize(extendedURL).serialize());
    }
}
