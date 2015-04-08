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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.resource.ResourceReferenceResolver;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StandardExtendedURLResourceReferenceResolver}.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class StandardExtendedURLResourceReferenceResolverTest
{
    @Rule
    public MockitoComponentMockingRule<StandardExtendedURLResourceReferenceResolver> mocker =
        new MockitoComponentMockingRule(StandardExtendedURLResourceReferenceResolver.class);

    @Test
    public void resolveWhenBinPathSegment() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/bin/view/Space/Page");
        ExtendedURL extendedURL = new ExtendedURL(url, "xwiki");
        extendedURL.getSegments().remove(0);

        ResourceReferenceResolver resolver = mock(ResourceReferenceResolver.class);

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/bin")).thenReturn(resolver);

        this.mocker.registerMockComponent(WikiReferenceExtractor.class, "domain");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_REFERENCE);

        this.mocker.getComponentUnderTest().resolve(extendedURL, new ResourceType("bin"),
            Collections.<String, Object>emptyMap());

        // Verify that the Entity URL Reference Resolver is called and with the proper parameters
        verify(resolver).resolve(eq(extendedURL), eq(new ResourceType("bin")), anyMap());
    }

    @Test
    public void resolveWhenWikiPathSegment() throws Exception
    {
        URL url = new URL("http://localhost:8080/xwiki/wiki/testwiki/view/Space/Page");
        ExtendedURL extendedURL = new ExtendedURL(url, "xwiki");
        extendedURL.getSegments().remove(0);

        ResourceReferenceResolver resolver = mock(ResourceReferenceResolver.class);

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/wiki")).thenReturn(resolver);

        this.mocker.registerMockComponent(WikiReferenceExtractor.class, "domain");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_REFERENCE);

        this.mocker.getComponentUnderTest().resolve(extendedURL, new ResourceType("wiki"),
            Collections.<String, Object>emptyMap());

        // Verify that the Entity URL Reference Resolver is called and with the proper parameters
        verify(resolver).resolve(eq(extendedURL), eq(new ResourceType("wiki")), anyMap());
    }

    @Test
    public void resolveWhenNoMatchingResolver() throws Exception
    {
        // Throw an exception when looking for a specific resource type resolver.
        ComponentManager contextComponentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(contextComponentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "standard/bin")).thenThrow(new ComponentLookupException("error"));
        when(contextComponentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceResolver.class,
            ExtendedURL.class), "bin")).thenThrow(new ComponentLookupException("error"));

        // Set up a mock bin Entity Reference Resolver
        StandardURLConfiguration configuration = this.mocker.getInstance(StandardURLConfiguration.class);
        when(configuration.getEntityPathPrefix()).thenReturn("binprefix");
        when(configuration.getWikiPathPrefix()).thenReturn("wikiprefix");
        this.mocker.registerMockComponent(WikiReferenceExtractor.class, "domain");
        this.mocker.registerMockComponent(EntityReferenceResolver.TYPE_REFERENCE);

        URL url = new URL("http://localhost:8080/xwiki/unknown");
        ExtendedURL extendedURL = new ExtendedURL(url, "xwiki");
        extendedURL.getSegments().remove(0);

        try {
            this.mocker.getComponentUnderTest().resolve(extendedURL, new ResourceType("bin"),
                Collections.<String, Object>emptyMap());
            fail("Should have thrown an exception here");
        } catch (UnsupportedResourceReferenceException expected) {
            assertEquals("Couldn't find any Resource Reference Resolver for type [bin]", expected.getMessage());
        }
    }
}
