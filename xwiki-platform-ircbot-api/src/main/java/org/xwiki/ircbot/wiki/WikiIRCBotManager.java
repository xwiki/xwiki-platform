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
     *
     * @throws IRCBotException if any error happens
     */
    void startBot() throws IRCBotException;

    /**
     * Disconnect the IRC Bot and unregister all Wiki Bot Listeners.
     *
     * @throws IRCBotException if any error happens
     */
    void stopBot() throws IRCBotException;

    /**
     * Register (and thus activate) the Wiki Bot Listener defined in the passed document's reference.
     *
     * @param reference the reference of the document containing the Wiki Bot Listener definition
     * @throws IRCBotException if any error happens
     */
    void registerWikiBotListener(DocumentReference reference) throws IRCBotException;

    /**
     * Unregister (and thus deactivate) the Wiki Bot Listener defined in the passed document's reference.
     *
     * @param reference the reference of the document containing the Wiki Bot Listener definition
     * @throws IRCBotException if any error happens
     */
    void unregisterWikiBotListener(DocumentReference reference) throws IRCBotException;

    /**
     * Register (and thus activate) all the Wiki Bot Listeners found in the current wiki.
     *
     * @throws IRCBotException if any error happens
     */
    void registerWikiBotListeners() throws IRCBotException;

    /**
     * Unregister (and thus deactivate) all the Wiki Bot Listeners found in the current wiki.
     *
     * @throws IRCBotException if any error happens
     */
    void unregisterWikiBotListeners() throws IRCBotException;

    /**
     * @return true if the IRC Bot is connected or false otherwise
     */
    boolean isBotStarted();

    /**
     * Provides information about all Bot Listeners (whether they are Wiki Bot Listeners or standard Java components).
     *
     * @return the information about all Bot Listeners (such as id, name, description, etc
     * @throws IRCBotException if any error happens
     */
    List<BotListenerData> getBotListenerData() throws IRCBotException;
}
