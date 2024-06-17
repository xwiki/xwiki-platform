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

import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.netflux.EntityChannel;
import org.xwiki.netflux.EntityChannelStore;
import org.xwiki.user.CurrentUserReference;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.websocket.WebSocketContext;

/**
 * A bot that protects entity channels, that are used to sychronize content that may contain scripts, by forcing a
 * content author (i.e. a script author) with minimum script access rights (between all the users that have ever pushed
 * changes to the channel).
 *
 * @version $Id$
 * @since 15.10.11
 * @since 16.4.1
 * @since 16.5.0
 */
@Component
@Singleton
@Named("EntityChannelScriptAuthorBot")
public class EntityChannelScriptAuthorBot extends AbstractBot
{
    private static final List<String> PROTECTED_CHANNELS = List.of("wysiwyg", "wiki");

    @Inject
    private Logger logger;

    @Inject
    private EntityChannelStore entityChannels;

    @Inject
    private UserReferenceResolver<CurrentUserReference> currentUserResolver;

    @Inject
    private WebSocketContext webSocketContext;

    @Inject
    private EntityChannelScriptAuthorTracker scriptAuthorTracker;

    @Override
    public boolean onJoinChannel(Channel channel)
    {
        // We want to protect only entity channels that are used to sychronize content that may contain scripts.
        Optional<EntityChannel> entityChannel = this.entityChannels.getChannel(channel.getKey());
        boolean accept = entityChannel.map(this::needsProtection).orElse(false);
        if (accept) {
            this.logger.debug("Joining channel [{}].", entityChannel.get());
        }
        return accept;
    }

    @Override
    public void onChannelMessage(Channel channel, User sender, String messageType, String message)
    {
        // We're interested only in messages that have content (we want to ignore for instance join, leave or ping
        // messages).
        if (MessageDispatcher.COMMAND_MSG.equals(messageType)) {
            this.entityChannels.getChannel(channel.getKey())
                .ifPresent(entityChannel -> this.webSocketContext.run(sender.getSession(), () -> {
                    UserReference senderUserReference = this.currentUserResolver.resolve(CurrentUserReference.INSTANCE);
                    this.scriptAuthorTracker.maybeUpdateScriptAuthor(entityChannel, senderUserReference);
                }));
        }
    }

    private boolean needsProtection(EntityChannel entityChannel)
    {
        // Protect only entity channels that are used to sychronize content that may contain scripts.
        List<String> path = entityChannel.getPath();
        return !path.isEmpty() && PROTECTED_CHANNELS.contains(path.get(path.size() - 1));
    }
}
