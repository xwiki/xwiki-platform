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

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import javax.inject.Named;

import org.apache.commons.lang3.reflect.FieldUtils;
import org.junit.jupiter.api.Test;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.ExecutionContextManager;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.internal.SimilarityCalculator;
import org.xwiki.test.annotation.BeforeComponent;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;

import com.xpn.xwiki.XWikiContext;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Validate {@link PrefilteringLiveNotificationEmailDispatcher}.
 * 
 * @version $Id$
 */
@ComponentTest
public class PrefilteringLiveNotificationEmailDispatcherTest
{
    @InjectMockComponents
    private PrefilteringLiveNotificationEmailDispatcher dispatcher;

    @MockComponent
    private SimilarityCalculator similarityCalculator;

    @MockComponent
    private PrefilteringLiveNotificationEmailSender sender;

    @MockComponent
    private NotificationConfiguration notificationConfiguration;

    @MockComponent
    @Named("context")
    private ComponentManager componentManager;

    @BeforeComponent
    void beforeComponent() throws ComponentLookupException
    {
        ExecutionContextManager executionContextManager = mock(ExecutionContextManager.class);
        when(this.componentManager.getInstance(ExecutionContextManager.class)).thenReturn(executionContextManager);
    }

    @Test
    void addEvent() throws IllegalAccessException, InterruptedException, NotificationException
    {
        // Force a very short grace period so that the test does not take 1 minute
        FieldUtils.writeField(this.dispatcher, "grace", 100, true);

        //////
        // One event

        DocumentReference userReference = new DocumentReference("wiki", "XWiki", "user");
        DefaultEvent event = new DefaultEvent();
        event.setId("id");
        event.setDate(new Date());

        this.dispatcher.addEvent(event, userReference);

        // Give enough time to wait for the grace period and process the event
        Thread.sleep(200);

        Map<DocumentReference, CompositeEvent> events = new HashMap<>();
        events.put(userReference, new CompositeEvent(event));

        verify(this.sender).sendMails(events);

        //////
        // One event with two users

        DocumentReference user2Reference = new DocumentReference("wiki", "XWiki", "user2");
        DefaultEvent sameevent = new DefaultEvent();
        sameevent.setId("id");
        sameevent.setDate(new Date());
        event.setDate(new Date());

        this.dispatcher.addEvent(event, userReference);
        this.dispatcher.addEvent(sameevent, user2Reference);

        // Give enough time to wait for the grace period and process the event
        Thread.sleep(200);

        events = new HashMap<>();
        events.put(userReference, new CompositeEvent(event));
        events.put(user2Reference, new CompositeEvent(event));

        verify(this.sender).sendMails(events);

        //////
        // Two similar events

        DefaultEvent similarevent = new DefaultEvent();
        similarevent.setId("similar");
        similarevent.setDate(new Date());
        DefaultEvent otherevent = new DefaultEvent();
        otherevent.setId("other");
        otherevent.setDate(new Date());
        event.setDate(new Date());

        when(this.similarityCalculator.computeSimilarity(event, similarevent))
            .thenReturn(SimilarityCalculator.SAME_DOCUMENT_AND_TYPE);

        this.dispatcher.addEvent(event, userReference);
        this.dispatcher.addEvent(similarevent, userReference);
        this.dispatcher.addEvent(otherevent, userReference);

        // Give enough time to wait for the grace period and process the event
        Thread.sleep(200);

        events = new HashMap<>();
        CompositeEvent composite = new CompositeEvent(event);
        composite.add(similarevent, SimilarityCalculator.SAME_DOCUMENT_AND_TYPE);
        events.put(userReference, composite);
        events.put(userReference, new CompositeEvent(otherevent));

        verify(this.sender).sendMails(events);
    }
}
