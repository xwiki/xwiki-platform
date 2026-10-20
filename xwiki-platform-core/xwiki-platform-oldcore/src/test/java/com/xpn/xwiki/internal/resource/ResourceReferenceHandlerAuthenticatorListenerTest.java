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
package com.xpn.xwiki.internal.resource;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.resource.ResourceReference;
import org.xwiki.resource.annotations.Authenticate;
import org.xwiki.resource.entity.EntityResourceAction;
import org.xwiki.resource.entity.EntityResourceReference;
import org.xwiki.resource.events.ResourceReferenceHandlingEvent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiContextInitializer;
import com.xpn.xwiki.XWikiException;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ResourceReferenceHandlerAuthenticatorListener}.
 *
 * @version $Id$
 */
@ComponentTest
class ResourceReferenceHandlerAuthenticatorListenerTest
{
    @Authenticate
    private static final class AuthenticatedHandler
    {
    }

    @InjectMockComponents
    private ResourceReferenceHandlerAuthenticatorListener listener;

    @MockComponent
    private XWikiContextInitializer xcontextInitializer;

    @MockComponent
    private Execution execution;

    @Mock
    private XWikiContext xcontext;

    private final ExecutionContext econtext = new ExecutionContext();

    @BeforeEach
    void setUp() throws Exception
    {
        when(this.execution.getContext()).thenReturn(this.econtext);
        when(this.xcontextInitializer.authenticate()).thenReturn(this.xcontextInitializer);
        when(this.xcontextInitializer.initialize(this.econtext)).thenReturn(this.xcontext);
    }

    @Test
    void onEventWhenHandlerDoesNotRequireAuthentication()
    {
        this.listener.onEvent(new ResourceReferenceHandlingEvent(mock(ResourceReference.class)), new Object(), null);

        verifyNoInteractions(this.xcontextInitializer);
    }

    @Test
    void onEventWithEntityResourceReference() throws Exception
    {
        EntityResourceReference reference =
            new EntityResourceReference(new DocumentReference("wiki", "Space", "Page"), new EntityResourceAction("foo"));

        this.listener.onEvent(new ResourceReferenceHandlingEvent(reference), new AuthenticatedHandler(), null);

        verify(this.xcontextInitializer).initialize(this.econtext);
        verify(this.xcontext).setAction("foo");
    }

    @Test
    void onEventWithOtherResourceReference() throws Exception
    {
        ResourceReference reference = mock(ResourceReference.class);

        this.listener.onEvent(new ResourceReferenceHandlingEvent(reference), new AuthenticatedHandler(), null);

        verify(this.xcontextInitializer).initialize(this.econtext);
        verify(this.xcontext, never()).setAction(any());
    }

    @Test
    void onEventAuthenticatesOnlyOncePerExecutionContext() throws Exception
    {
        ResourceReferenceHandlingEvent event = new ResourceReferenceHandlingEvent(mock(ResourceReference.class));

        // Nested Handlers (e.g. the Entity Resource Reference Handler and the Handler of the action it calls) must not
        // authenticate the request twice.
        this.listener.onEvent(event, new AuthenticatedHandler(), null);
        this.listener.onEvent(event, new AuthenticatedHandler(), null);

        verify(this.xcontextInitializer, times(1)).initialize(this.econtext);
    }

    @Test
    void onEventWhenInitializationFails() throws Exception
    {
        when(this.xcontextInitializer.initialize(this.econtext)).thenThrow(new XWikiException());
        ResourceReferenceHandlingEvent event = new ResourceReferenceHandlingEvent(mock(ResourceReference.class));
        AuthenticatedHandler handler = new AuthenticatedHandler();

        assertThrows(RuntimeException.class, () -> this.listener.onEvent(event, handler, null));
    }
}
