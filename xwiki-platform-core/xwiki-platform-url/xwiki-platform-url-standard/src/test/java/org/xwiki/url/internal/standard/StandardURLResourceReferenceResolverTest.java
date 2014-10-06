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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.entity.EntityResourceReference;
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

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/bin")).thenReturn(resolver);

        this.mocker.registerMockComponent(WikiReferenceExtractor.class, "domain");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_REFERENCE);

        this.mocker.getComponentUnderTest().resolve(url, parameters);

        // Verify that the Entity URL Reference Resolver is called and with the proper parameters
        ExtendedURL expectedExtendedURL = new ExtendedURL(url, "xwiki");
        expectedExtendedURL.getSegments().remove(0);
        verify(resolver).resolve(eq(expectedExtendedURL), eq(parameters));
    }

    @Test
    public void resolveWhenWikiPathSegment() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/wiki/testwiki/view/Space/Page");
        Map<String, Object> parameters = Collections.singletonMap("ignorePrefix", (Object) "xwiki");

        ResourceReferenceResolver resolver = mock(ResourceReferenceResolver.class);

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/wiki")).thenReturn(resolver);

        this.mocker.registerMockComponent(WikiReferenceExtractor.class, "domain");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_REFERENCE);

        this.mocker.getComponentUnderTest().resolve(url, parameters);

        // Verify that the Entity URL Reference Resolver is called and with the proper parameters
        ExtendedURL expectedExtendedURL = new ExtendedURL(url, "xwiki");
        expectedExtendedURL.getSegments().remove(0);
        verify(resolver).resolve(eq(expectedExtendedURL), eq(parameters));
    }

    @Test
    public void resolveWhenNoMatchingResolver() throws Exception
    {
        // Throw an exception when looking for a specific resource type resolver.
        ComponentManager contextComponentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(contextComponentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/unknown")).thenThrow(new ComponentLookupException("error"));
        when(contextComponentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "unknown")).thenThrow(new ComponentLookupException("error"));

        // Set up a mock bin Entity Reference Resolver
        StandardURLConfiguration configuration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(configuration.getEntityPathPrefix()).thenReturn("binprefix");
        when(configuration.getWikiPathPrefix()).thenReturn("wikiprefix");
        this.mocker.registerMockComponent(WikiReferenceExtractor.class, "domain");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_REFERENCE);

        URL url = new URL("http://localhost:8080/xwiki/unknown");
        Map<String, Object> parameters = Collections.singletonMap("ignorePrefix", (Object) "xwiki");

        ResourceReference reference = this.mocker.getComponentUnderTest().resolve(url, parameters);

        assertEquals(EntityResourceReference.TYPE, reference.getType());
        assertEquals("view", ((EntityResourceReference) reference).getAction().toString());
    }
}
