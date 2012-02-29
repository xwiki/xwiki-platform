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
package org.xwiki.ircbot.wiki;

import java.util.Collection;
import java.util.List;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.wiki.BotListenerData;
import org.xwiki.model.reference.DocumentReference;

/**
 * Provides actions that can be done on the IRC Bot and that involve Wiki Bot Listeners.
 *
 * @version $Id$
 * @since 4.0M1
 */
@ComponentRole
public interface WikiIRCBotManager
{
    /**
     * Connects the IRC Bot using the Configuration data located in
     * {@link WikiIRCBotConstants#WIKI_BOT_CONFIGURATION_CLASS} and registers all Wiki Bot Listeners that are
     * not marked as inactive. Note that the Bot is only connected if it's not marked inactive.
     */
    void startBot() throws IRCBotException;

    /**
     * Disconnect the IRC Bot and unregister all Wiki Bot Listeners.
     */
    void stopBot() throws IRCBotException;

    void registerWikiBotListener(DocumentReference reference) throws IRCBotException;

    void unregisterWikiBotListener(DocumentReference reference);

    void registerWikiBotListeners() throws IRCBotException;

    void unregisterWikiBotListeners() throws IRCBotException;

    boolean isBotStarted();

    List<BotListenerData> getBotListenerData() throws IRCBotException;
}
