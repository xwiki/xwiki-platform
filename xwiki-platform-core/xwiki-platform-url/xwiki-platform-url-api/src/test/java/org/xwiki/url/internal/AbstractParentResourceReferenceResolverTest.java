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
package org.xwiki.url.internal;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Arrays;

import javax.inject.Named;
import javax.inject.Singleton;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.xwiki.component.annotation.Component;
import org.xwiki.resource.CreateResourceReferenceException;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * @version $Id$
 */
@ComponentTest
class AbstractParentResourceReferenceResolverTest
{
    @Component
    @Named("test")
    @Singleton
    public static class TestParentResourceReferenceResolver extends AbstractParentResourceReferenceResolver
    {

    }

    @InjectMockComponents
    private TestParentResourceReferenceResolver resolver;

    @Test
    void resolve() throws MalformedURLException, CreateResourceReferenceException, UnsupportedResourceReferenceException
    {
        ExtendedURL extendedURL = new ExtendedURL(
            new URL("http://host/xwiki/test/child/1/2/3?key1=value11&key1=value12&key2=value2"), "xwiki/test");
        ResourceType type = new ResourceType("test");

        ParentResourceReference reference = this.resolver.resolve(extendedURL, type, null);

        assertSame(type, reference.getType());
        Assertions.assertEquals("child", reference.getChild());
        Assertions.assertEquals("child/1/2/3", reference.getRootPath());
        Assertions.assertEquals(Arrays.asList("1", "2", "3"), reference.getPathSegments());
        Assertions.assertEquals(2, reference.getParameters().size());
        Assertions.assertEquals(Arrays.asList("value11", "value12"), reference.getParameters().get("key1"));
        Assertions.assertEquals(Arrays.asList("value2"), reference.getParameters().get("key2"));
    }
}
