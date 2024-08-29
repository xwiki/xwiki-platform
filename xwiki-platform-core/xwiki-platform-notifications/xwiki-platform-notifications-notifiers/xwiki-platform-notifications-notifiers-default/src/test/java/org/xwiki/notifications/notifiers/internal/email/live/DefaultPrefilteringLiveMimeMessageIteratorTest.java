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

import java.util.Collections;
import java.util.List;

import javax.inject.Named;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.eventstream.Event;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.GroupingEventManager;
import org.xwiki.notifications.NotificationException;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.test.mockito.MockitoComponentManager;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link DefaultPrefilteringLiveMimeMessageIterator}.
 *
 * @version $Id$
 */
@ComponentTest
class DefaultPrefilteringLiveMimeMessageIteratorTest
{
    @InjectMockComponents
    private DefaultPrefilteringLiveMimeMessageIterator liveMimeMessageIterator;

    @MockComponent
    private GroupingEventManager groupingEventManager;

    @MockComponent
    @Named("document")
    private UserReferenceResolver<DocumentReference> userReferenceResolver;

    @MockComponent
    private LiveNotificationEmailEventFilter eventFilter;

    private DocumentReference user = new DocumentReference("xwiki", "XWiki", "Foo");
    private String userEmail = "foo@xwiki.com";

    @BeforeEach
    void setup(MockitoComponentManager componentManager) throws Exception
    {
        DocumentAccessBridge documentAccessBridge = componentManager.getInstance(DocumentAccessBridge.class);
        when(documentAccessBridge.getProperty(user,
            new DocumentReference("xwiki", "XWiki", "XWikiUsers"), 0, "email")).thenReturn(userEmail);
    }

    @Test
    void retrieveCompositeEventList() throws NotificationException
    {
        Event event = mock(Event.class);
        CompositeEvent compositeEvent = mock(CompositeEvent.class);
        UserReference userReference = mock(UserReference.class);
        when(this.userReferenceResolver.resolve(user)).thenReturn(userReference);

        when(this.groupingEventManager.getCompositeEvents(List.of(event), userReference, "email"))
            .thenReturn(List.of(compositeEvent));

        when(this.eventFilter.canAccessEvent(user, compositeEvent)).thenReturn(false);
        this.liveMimeMessageIterator
            .initialize(Collections.singletonMap(user, List.of(event)), Collections.emptyMap(), null);
        assertEquals(Collections.emptyList(), this.liveMimeMessageIterator.retrieveCompositeEventList(user));

        when(this.eventFilter.canAccessEvent(user, compositeEvent)).thenReturn(true);
        assertEquals(Collections.singletonList(compositeEvent),
            this.liveMimeMessageIterator.retrieveCompositeEventList(user));
    }
}
