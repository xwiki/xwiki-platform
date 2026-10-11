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
package org.xwiki.notifications.filters.internal.user;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ComponentTest
class OwnEventFilterTest
{
    @InjectMockComponents
    private OwnEventFilter filter;

    @MockComponent
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Event user1Event;

    private Event user2Event;

    private DocumentReference user1Reference = new DocumentReference("xwiki", "XWiki", "user1");

    private DocumentReference user2Reference = new DocumentReference("xwiki", "XWiki", "user2");

    @BeforeEach
    void beforeEach()
    {
        when(this.entityReferenceSerializer.serialize(this.user1Reference)).thenReturn("xwiki:XWiki.user1");
        when(this.entityReferenceSerializer.serialize(this.user2Reference)).thenReturn("xwiki:XWiki.user2");

        this.user1Event = mock(Event.class);
        when(this.user1Event.getUser()).thenReturn(this.user1Reference);

        this.user2Event = mock(Event.class);
        when(this.user2Event.getUser()).thenReturn(this.user2Reference);
    }

    @Test
    void filterEvent()
    {
        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT, this.filter.filterEvent(this.user1Event,
            this.user2Reference, List.of(), NotificationFormat.ALERT));

        assertEquals(NotificationFilter.FilterPolicy.FILTER, this.filter.filterEvent(this.user1Event,
            this.user1Reference, List.of(), NotificationFormat.ALERT));
    }

    @Test
    void filterTargetedSystemEvent()
    {
        when(this.user1Event.getTarget()).thenReturn(Set.of("user1", "user2"));

        assertEquals(NotificationFilter.FilterPolicy.NO_EFFECT, this.filter.filterEvent(this.user1Event,
            this.user1Reference, List.of(), NotificationFormat.ALERT));
    }
}
