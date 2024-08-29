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
package org.xwiki.notifications.notifiers.internal;

import java.util.List;
import java.util.concurrent.Callable;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.namespace.NamespaceContextExecutor;
import org.xwiki.model.namespace.WikiNamespace;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.notifiers.NotificationDisplayer;
import org.xwiki.rendering.block.Block;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeast;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultNotificationRenderer}
 *
 * @version $Id$
 * @since 15.5RC1
 */
@ComponentTest
class DefaultNotificationRendererTest
{
    @InjectMockComponents
    private DefaultNotificationRenderer notificationRenderer;

    @MockComponent
    private NamespaceContextExecutor namespaceContextExecutor;

    @MockComponent
    private NotificationDisplayer defaultDisplayer;

    @BeforeComponent
    void beforeComponent(MockitoComponentManager componentManager) throws Exception
    {
        componentManager.registerComponent(ComponentManager.class, "context", componentManager);
    }

    @Test
    void render(MockitoComponentManager componentManager) throws Exception
    {
        CompositeEvent fooEvent = mock(CompositeEvent.class);
        NotificationDisplayer fooDisplayer = componentManager.registerMockComponent(NotificationDisplayer.class, "foo");
        NotificationDisplayer barDisplayer = componentManager.registerMockComponent(NotificationDisplayer.class, "bar");

        when(fooDisplayer.getSupportedEvents()).thenReturn(List.of("foo"));
        when(barDisplayer.getSupportedEvents()).thenReturn(List.of("bar"));
        when(fooEvent.getType()).thenReturn("foo");

        Block fooBlock = mock(Block.class);
        when(fooDisplayer.renderNotification(fooEvent)).thenReturn(fooBlock);
        assertEquals(fooBlock, this.notificationRenderer.render(fooEvent));
        verifyNoInteractions(this.namespaceContextExecutor);

        CompositeEvent otherEvent = mock(CompositeEvent.class);
        when(otherEvent.getType()).thenReturn("other");
        when(otherEvent.getDocument()).thenReturn(new DocumentReference("subwiki", "SomeSpace", "SomeDoc"));

        Block otherBlock = mock(Block.class);
        when(defaultDisplayer.renderNotification(otherEvent)).thenReturn(otherBlock);
        when(namespaceContextExecutor.execute(any(), any())).then(invocationOnMock -> {
            Callable callable = invocationOnMock.getArgument(1);
            return callable.call();
        });
        assertEquals(otherBlock, this.notificationRenderer.render(otherEvent));
        verify(namespaceContextExecutor).execute(eq(new WikiNamespace("subwiki")), any());
        verify(barDisplayer, atLeast(1)).getSupportedEvents();
    }
}