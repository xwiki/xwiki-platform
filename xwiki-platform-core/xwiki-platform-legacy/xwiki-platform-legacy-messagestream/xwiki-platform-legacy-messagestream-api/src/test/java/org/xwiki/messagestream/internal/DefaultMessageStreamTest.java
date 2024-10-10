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
package org.xwiki.messagestream.internal;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStore;
import org.xwiki.eventstream.internal.DefaultEvent;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.ObjectReference;
import org.xwiki.query.QueryManager;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.xwiki.eventstream.Event.Importance.CRITICAL;
import static org.xwiki.eventstream.Event.Importance.MAJOR;
import static org.xwiki.eventstream.Event.Importance.MEDIUM;
import static org.xwiki.eventstream.Event.Importance.MINOR;

/**
 * Test of {@link DefaultMessageStream}.
 *
 * @version $Id$
 * @since 12.9RC1
 */
@ComponentTest
class DefaultMessageStreamTest
{
    public static final DocumentReference DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "Doc");

    public static final DocumentReference USER_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "User");

    public static final String USER_REFERENCE = "xwiki:XWiki.User";

    public static final DocumentReference EVENT_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki", "eid");

    public static final String MESSAGE = "message";

    private static final DocumentReference RECIPIENT_USER_DOCUMENT_REFERENCE = new DocumentReference("xwiki", "XWiki",
        "RecipientUser");

    private static final DocumentReference RECIPIENT_GROUP_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki",
            "RecipientGroup");

    @InjectMockComponents
    private DefaultMessageStream defaultMessageStream;

    @MockComponent
    private QueryManager qm;

    @MockComponent
    private ModelContext context;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private EventStore eventStore;

    @MockComponent
    private EventFactory factory;

    @MockComponent
    private DocumentAccessBridge bridge;

    @Test
    void postPublicMessage() throws Exception
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        when(this.eventStore.saveEvent(t)).thenReturn(completableFuture);
        this.defaultMessageStream.postPublicMessage(MESSAGE);
        DefaultEvent event = new DefaultEvent();
        event.setType("publicMessage");
        event.setApplication("MessageStream");
        event.setDocument(EVENT_DOCUMENT_REFERENCE);
        event.setImportance(MINOR);
        event.setRelatedEntity(USER_DOCUMENT_REFERENCE);
        event.setId("eid");
        event.setStream(USER_REFERENCE);
        event.setBody(MESSAGE);
        event.setTitle("messagestream.descriptors.rss.publicMessage.title");
        verify(this.eventStore).saveEvent(event);
        verify(completableFuture).get();
    }

    @Test
    void postDirectMessageToUser() throws Exception
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        when(this.eventStore.saveEvent(t)).thenReturn(completableFuture);
        when(this.bridge.exists(RECIPIENT_USER_DOCUMENT_REFERENCE)).thenReturn(true);
        this.defaultMessageStream.postDirectMessageToUser(MESSAGE, RECIPIENT_USER_DOCUMENT_REFERENCE);
        DefaultEvent event = new DefaultEvent();
        event.setType("directMessage");
        event.setApplication("MessageStream");
        event.setDocument(EVENT_DOCUMENT_REFERENCE);
        event.setImportance(CRITICAL);
        event.setRelatedEntity(new ObjectReference("XWiki.XWikiUsers", RECIPIENT_USER_DOCUMENT_REFERENCE));
        event.setId("eid");
        event.setBody(MESSAGE);
        event.setTitle("messagestream.descriptors.rss.directMessage.title");
        verify(this.eventStore).saveEvent(event);
        verify(completableFuture).get();
    }

    @Test
    void postDirectMessageToUserRecipientDoesNotExist() throws Exception
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.bridge.exists(RECIPIENT_USER_DOCUMENT_REFERENCE)).thenReturn(false);

        CompletableFuture completableFuture = mock(CompletableFuture.class);
        when(this.eventStore.saveEvent(t)).thenReturn(completableFuture);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.defaultMessageStream.postDirectMessageToUser(MESSAGE, RECIPIENT_USER_DOCUMENT_REFERENCE));
        assertEquals("Target user does not exist", exception.getMessage());
    }

    @Test
    void postMessageToGroup() throws Exception
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        when(this.eventStore.saveEvent(t)).thenReturn(completableFuture);
        when(this.bridge.exists(RECIPIENT_GROUP_DOCUMENT_REFERENCE)).thenReturn(true);
        this.defaultMessageStream.postMessageToGroup(MESSAGE, RECIPIENT_GROUP_DOCUMENT_REFERENCE);
        DefaultEvent event = new DefaultEvent();
        event.setType("groupMessage");
        event.setApplication("MessageStream");
        event.setDocument(EVENT_DOCUMENT_REFERENCE);
        event.setImportance(MAJOR);
        event.setRelatedEntity(new ObjectReference("XWiki.XWikiGroups", RECIPIENT_GROUP_DOCUMENT_REFERENCE));
        event.setId("eid");
        event.setBody(MESSAGE);
        event.setTitle("messagestream.descriptors.rss.groupMessage.title");
        verify(this.eventStore).saveEvent(event);
        verify(completableFuture).get();
    }

    @Test
    void postMessageToGroupRecipientDoesNotExist() throws Exception
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.bridge.exists(RECIPIENT_GROUP_DOCUMENT_REFERENCE)).thenReturn(false);

        CompletableFuture completableFuture = mock(CompletableFuture.class);
        when(this.eventStore.saveEvent(t)).thenReturn(completableFuture);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.defaultMessageStream.postMessageToGroup(MESSAGE, RECIPIENT_GROUP_DOCUMENT_REFERENCE));
        assertEquals("Target group does not exist", exception.getMessage());
    }

    @Test
    void postPersonalMessage() throws Exception
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        CompletableFuture completableFuture = mock(CompletableFuture.class);
        when(this.eventStore.saveEvent(t)).thenReturn(completableFuture);
        this.defaultMessageStream.postPersonalMessage(MESSAGE);
        DefaultEvent event = new DefaultEvent();
        event.setType("personalMessage");
        event.setApplication("MessageStream");
        event.setDocument(EVENT_DOCUMENT_REFERENCE);
        event.setImportance(MEDIUM);
        event.setRelatedEntity(USER_DOCUMENT_REFERENCE);
        event.setId("eid");
        event.setStream(USER_REFERENCE);
        event.setBody(MESSAGE);
        event.setTitle("messagestream.descriptors.rss.personalMessage.title");
        verify(this.eventStore).saveEvent(event);
        verify(completableFuture).get();
    }
}
