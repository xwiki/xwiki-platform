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
package org.xwiki.resource.internal.entity;

import java.util.Collections;

import org.junit.jupiter.api.Test;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EntityResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 13.5RC1
 */
@ComponentTest
class EntityResourceReferenceHandlerTest
{
    @InjectMockComponents
    private EntityResourceReferenceHandler handler;

    @MockComponent
    private ResourceReferenceHandlerManager<EntityResourceAction> handlerManager;

    @Test
    void getSupportedResourceReferences()
    {
        assertEquals(Collections.singletonList(EntityResourceReference.TYPE), handler.getSupportedResourceReferences());
    }

    @Test
    void handle() throws ResourceReferenceHandlerException
    {
        EntityResourceReference resourceReference = mock(EntityResourceReference.class);
        ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);
        EntityResourceAction action = new EntityResourceAction("foo");
        when(resourceReference.getAction()).thenReturn(action);

        when(handlerManager.canHandle(action)).thenReturn(false);
        assertThrows(NotFoundResourceHandlerException.class, () -> {
            handler.handle(resourceReference, chain);
        });

        when(handlerManager.canHandle(action)).thenReturn(true);
        handler.handle(resourceReference, chain);
        verify(handlerManager).handle(resourceReference);
        verify(chain).handleNext(resourceReference);
        verify(handlerManager, times(2)).canHandle(action);
    }
}
