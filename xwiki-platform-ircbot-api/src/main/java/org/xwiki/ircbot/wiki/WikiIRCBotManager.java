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
import java.util.Map;

import org.xwiki.component.annotation.Role;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.internal.BotListenerData;

/**
 * Start/stop the Wiki IRC Bot including its Wiki Bot Listeners and provides some other APIs about the Wiki IRC Bot.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface WikiIRCBotManager
{
    /**
     * Connects the IRC Bot using the Configuration data located in
     * {@link WikiIRCBotConstants#WIKI_BOT_CONFIGURATION_CLASS} and registers all Wiki Bot Listeners that are
     * not marked as inactive. Note that the Bot is only connected if it's not marked inactive.
     *
     * @param updateBotStatus if true then update the Bot status to mark it as active
     * @throws IRCBotException if any error happens
     */
    void startBot(boolean updateBotStatus) throws IRCBotException;

    /**
     * Disconnect the IRC Bot and unregister all Wiki Bot Listeners.
     *
     * @param updateBotStatus if true then update the Bot status to mark it as inactive (this means that the Bot should
     *        not be restarted automatically by the IRC Bot Scheduler Job for example)
     * @throws IRCBotException if any error happens
     */
    void stopBot(boolean updateBotStatus) throws IRCBotException;

    /**
     * @return true if the IRC Bot is connected or false otherwise
     */
    boolean isBotStarted();

    /**
     * Provides information about all Bot Listeners (whether they are Wiki Bot Listeners or standard Java components).
     *
     * @return the information about all Bot Listeners (such as id, name, description, etc)
     * @throws IRCBotException if any error happens
     */
    List<BotListenerData> getBotListenerData() throws IRCBotException;

    /**
     * @return the IRC Bot Execution Context where variables from the Bot Events are stored. You'll need to access
     *         this if you write a Wiki Bot Listener and want access to the Event data such as the channel, the message
     *         the user, etc
     * @throws IRCBotException if any error happens
     */
    Map<String, Object> getContext() throws IRCBotException;
}
