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
package org.xwiki.notifications.notifiers.internal.email.live;

import javax.inject.Named;

import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.internal.DefaultEntityEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.notifications.notifiers.internal.email.IntervalUsersManager;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link PrefilteringLiveNotificationEmailManager}.
 * 
 * @version $Id$
 */
@ComponentTest
public class PrefilteringLiveNotificationEmailManagerTest
{
    @InjectMockComponents
    private PrefilteringLiveNotificationEmailManager manager;

    @MockComponent
    private PrefilteringLiveNotificationEmailDispatcher dispatcher;

    @MockComponent
    private DocumentReferenceResolver<String> resolver;

    @MockComponent
    private IntervalUsersManager intervals;

    @MockComponent
    private Execution execution;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @BeforeComponent
    void beforeComponent() throws ComponentLookupException
    {
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        Execution execution = mock(Execution.class);

        when(this.componentManager.getInstance(ExecutionContextManager.class)).thenReturn(executionContextManager);
        when(this.componentManager.getInstance(Execution.class)).thenReturn(execution);
    }

    @Test
    void addEvent() throws InterruptedException
    {
        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");
        when(this.resolver.resolve("entity", null)).thenReturn(userReference);

        Event event = mock(Event.class);
        EntityEvent entityEvent = new DefaultEntityEvent(event, "entity");

        when(this.intervals.getInterval(userReference)).thenReturn(NotificationEmailInterval.LIVE);

        this.manager.addEvent(entityEvent);

        Thread.sleep(100);

        verify(this.dispatcher).addEvent(event, userReference);

        when(this.intervals.getInterval(userReference)).thenReturn(NotificationEmailInterval.DAILY);

        this.manager.addEvent(entityEvent);

        Thread.sleep(100);

        verify(this.dispatcher).addEvent(event, userReference);
    }
}
