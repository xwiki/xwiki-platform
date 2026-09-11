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
package org.xwiki.resource.internal;

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceType;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link MainResourceReferenceHandlerManager}.
 *
 * @version $Id$
 * @since 6.1M2
 */
@ComponentTest
class MainResourceReferenceHandlerManagerTest
{
    @InjectMockComponents
    private MainResourceReferenceHandlerManager handlerManager;

    @Mock(name = "handler1")
    private ResourceReferenceHandler<ResourceType> handler1;

    @Mock(name = "handler2")
    private ResourceReferenceHandler<ResourceType> handler2;

    @Test
    void handleWithOrder(ComponentManager componentManager) throws Exception
    {
        // First Handler component will lower priority
        when(this.handler1.getSupportedResourceReferences()).thenReturn(Arrays.asList(new ResourceType("test")));

        // Second Handler component will higher priority so that it's executed first
        when(this.handler2.getSupportedResourceReferences()).thenReturn(Arrays.asList(new ResourceType("test")));
        // We return -1 to mean that the second Handler has a higher priority than the first Handler
        when(this.handler2.compareTo(this.handler1)).thenReturn(-1);

        ComponentManager contextComponentManager = componentManager.getInstance(ComponentManager.class, "context");
        when(contextComponentManager.<ResourceReferenceHandler<?>>getInstanceList(
            new DefaultParameterizedType(null, ResourceReferenceHandler.class, ResourceType.class)))
                .thenReturn(Arrays.<ResourceReferenceHandler<?>>asList(this.handler1, this.handler2));

        ResourceReference reference = mock(ResourceReference.class);
        when(reference.getType()).thenReturn(new ResourceType("test"));

        handlerManager.handle(reference);

        // Verify that the second Action is called (since it has a higher priority).
        verify(this.handler2).handle(same(reference), any(ResourceReferenceHandlerChain.class));
    }

    @Test
    void matches()
    {
        ResourceType resourceType1 = new ResourceType("test1");
        ResourceType resourceType2 = new ResourceType("test2");

        when(this.handler1.getSupportedResourceReferences()).thenReturn(Arrays.asList(resourceType1));
        when(this.handler2.getSupportedResourceReferences())
            .thenReturn(Arrays.asList(resourceType1, resourceType2));

        assertTrue(this.handlerManager.matches(this.handler1, resourceType1));
        assertFalse(this.handlerManager.matches(this.handler1, resourceType2));

        assertTrue(this.handlerManager.matches(this.handler2, resourceType1));
        assertTrue(this.handlerManager.matches(this.handler2, resourceType2));
    }
}
