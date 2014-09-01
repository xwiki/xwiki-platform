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

import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StandardURLResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class StandardURLResourceReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<StandardURLResourceReferenceResolver> mocker =
        new MockitoComponentMockingRule(StandardURLResourceReferenceResolver.class);

    @Test
    public void resolveWhenBinPathSegment() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/bin/view/Space/Page");
        Map<String, Object> parameters = Collections.singletonMap("ignorePrefix", (Object) "xwiki");

        ResourceReferenceResolver resolver = mock(ResourceReferenceResolver.class);
        ExtendedURL extendedURL = new ExtendedURL(url, "xwiki");

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/bin")).thenReturn(resolver);

        this.mocker.getComponentUnderTest().resolve(url, parameters);

        // Verify the Entity URL Reference Resolver is called and with the proper parameters
        verify(resolver).resolve(eq(extendedURL), eq(parameters));
    }

    @Test
    @Ignore("Put back when StandardURLResourceReferenceResolver.resolve() has its TODO/FIXME removed")
    public void resolveWhenNoMatchingResolver() throws Exception
    {
        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/unknown")).thenThrow(new ComponentLookupException("error"));
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "unknown")).thenThrow(new ComponentLookupException("error"));

        URL url = new URL("http://localhost:8080/xwiki/unknown");
        Map<String, Object> parameters = Collections.singletonMap("ignorePrefix", (Object) "xwiki");

        try {
            this.mocker.getComponentUnderTest().resolve(url, parameters);
            fail("Should have thrown an exception here");
        } catch (UnsupportedResourceReferenceException expected) {
            assertEquals("Failed to find a Resolver for Resource Reference of type [unknown] for URL "
                + "[http://localhost:8080/xwiki/unknown]", expected.getMessage());
        }
    }
}
