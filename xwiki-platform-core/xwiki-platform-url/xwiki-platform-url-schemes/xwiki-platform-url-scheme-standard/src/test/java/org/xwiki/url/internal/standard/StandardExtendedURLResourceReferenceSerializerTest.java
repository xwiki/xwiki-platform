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

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.url.ExtendedURL;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.same;
import static org.mockito.Mockito.verify;

/**
 * Unit tests for {@link StandardExtendedURLResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class StandardExtendedURLResourceReferenceSerializerTest
{
    @InjectMockComponents
    private StandardExtendedURLResourceReferenceSerializer serializer;

    public static class TestResourceReference extends AbstractResourceReference
    {
        TestResourceReference()
        {
            setType(new ResourceType("test"));
        }
    }

    @BeforeComponent
    void setUp(MockitoComponentManager componentManager) throws Exception
    {
        // Register the real mockito component manager as the context component manager so we can easily mock
        // components on it. This needs to be done before the component to test is initialized.
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void serialize(MockitoComponentManager componentManager) throws Exception
    {
        TestResourceReference resource = new TestResourceReference();

        ResourceReferenceSerializer<TestResourceReference, ExtendedURL> testResourceReferenceSerializer =
            componentManager.registerMockComponent(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
                TestResourceReference.class, ExtendedURL.class), "standard");

        this.serializer.serialize(resource);

        // Verify the serializer is called and with the proper parameters
        verify(testResourceReferenceSerializer).serialize(same(resource));
    }

    @Test
    void serializeWithFallback(MockitoComponentManager componentManager) throws Exception
    {
        TestResourceReference resource = new TestResourceReference();

        // Register a resource reference serializer without name.
        ResourceReferenceSerializer<TestResourceReference, ExtendedURL> testResourceReferenceSerializer =
            componentManager.registerMockComponent(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
                TestResourceReference.class, ExtendedURL.class));

        this.serializer.serialize(resource);

        // Verify the serializer is called and with the proper parameters
        verify(testResourceReferenceSerializer).serialize(same(resource));
    }

    @Test
    void serializeWhenNoMatchingSerializer()
    {
        TestResourceReference resource = new TestResourceReference();

        Throwable exception = assertThrows(UnsupportedResourceReferenceException.class, () -> {
            this.serializer.serialize(resource);
        });
        assertEquals("Failed to find serializer for Resource Reference [type = [test], parameters = []] and "
            + "URL format [standard]", exception.getMessage());
    }
}
