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
package org.xwiki.netflux.internal;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import javax.websocket.Session;

import org.junit.jupiter.api.Test;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.websocket.WebSocketContext;

/**
 * Unit tests for {@link EntityChannelScriptAuthorBot}.
 *
 * @version $Id$
 */
@ComponentTest
class EntityChannelScriptAuthorBotTest
{
    @InjectMockComponents
    private EntityChannelScriptAuthorBot bot;

    @MockComponent
    private EntityChannelStore entityChannels;

    @MockComponent
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @MockComponent
    private WebSocketContext webSocketContext;

    @MockComponent
    private EntityChannelScriptAuthorTracker scriptAuthorTracker;

    private DocumentReference documentReference = new DocumentReference("test", "Some", "Page");

    private Channel channel = new Channel("one");

    @Test
    void onJoinChannel()
    {
        // No entity channel associated.
        assertFalse(this.bot.onJoinChannel(this.channel));

        // The associated entity channel doesn't need protection.
        EntityChannel entityChannel =
            new EntityChannel(this.documentReference, List.of("en", "content", "events"), this.channel.getKey());
        when(this.entityChannels.getChannel(this.channel.getKey())).thenReturn(Optional.of(entityChannel));
        assertFalse(this.bot.onJoinChannel(this.channel));

        // The associated entity channel needs protection.
        entityChannel =
            new EntityChannel(this.documentReference, List.of("en", "content", "wiki"), this.channel.getKey());
        when(this.entityChannels.getChannel(this.channel.getKey())).thenReturn(Optional.of(entityChannel));
        assertTrue(this.bot.onJoinChannel(this.channel));

        // The associated entity channel doesn't need protection.
        entityChannel = new EntityChannel(this.documentReference, List.of(), this.channel.getKey());
        when(this.entityChannels.getChannel(this.channel.getKey())).thenReturn(Optional.of(entityChannel));
        assertFalse(this.bot.onJoinChannel(this.channel));

        // The associated entity channel needs protection.
        entityChannel =
            new EntityChannel(this.documentReference, List.of("en", "property", "wysiwyg"), this.channel.getKey());
        when(this.entityChannels.getChannel(this.channel.getKey())).thenReturn(Optional.of(entityChannel));
        assertTrue(this.bot.onJoinChannel(this.channel));
    }

    @Test
    void onChannelMessage()
    {
        User sender = mock(User.class);
        Session session = mock(Session.class);
        when(sender.getSession()).thenReturn(session);

        doAnswer(invocation -> {
            Runnable runnable = invocation.getArgument(1);
            runnable.run();
            return null;
        }).when(this.webSocketContext).run(any(Session.class), any(Runnable.class));

        // Not a command message.
        this.bot.onChannelMessage(this.channel, sender, "PING", null);
        verify(this.scriptAuthorTracker, never()).maybeUpdateScriptAuthor(any(EntityChannel.class),
            any(UserReference.class));

        // No entity channel associated.
        this.bot.onChannelMessage(this.channel, sender, "MSG", null);
        verify(this.scriptAuthorTracker, never()).maybeUpdateScriptAuthor(any(EntityChannel.class),
            any(UserReference.class));

        // The message is send to an entity channel.
        UserReference currentUserReference = mock(UserReference.class);
        when(this.currentUserResolver.resolve(CurrentUserReference.INSTANCE)).thenReturn(currentUserReference);

        EntityChannel entityChannel =
            new EntityChannel(this.documentReference, List.of("en", "content", "wiki"), this.channel.getKey());
        when(this.entityChannels.getChannel(this.channel.getKey())).thenReturn(Optional.of(entityChannel));

        this.bot.onChannelMessage(this.channel, sender, "MSG", null);
        verify(this.scriptAuthorTracker).maybeUpdateScriptAuthor(entityChannel, currentUserReference);
    }
}
