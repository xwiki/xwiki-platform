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

import org.junit.Rule;
import org.junit.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.util.DefaultParameterizedType;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.ResourceReferenceHandler;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceType;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.same;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;

/**
 * Unit tests for {@link MainResourceReferenceHandlerManager}.
 *
 * @version $Id$
 * @since 6.1M2
 */
public class MainResourceReferenceHandlerManagerTest
{
    @Rule
    public MockitoComponentMockingRule<MainResourceReferenceHandlerManager> componentManager =
        new MockitoComponentMockingRule<>(MainResourceReferenceHandlerManager.class);

    @Test
    public void handleWithOrder() throws Exception
    {
        // First Handler component will lower priority
        ResourceReferenceHandler testHandler = mock(ResourceReferenceHandler.class, "handler1");
        when(testHandler.getSupportedResourceReferences()).thenReturn(Arrays.asList(new ResourceType("test")));

        // Second Handler component will higher priority so that it's executed first
        ResourceReferenceHandler beforeTestHandler = mock(ResourceReferenceHandler.class, "handler2");
        when(beforeTestHandler.getSupportedResourceReferences()).thenReturn(Arrays.asList(new ResourceType("test")));
        // We return 1 to mean that the second Handler has a higher priority than the first Handler
        when(beforeTestHandler.compareTo(testHandler)).thenReturn(1);

        ComponentManager contextComponentManager = this.componentManager.getInstance(ComponentManager.class, "context");
        when(contextComponentManager.<ResourceReferenceHandler>getInstanceList(
            new DefaultParameterizedType(null, ResourceReferenceHandler.class, ResourceType.class)))
                .thenReturn(Arrays.asList(testHandler, beforeTestHandler));

        ResourceReference reference = mock(ResourceReference.class);
        when(reference.getType()).thenReturn(new ResourceType("test"));

        this.componentManager.getComponentUnderTest().handle(reference);

        // Verify that the second Action is called (since it has a higher priority).
        verify(beforeTestHandler).handle(same(reference), any(ResourceReferenceHandlerChain.class));
    }
}
