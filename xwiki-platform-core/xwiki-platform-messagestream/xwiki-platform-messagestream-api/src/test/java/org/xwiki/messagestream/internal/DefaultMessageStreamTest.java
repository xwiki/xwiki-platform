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

import java.util.Objects;

import org.junit.jupiter.api.Test;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.eventstream.EventFactory;
import org.xwiki.eventstream.EventStream;
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
import static org.mockito.ArgumentMatchers.argThat;
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
 * @since 11.10.11
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

    public static final ObjectReference RECIPIENT_USER_OBJECT_REFERENCE =
        new ObjectReference("XWiki.XWikiUsers", RECIPIENT_USER_DOCUMENT_REFERENCE);

    private static final DocumentReference RECIPIENT_GROUP_DOCUMENT_REFERENCE =
        new DocumentReference("xwiki", "XWiki", "RecipientGroup");

    public static final ObjectReference RECIPIENT_GROUP_OBJECT_REFERENCE =
        new ObjectReference("XWiki.XWikiGroups", RECIPIENT_GROUP_DOCUMENT_REFERENCE);

    @InjectMockComponents
    private DefaultMessageStream defaultMessageStream;

    @MockComponent
    private QueryManager qm;

    @MockComponent
    private ModelContext context;

    @MockComponent
    private EntityReferenceSerializer<String> serializer;

    @MockComponent
    private EventStream stream;

    @MockComponent
    private EventFactory factory;

    @MockComponent
    private DocumentAccessBridge bridge;

    @Test
    void postPublicMessage()
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        this.defaultMessageStream.postPublicMessage(MESSAGE);
        verify(this.stream).addEvent(argThat(other ->
            Objects.equals(other.getType(), "publicMessage")
                && Objects.equals(other.getApplication(), "MessageStream")
                && Objects.equals(other.getDocument(), EVENT_DOCUMENT_REFERENCE)
                && Objects.equals(other.getImportance(), MINOR)
                && Objects.equals(other.getRelatedEntity(), USER_DOCUMENT_REFERENCE)
                && Objects.equals(other.getId(), "eid")
                && Objects.equals(other.getStream(), USER_REFERENCE)
                && Objects.equals(other.getBody(), MESSAGE)
                && Objects.equals(other.getTitle(), "messagestream.descriptors.rss.publicMessage.title")));
    }

    @Test
    void postDirectMessageToUser()
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.bridge.exists(RECIPIENT_USER_DOCUMENT_REFERENCE)).thenReturn(true);
        this.defaultMessageStream.postDirectMessageToUser(MESSAGE, RECIPIENT_USER_DOCUMENT_REFERENCE);
        verify(this.stream).addEvent(argThat(other ->
            Objects.equals(other.getType(), "directMessage")
                && Objects.equals(other.getApplication(), "MessageStream")
                && Objects.equals(other.getDocument(), EVENT_DOCUMENT_REFERENCE)
                && Objects.equals(other.getImportance(), CRITICAL)
                && Objects.equals(other.getRelatedEntity(), RECIPIENT_USER_OBJECT_REFERENCE)
                && Objects.equals(other.getId(), "eid")
                && Objects.equals(other.getStream(), null)
                && Objects.equals(other.getBody(), MESSAGE)
                && Objects.equals(other.getTitle(), "messagestream.descriptors.rss.directMessage.title")));
    }

    @Test
    void postDirectMessageToUserRecipientDoesNotExist()
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.bridge.exists(RECIPIENT_USER_DOCUMENT_REFERENCE)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.defaultMessageStream.postDirectMessageToUser(MESSAGE, RECIPIENT_USER_DOCUMENT_REFERENCE));
        assertEquals("Target user does not exist", exception.getMessage());
    }

    @Test
    void postMessageToGroup()
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.bridge.exists(RECIPIENT_GROUP_DOCUMENT_REFERENCE)).thenReturn(true);
        this.defaultMessageStream.postMessageToGroup(MESSAGE, RECIPIENT_GROUP_DOCUMENT_REFERENCE);
        verify(this.stream).addEvent(argThat(other ->
            Objects.equals(other.getType(), "groupMessage")
                && Objects.equals(other.getApplication(), "MessageStream")
                && Objects.equals(other.getDocument(), EVENT_DOCUMENT_REFERENCE)
                && Objects.equals(other.getImportance(), MAJOR)
                && Objects.equals(other.getRelatedEntity(), RECIPIENT_GROUP_OBJECT_REFERENCE)
                && Objects.equals(other.getId(), "eid")
                && Objects.equals(other.getStream(), null)
                && Objects.equals(other.getBody(), MESSAGE)
                && Objects.equals(other.getTitle(), "messagestream.descriptors.rss.groupMessage.title")));
    }

    @Test
    void postMessageToGroupRecipientDoesNotExist()
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        when(this.bridge.exists(RECIPIENT_GROUP_DOCUMENT_REFERENCE)).thenReturn(false);

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
            () -> this.defaultMessageStream.postMessageToGroup(MESSAGE, RECIPIENT_GROUP_DOCUMENT_REFERENCE));
        assertEquals("Target group does not exist", exception.getMessage());
    }

    @Test
    void postPersonalMessage()
    {
        DefaultEvent t = new DefaultEvent();
        t.setId("eid");
        when(this.factory.createEvent()).thenReturn(t);
        when(this.context.getCurrentEntityReference()).thenReturn(DOCUMENT_REFERENCE);
        when(this.bridge.getCurrentUserReference()).thenReturn(USER_DOCUMENT_REFERENCE);
        when(this.serializer.serialize(USER_DOCUMENT_REFERENCE)).thenReturn(USER_REFERENCE);
        this.defaultMessageStream.postPersonalMessage(MESSAGE);
        verify(this.stream).addEvent(argThat(other ->
            Objects.equals(other.getType(), "personalMessage")
                && Objects.equals(other.getApplication(), "MessageStream")
                && Objects.equals(other.getDocument(), EVENT_DOCUMENT_REFERENCE)
                && Objects.equals(other.getImportance(), MEDIUM)
                && Objects.equals(other.getRelatedEntity(), USER_DOCUMENT_REFERENCE)
                && Objects.equals(other.getId(), "eid")
                && Objects.equals(other.getStream(), USER_REFERENCE)
                && Objects.equals(other.getBody(), MESSAGE)
                && Objects.equals(other.getTitle(), "messagestream.descriptors.rss.personalMessage.title")));
    }
}
