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
package org.xwiki.notifications.page.events;

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.xwiki.bridge.event.ApplicationReadyEvent;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.SpaceReference;
import org.xwiki.notifications.internal.page.PageNotificationEventDescriptorManager;
import org.xwiki.test.mockito.MockitoComponentMockingRule;

import com.xpn.xwiki.internal.event.XObjectAddedEvent;
import com.xpn.xwiki.internal.event.XObjectDeletedEvent;
import com.xpn.xwiki.internal.event.XObjectEvent;
import com.xpn.xwiki.internal.event.XObjectUpdatedEvent;
import com.xpn.xwiki.objects.BaseObjectReference;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * @version $Id$
 */
public class PageNotificationEventListenerUpdaterTest
{
    @Rule
    public final MockitoComponentMockingRule<PageNotificationEventListenerUpdater> mocker =
            new MockitoComponentMockingRule<>(PageNotificationEventListenerUpdater.class);

    private PageNotificationEventDescriptorManager pageNotificationEventDescriptorManager;

    @Before
    public void setUp() throws Exception
    {
        pageNotificationEventDescriptorManager =
                mocker.registerMockComponent(PageNotificationEventDescriptorManager.class);
    }

    private void mockEventWithCorrectBaseObjectReference(XObjectEvent event)
    {
        BaseObjectReference objectReference = mock(BaseObjectReference.class);
        SpaceReference spaceReference = mock(SpaceReference.class);

        DocumentReference xClassReference = new DocumentReference("PageNotificationEventDescriptorClass",
                spaceReference);

        when(objectReference.getXClassReference()).thenReturn(xClassReference);
        when(event.getReference()).thenReturn(objectReference);
    }

    @Test
    public void onEventWithApplicationReadyEvent() throws Exception
    {
        mocker.getComponentUnderTest().onEvent(mock(ApplicationReadyEvent.class),
                mock(Object.class), mock(Object.class));

        verify(this.pageNotificationEventDescriptorManager, times(1)).updateDescriptors();
    }

    @Test
    public void onEventWithXObjectAddedEvent() throws Exception
    {
        XObjectAddedEvent event = mock(XObjectAddedEvent.class);
        this.mockEventWithCorrectBaseObjectReference(event);

        mocker.getComponentUnderTest().onEvent(event, mock(Object.class), mock(Object.class));

        verify(this.pageNotificationEventDescriptorManager, times(1)).updateDescriptors();
    }

    @Test
    public void onEventWithXObjectUpdatedEvent() throws Exception
    {
        XObjectUpdatedEvent event = mock(XObjectUpdatedEvent.class);
        this.mockEventWithCorrectBaseObjectReference(event);

        mocker.getComponentUnderTest().onEvent(event, mock(Object.class), mock(Object.class));

        verify(this.pageNotificationEventDescriptorManager, times(1)).updateDescriptors();
    }

    @Test
    public void onEventWithXObjectDeletedEvent() throws Exception
    {
        XObjectDeletedEvent event = mock(XObjectDeletedEvent.class);
        this.mockEventWithCorrectBaseObjectReference(event);

        mocker.getComponentUnderTest().onEvent(event, mock(Object.class), mock(Object.class));

        verify(this.pageNotificationEventDescriptorManager, times(1)).updateDescriptors();
    }
}
