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

import javax.inject.Named;
import javax.servlet.ServletContext;

import org.junit.jupiter.api.Test;
import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ContextExtendedURLURLNormalizer}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
public class ContextExtendedURLURLNormalizerTest
{
    @InjectMockComponents
    private ContextExtendedURLURLNormalizer normalizer;

    @MockComponent
    @Named("xwikicfg")
    private ConfigurationSource configurationSource;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private ServletEnvironment environment;

    @BeforeComponent
    public void before() throws Exception
    {
        // Register an Environment component that is of type ServletEnvironment so that the instanceof in the code
        // works. We do this before the component's @Inject are mocked so that our mock is used.
        this.environment = mock(ServletEnvironment.class);
        this.componentManager.registerComponent(Environment.class, this.environment);
    }

    @Test
    void normalizeWhenConfigurationPropertyDefined()
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.normalizer.normalize(extendedURL).serialize());
    }

    @Test
    void normalizeWhenConfigurationPropertyDefinedButWithLeadingAndTrailingSlash()
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("/xwiki/");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.normalizer.normalize(extendedURL).serialize());
    }

    @Test
    void normalizeWhenNoConfigurationPropertyButEnvironment() throws Exception
    {
        ServletContext sc = mock(ServletContext.class);
        when(this.environment.getServletContext()).thenReturn(sc);
        when(sc.getContextPath()).thenReturn("/xwiki");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/xwiki/one/two", this.normalizer.normalize(extendedURL).serialize());
    }

    @Test
    void normalizeWhenNoConfigurationPropertyButEnvironmentWithRootContext() throws Exception
    {
        ServletContext sc = mock(ServletContext.class);
        when(this.environment.getServletContext()).thenReturn(sc);
        when(sc.getContextPath()).thenReturn("");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/one/two", this.normalizer.normalize(extendedURL).serialize());
    }

    @BeforeComponent("normalizeWhenNoConfigurationPropertyAndNoServletEnvironment")
    public void beforeNormalizeWhenNoConfigurationPropertyAndNoServletEnvironment() throws Exception
    {
        // We need to have the environment component not be a ServletEnvironment, thus we inject a mock one to override
        this.componentManager.registerMockComponent(Environment.class);
    }

    @Test
    void normalizeWhenNoConfigurationPropertyAndNoServletEnvironment()
    {
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        Throwable expected = assertThrows(RuntimeException.class, () -> this.normalizer.normalize(extendedURL));
        assertEquals("Failed to normalize the URL [/one/two] since the application's Servlet context couldn't be "
            + "computed.", expected.getMessage());
    }

    @Test
    void normalizeExtendedURLWithParameters()
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("xwiki");

        Map<String, List<String>> params = new HashMap<>();
        params.put("age", Arrays.asList("32"));
        params.put("colors", Arrays.asList("red", "blue"));

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"), params);
        assertSame(params, this.normalizer.normalize(extendedURL).getParameters());
    }

    @Test
    void normalizeWhenRootWebapp()
    {
        when(this.configurationSource.getProperty("xwiki.webapppath")).thenReturn("");

        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"));
        assertEquals("/one/two", this.normalizer.normalize(extendedURL).serialize());
    }
}
