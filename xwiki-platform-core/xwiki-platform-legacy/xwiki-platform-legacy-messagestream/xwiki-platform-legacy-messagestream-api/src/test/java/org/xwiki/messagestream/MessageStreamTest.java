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
package org.xwiki.messagestream;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.internal.verification.api.VerificationData;
import org.mockito.verification.VerificationMode;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.eventstream.Event;
import org.xwiki.eventstream.Event.Importance;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventSearchResult;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.EventStreamException;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.eventstream.query.SimpleEventQuery;
import org.xwiki.messagestream.internal.DefaultMessageStream;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for the {@link org.xwiki.userstatus.internal.DefaultEvent default event} and
 * {@link org.xwiki.messagestream.internal.DefaultMessageStream default event factory}.
 * 
 * @version $Id$
 */
@ComponentTest
class MessageStreamTest
{
    private static final DocumentReference CURRENT_USER = new DocumentReference("wiki", "XWiki", "JohnDoe");

    private static final DocumentReference TARGET_USER = new DocumentReference("wiki", "XWiki", "JaneBuck");

    private static final DocumentReference TARGET_GROUP = new DocumentReference("wiki", "XWiki", "MyFriends");

    @MockComponent
    private DocumentAccessBridge mockBridge;

    @MockComponent
    private EntityReferenceSerializer<String> mockSerializer;

    @MockComponent
    private EventFactory mockEventFactory;

    @MockComponent
    private ModelContext mockContext;

    @MockComponent
    private EventStore mockEventStore;

    @InjectMockComponents
    private DefaultMessageStream stream;

    @BeforeEach
    void beforeEach()
    {
        when(this.mockSerializer.serialize(CURRENT_USER)).thenReturn("wiki:XWiki.JohnDoe");
        when(this.mockSerializer.serialize(TARGET_USER)).thenReturn("wiki:XWiki.JaneBuck");
        when(this.mockSerializer.serialize(TARGET_GROUP)).thenReturn("wiki:XWiki.MyFriends");

        when(this.mockEventStore.saveEvent(any())).thenReturn(CompletableFuture.completedFuture(null));
    }

    private Event setupForNewMessage() throws Exception
    {
        Event event = new DefaultEvent();
        event.setId(UUID.randomUUID().toString());

        when(this.mockEventFactory.createEvent()).thenReturn(event);
        when(this.mockContext.getCurrentEntityReference()).thenReturn(new DocumentReference("wiki", "Space", "Page"));

        return event;
    }

    private void verifyForNewMessage(Event event)
    {
        verify(this.mockEventFactory).createEvent();
        verify(this.mockEventStore).saveEvent(event);
    }

    private Event setupForPublicMessage() throws Exception
    {
        Event e = setupForNewMessage();

        when(this.mockBridge.getCurrentUserReference()).thenReturn(CURRENT_USER);

        return e;
    }

    private Event setupForPersonalMessage() throws Exception
    {
        Event e = setupForNewMessage();

        when(this.mockBridge.getCurrentUserReference()).thenReturn(CURRENT_USER);

        return e;
    }

    private Event setupForDirectMessage() throws ComponentLookupException, Exception
    {
        Event e = setupForNewMessage();

        when(this.mockBridge.exists(TARGET_USER)).thenReturn(true);

        return e;
    }

    private Event setupForGroupMessage() throws ComponentLookupException, Exception
    {
        Event e = setupForNewMessage();

        when(this.mockBridge.exists(TARGET_GROUP)).thenReturn(true);

        return e;
    }

    private void setupForLimitQueries(int expectedLimit, int expectedOffset) throws Exception
    {
        when(this.mockBridge.getCurrentUserReference()).thenReturn(CURRENT_USER);

        when(this.mockEventStore.search(any())).thenReturn(EventSearchResult.EMPTY);
    }

    private void verifyLimitQueries(int expectedLimit, int expectedOffset) throws EventStreamException
    {
        verify(this.mockEventStore, new VerificationMode()
        {
            @Override
            public void verify(VerificationData data)
            {
                SimpleEventQuery query = data.getAllInvocations().get(0).getArgument(0);
                assertEquals(expectedLimit, query.getLimit());
                assertEquals(expectedOffset, query.getOffset());
            }
        }).search(any());
    }

    // Tests

    @Test
    void postPublicMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();

        this.stream.postPublicMessage("Hello World!");

        assertEquals("Hello World!", postedMessage.getBody());
        assertEquals(Importance.MINOR, postedMessage.getImportance());
        assertEquals("publicMessage", postedMessage.getType());
        assertEquals(CURRENT_USER, postedMessage.getRelatedEntity());
    }

    @Test
    void postPublicMessageWithNullMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();

        this.stream.postPublicMessage(null);

        assertEquals(null, postedMessage.getBody());
    }

    @Test
    void postPublicMessageWithEmptyMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();

        this.stream.postPublicMessage("");

        assertEquals("", postedMessage.getBody());
    }

    @Test
    void postPublicMessageWithLongMessage() throws Exception
    {
        Event postedMessage = setupForPublicMessage();

        this.stream.postPublicMessage(StringUtils.repeat('a', 10000));

        assertEquals(StringUtils.repeat('a', 2000), postedMessage.getBody());
    }

    @Test
    void postPersonalMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();

        this.stream.postPersonalMessage("Hello World!");

        assertEquals("Hello World!", postedMessage.getBody());
        assertEquals(Importance.MEDIUM, postedMessage.getImportance());
        assertEquals("personalMessage", postedMessage.getType());
        assertEquals(CURRENT_USER, postedMessage.getRelatedEntity());
    }

    @Test
    void postPersonalMessageWithNullMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();

        this.stream.postPersonalMessage(null);

        assertEquals(null, postedMessage.getBody());
    }

    @Test
    void postPersonalMessageWithEmptyMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();

        this.stream.postPersonalMessage("");

        assertEquals("", postedMessage.getBody());
    }

    @Test
    void postPersonalMessageWithLongMessage() throws Exception
    {
        Event postedMessage = setupForPersonalMessage();

        this.stream.postPersonalMessage(StringUtils.repeat('a', 10000));

        assertEquals(StringUtils.repeat('a', 2000), postedMessage.getBody());
    }

    @Test
    void postDirectMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();

        this.stream.postDirectMessageToUser("Hello World!", TARGET_USER);

        verifyForNewMessage(postedMessage);

        assertEquals("Hello World!", postedMessage.getBody());
        assertEquals(Importance.CRITICAL, postedMessage.getImportance());
        assertEquals("directMessage", postedMessage.getType());
        assertEquals("wiki:XWiki.JaneBuck", postedMessage.getStream());
        assertEquals(new ObjectReference("XWiki.XWikiUsers", TARGET_USER), postedMessage.getRelatedEntity());
    }

    @Test
    void postDirectMessageWithNullMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();

        this.stream.postDirectMessageToUser(null, TARGET_USER);

        verifyForNewMessage(postedMessage);

        assertEquals(null, postedMessage.getBody());
    }

    @Test
    void postDirectMessageWithEmptyMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();

        this.stream.postDirectMessageToUser("", TARGET_USER);

        verifyForNewMessage(postedMessage);

        assertEquals("", postedMessage.getBody());
    }

    @Test
    void postDirectMessageWithLongMessage() throws Exception
    {
        Event postedMessage = setupForDirectMessage();

        this.stream.postDirectMessageToUser(StringUtils.repeat('a', 10000), TARGET_USER);

        verifyForNewMessage(postedMessage);

        assertEquals(StringUtils.repeat('a', 2000), postedMessage.getBody());
    }

    @Test
    void postDirectMessageWithNonExistingTarget() throws Exception
    {
        DocumentReference targetUser = new DocumentReference("xwiki", "XWiki", "Nobody");
        when(this.mockBridge.exists(targetUser)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> this.stream.postDirectMessageToUser("Hello Nobody!", targetUser));
    }

    @Test
    void postGroupMessage() throws Exception
    {
        Event postedMessage = setupForGroupMessage();

        this.stream.postMessageToGroup("Hello Friends!", TARGET_GROUP);

        assertEquals("Hello Friends!", postedMessage.getBody());
        assertEquals(Importance.MAJOR, postedMessage.getImportance());
        assertEquals("groupMessage", postedMessage.getType());
        assertEquals("wiki:XWiki.MyFriends", postedMessage.getStream());
        assertEquals(new ObjectReference("XWiki.XWikiGroups", TARGET_GROUP), postedMessage.getRelatedEntity());
    }

    @Test
    void postGroupMessageWithNonExistingTarget() throws Exception
    {
        DocumentReference targetGroup = new DocumentReference("xwiki", "XWiki", "Nobodies");

        when(this.mockBridge.exists(targetGroup)).thenReturn(false);

        assertThrows(IllegalArgumentException.class,
            () -> this.stream.postMessageToGroup("Hello Nobodies!", targetGroup));
    }

    @Test
    void getRecentPersonalMessages() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages();

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesForAuthor() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages(CURRENT_USER);

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesWithNegativeLimit() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages(-4, 0);

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesWithZeroLimit() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages(0, 0);

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesWithLimit1() throws Exception
    {
        setupForLimitQueries(1, 0);

        this.stream.getRecentPersonalMessages(1, 0);

        verifyLimitQueries(1, 0);
    }

    @Test
    void getRecentPersonalMessagesWithLimit30() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages(30, 0);

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesWithLimit100() throws Exception
    {
        setupForLimitQueries(100, 0);

        this.stream.getRecentPersonalMessages(100, 0);

        verifyLimitQueries(100, 0);
    }

    @Test
    void getRecentPersonalMessagesWithNegativeOffset() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages(30, -4);

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesWithNegativeLimitAndOffset() throws Exception
    {
        setupForLimitQueries(30, 0);

        this.stream.getRecentPersonalMessages(-1, -1);

        verifyLimitQueries(30, 0);
    }

    @Test
    void getRecentPersonalMessagesWithZeroOffset() throws Exception
    {
        setupForLimitQueries(100, 0);

        this.stream.getRecentPersonalMessages(100, 0);

        verifyLimitQueries(100, 0);
    }

    @Test
    void getRecentPersonalMessagesWithOffset1() throws Exception
    {
        setupForLimitQueries(20, 1);

        this.stream.getRecentPersonalMessages(20, 1);

        verifyLimitQueries(20, 1);
    }

    @Test
    void getRecentPersonalMessagesWithOffset100() throws Exception
    {
        setupForLimitQueries(50, 100);

        this.stream.getRecentPersonalMessages(50, 100);

        verifyLimitQueries(50, 100);
    }

    @Test
    void getRecentPersonalMessagesWhenQueryFails() throws Exception
    {
        setupForLimitQueries(30, 0);

        doThrow(new EventStreamException()).when(this.mockEventStore).search(any());

        List<Event> result = this.stream.getRecentPersonalMessages();
        assertNotNull(result);
        assertTrue(result.isEmpty());

        verifyLimitQueries(30, 0);
    }
}
