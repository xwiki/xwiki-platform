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
package org.xwiki.ircbot;

import java.io.IOException;
import java.util.Set;

import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.managers.ListenerManager;
import org.xwiki.component.annotation.Role;

/**
 * Represents an IRC Bot.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Role
public interface IRCBot
{
    void setName(String botName);
    void connect(String hostname) throws IOException, IrcException;
    void joinChannel(String channel);
    void disconnect();
    void identify(String password);
    void sendMessage(String target, String message);

    Set<String> getChannelsNames();
    boolean isConnected();

    public ListenerManager<? extends PircBotX> getListenerManager();
}
