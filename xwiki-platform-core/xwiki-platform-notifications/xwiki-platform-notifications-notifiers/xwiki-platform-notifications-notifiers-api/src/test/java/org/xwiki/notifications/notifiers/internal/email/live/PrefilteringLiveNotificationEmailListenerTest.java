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

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.EntityEvent;
import org.xwiki.eventstream.events.MailEntityAddedEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.observation.remote.RemoteObservationManagerContext;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link PrefilteringLiveNotificationEmailListener}.
 * 
 * @version $Id$
 */
@ComponentTest
class PrefilteringLiveNotificationEmailListenerTest
{
    private static final MailEntityAddedEvent EVENT = new MailEntityAddedEvent();

    @InjectMockComponents
    private PrefilteringLiveNotificationEmailListener listener;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    private RemoteObservationManagerContext remoteState;

    @MockComponent
    private PrefilteringLiveNotificationEmailManager manager;

    @Test
    void onEvent()
    {
        EntityEvent event = mock(EntityEvent.class);

        when(this.remoteState.isRemoteState()).thenReturn(false);
        when(this.notificationConfiguration.isEnabled()).thenReturn(true);
        when(this.notificationConfiguration.areEmailsEnabled()).thenReturn(true);

        this.listener.onEvent(EVENT, event, null);
        verify(this.manager, times(1)).addEvent(event);

        when(this.remoteState.isRemoteState()).thenReturn(true);
        when(this.notificationConfiguration.isEnabled()).thenReturn(true);
        when(this.notificationConfiguration.areEmailsEnabled()).thenReturn(true);

        this.listener.onEvent(EVENT, event, null);
        verify(this.manager, times(1)).addEvent(event);

        when(this.remoteState.isRemoteState()).thenReturn(false);
        when(this.notificationConfiguration.isEnabled()).thenReturn(false);
        when(this.notificationConfiguration.areEmailsEnabled()).thenReturn(true);

        this.listener.onEvent(EVENT, event, null);
        verify(this.manager, times(1)).addEvent(event);

        when(this.remoteState.isRemoteState()).thenReturn(false);
        when(this.notificationConfiguration.isEnabled()).thenReturn(true);
        when(this.notificationConfiguration.areEmailsEnabled()).thenReturn(false);

        this.listener.onEvent(EVENT, event, null);
        verify(this.manager, times(1)).addEvent(event);
    }
}
