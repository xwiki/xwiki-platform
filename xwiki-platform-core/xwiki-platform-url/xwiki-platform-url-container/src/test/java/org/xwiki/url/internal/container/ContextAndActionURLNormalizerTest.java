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
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;

import org.junit.jupiter.api.Test;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.environment.Environment;
import org.xwiki.environment.internal.ServletEnvironment;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
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
 * Unit tests for {@link ContextAndActionURLNormalizer}.
 *
 * @version $Id$
 * @since 7.4M1
 */
@ComponentTest
public class ContextAndActionURLNormalizerTest
{
    @InjectMockComponents
    private ContextAndActionURLNormalizer normalizer;

    @MockComponent
    private Container container;

    private ServletEnvironment environment;

    private ServletContext servletContext;

    private ExtendedURL testURL = new ExtendedURL(Arrays.asList("one", "two"));

    @BeforeComponent
    public void before(MockitoComponentManager componentManager) throws Exception
    {
        this.environment = mock(ServletEnvironment.class);
        componentManager.registerComponent(Environment.class, this.environment);

        this.servletContext = mock(ServletContext.class);
        when(this.environment.getServletContext()).thenReturn(this.servletContext);

        ServletRegistration sr = mock(ServletRegistration.class);
        when(this.servletContext.getServletRegistration("action")).thenReturn(sr);
        when(sr.getMappings()).thenReturn(Arrays.asList("/bin/*", "/wiki/*", "/testbin/*"));
    }

    @Test
    void normalizeWithNoKnownContextPathThrowsException()
    {
        Throwable expected = assertThrows(RuntimeException.class, () -> this.normalizer.normalize(this.testURL));
        assertEquals("Failed to normalize the URL [/one/two] since the application's Servlet context couldn't be "
            + "computed.", expected.getMessage());
    }

    @Test
    void normalizeUsesServletContext()
    {
        when(this.servletContext.getContextPath()).thenReturn("/xwiki");

        assertEquals("/xwiki/bin/one/two", this.normalizer.normalize(this.testURL).serialize());
    }

    @Test
    void normalizeWithRootServletContext()
    {
        when(this.servletContext.getContextPath()).thenReturn("");

        assertEquals("/bin/one/two", this.normalizer.normalize(this.testURL).serialize());
    }

    @Test
    void normalizeFromAlternateServletMappingPreservesServletMapping()
    {
        when(this.servletContext.getContextPath()).thenReturn("/xwiki");

        HttpServletRequest req = createMockRequest();
        when(req.getServletPath()).thenReturn("/testbin");

        assertEquals("/xwiki/testbin/one/two", this.normalizer.normalize(this.testURL).serialize());
    }

    @BeforeComponent("normalizeFromRootServletContextAndServletMapping")
    public void beforeNormalizeFromRootServletContextAndServletMapping()
    {
        when(this.servletContext.getServletRegistration("action").getMappings())
            .thenReturn(Arrays.asList("/*", "/bin/*", "/wiki/*", "/testbin/*"));
    }

    @Test
    void normalizeFromRootServletContextAndServletMapping()
    {
        when(this.servletContext.getContextPath()).thenReturn("/");

        HttpServletRequest req = createMockRequest();
        when(req.getServletPath()).thenReturn("/");

        assertEquals("/one/two", this.normalizer.normalize(this.testURL).serialize());
    }

    @Test
    void normalizeFromNonActionRequestUsesDefaultServletMapping()
    {
        when(this.servletContext.getContextPath()).thenReturn("/xwiki");

        HttpServletRequest req = createMockRequest();
        when(req.getServletPath()).thenReturn("/rest");

        assertEquals("/xwiki/bin/one/two", this.normalizer.normalize(this.testURL).serialize());
    }

    @Test
    void normalizePreservesParameters()
    {
        when(this.servletContext.getContextPath()).thenReturn("/xwiki");

        Map<String, List<String>> params = new HashMap<>();
        params.put("age", Arrays.asList("32"));
        params.put("colors", Arrays.asList("red", "blue"));
        ExtendedURL extendedURL = new ExtendedURL(Arrays.asList("one", "two"), params);

        assertSame(params, this.normalizer.normalize(extendedURL).getParameters());
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
