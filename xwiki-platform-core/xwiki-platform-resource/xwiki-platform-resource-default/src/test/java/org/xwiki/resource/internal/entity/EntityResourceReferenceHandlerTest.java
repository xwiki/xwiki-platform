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

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.resource.NotFoundResourceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerChain;
import org.xwiki.resource.ResourceReferenceHandlerException;
import org.xwiki.resource.ResourceReferenceHandlerManager;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectComponentManager;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link EntityResourceReferenceHandler}.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
@ComponentTest
class EntityResourceReferenceHandlerTest
{
    @InjectMockComponents
    private EntityResourceReferenceHandler handler;

    @MockComponent
    private ResourceReferenceHandlerManager<EntityResourceAction> handlerManager;

    @MockComponent
    private EntityResourceContextInitializer contextInitializer;

    @InjectComponentManager
    private MockitoComponentManager componentManager;

    private final EntityResourceReference resourceReference = mock(EntityResourceReference.class);

    private final ResourceReferenceHandlerChain chain = mock(ResourceReferenceHandlerChain.class);

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.contextInitializer.initialize(this.resourceReference)).thenReturn(true);
    }

    @Test
    void getSupportedResourceReferences()
    {
        assertEquals(List.of(EntityResourceReference.TYPE), this.handler.getSupportedResourceReferences());
    }

    @Test
    void handle() throws ResourceReferenceHandlerException
    {
        this.handler.handle(this.resourceReference, this.chain);

        verify(this.contextInitializer).initialize(this.resourceReference);
        verify(this.handlerManager).handle(this.resourceReference);
        verify(this.chain).handleNext(this.resourceReference);
    }

    @Test
    void handleWhenTargetIsNotInitialized() throws ResourceReferenceHandlerException
    {
        // The initializer has answered the request (e.g. with the initialization progress).
        when(this.contextInitializer.initialize(this.resourceReference)).thenReturn(false);

        this.handler.handle(this.resourceReference, this.chain);

        verifyNoInteractions(this.handlerManager, this.chain);
    }

    @Test
    void handleWithoutContextInitializer() throws Exception
    {
        this.componentManager.unregisterComponent(EntityResourceContextInitializer.class, "default");

        this.handler.handle(this.resourceReference, this.chain);

        verify(this.handlerManager).handle(this.resourceReference);
        verify(this.chain).handleNext(this.resourceReference);
    }

    @Test
    void handleWhenNoActionHandler() throws ResourceReferenceHandlerException
    {
        NotFoundResourceHandlerException exception = new NotFoundResourceHandlerException(this.resourceReference);
        doThrow(exception).when(this.handlerManager).handle(this.resourceReference);

        assertSame(exception, assertThrows(NotFoundResourceHandlerException.class,
            () -> this.handler.handle(this.resourceReference, this.chain)));
        verifyNoInteractions(this.chain);
    }
}
