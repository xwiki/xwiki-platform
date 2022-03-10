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

import java.util.Arrays;

import org.junit.jupiter.api.Test;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.internal.StreamEventSearchResult;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.notifiers.internal.email.IntervalUsersManager;
import org.xwiki.notifications.preferences.NotificationEmailInterval;
import org.xwiki.query.QueryException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link MissingLiveNotificationMailsJob}.
 * 
 * @version $Id$
 */
@ComponentTest
public class MissingLiveNotificationMailsJobTest
{
    @InjectMockComponents
    private MissingLiveNotificationMailsJob job;

    @MockComponent
    private PrefilteringLiveNotificationEmailDispatcher dispatcher;

    @MockComponent
    private EventStore eventStore;

    @MockComponent
    private EntityReferenceSerializer<String> referenceSerializer;

    @MockComponent
    private IntervalUsersManager intervals;

    @Test
    void run() throws QueryException, EventStreamException
    {
        DocumentReference user1 = new DocumentReference("wiki", "space", "user1");
        DocumentReference user2 = new DocumentReference("wiki", "space", "user2");

        when(this.intervals.getUsers(NotificationEmailInterval.LIVE, "wiki")).thenReturn(Arrays.asList(user1, user2));

        when(this.referenceSerializer.serialize(user1)).thenReturn("wiki:space:user1");
        when(this.referenceSerializer.serialize(user2)).thenReturn("wiki:space:user2");

        Event eventa = new DefaultEvent();
        eventa.setId("a");
        Event eventb = new DefaultEvent();
        eventb.setId("b");
        Event eventc = new DefaultEvent();
        eventc.setId("c");

        SimpleEventQuery query1 = new SimpleEventQuery();
        query1.withMail("wiki:space:user1");
        when(this.eventStore.search(query1))
            .thenReturn(new StreamEventSearchResult(2, 0, 2, Arrays.asList(eventa, eventb).stream()));

        SimpleEventQuery query2 = new SimpleEventQuery();
        query2.withMail("wiki:space:user2");
        when(this.eventStore.search(query2))
            .thenReturn(new StreamEventSearchResult(2, 0, 2, Arrays.asList(eventa, eventc).stream()));

        MissingLiveNotificationMailsRequest request = new MissingLiveNotificationMailsRequest("wiki");
        request.setVerbose(false);

        this.job.initialize(request);
        this.job.run();

        verify(this.dispatcher, times(4)).addEvent(any(), any());
        verify(this.dispatcher).addEvent(eventa, user1);
        verify(this.dispatcher).addEvent(eventb, user1);
        verify(this.dispatcher).addEvent(eventa, user2);
        verify(this.dispatcher).addEvent(eventc, user2);
    }
}
