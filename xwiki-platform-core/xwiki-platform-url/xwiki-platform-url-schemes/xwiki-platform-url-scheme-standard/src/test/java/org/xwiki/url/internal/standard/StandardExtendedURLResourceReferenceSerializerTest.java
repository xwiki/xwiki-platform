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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.AbstractResourceReference;
import org.xwiki.resource.ResourceReferenceSerializer;
import org.xwiki.resource.ResourceType;
import org.xwiki.resource.UnsupportedResourceReferenceException;
import org.xwiki.test.mockito.MockitoComponentMockingRule;
import org.xwiki.url.ExtendedURL;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link StandardExtendedURLResourceReferenceSerializer}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class StandardExtendedURLResourceReferenceSerializerTest
{
    @Rule
    public MockitoComponentMockingRule<StandardExtendedURLResourceReferenceSerializer> mocker =
        new MockitoComponentMockingRule<>(StandardExtendedURLResourceReferenceSerializer.class);

    public class TestResourceReference extends AbstractResourceReference
    {
        public TestResourceReference()
        {
            setType(new ResourceType("test"));
        }
    }

    @Test
    public void serialize() throws Exception
    {
        TestResourceReference resource = new TestResourceReference();

        ResourceReferenceSerializer serializer = mock(ResourceReferenceSerializer.class);

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
            TestResourceReference.class, ExtendedURL.class), "standard")).thenReturn(serializer);

        this.mocker.getComponentUnderTest().serialize(resource);

        // Verify the serializer is called and with the proper parameters
        verify(serializer).serialize(same(resource));
    }

    @Test
    public void serializeWhenNoMatchingSerializer() throws Exception
    {
        TestResourceReference resource = new TestResourceReference();

        ComponentManager componentManager = this.mocker.getInstance(ComponentManager.class, "context");
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
            TestResourceReference.class, ExtendedURL.class), "standard")).thenThrow(
                new ComponentLookupException("error"));
        when(componentManager.getInstance(new DefaultParameterizedType(null, ResourceReferenceSerializer.class,
            TestResourceReference.class, ExtendedURL.class))).thenThrow(
            new ComponentLookupException("error"));

        try {
            this.mocker.getComponentUnderTest().serialize(resource);
            fail("Should have thrown an exception here");
        } catch (UnsupportedResourceReferenceException expected) {
            assertEquals("Failed to find serializer for Resource Reference [type = [test], parameters = []] and "
                + "URL format [standard]", expected.getMessage());
        }
    }
}