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
package org.xwiki.ircbot.internal;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;

import org.pircbotx.PircBotX;
import org.pircbotx.exception.IrcException;
import org.pircbotx.hooks.managers.ListenerManager;
import org.pircbotx.hooks.managers.ThreadedListenerManager;
import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.IRCBot;

/**
 * Stub IRC Bot for the functional tests. Ensures that nothing is sent on the wire.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class StubIRCBot implements IRCBot
{
    private String hostname;

    private List<String> messages = new ArrayList();

    private ThreadedListenerManager listenerManager = new ThreadedListenerManager();

    public List<String> getMessages()
    {
        return this.messages;
    }

    @Override
    public Set<String> getChannelsNames()
    {
        return Collections.singleton("channel");
    }

    @Override
    public void connect(String hostname) throws IOException, IrcException
    {
        this.hostname = hostname;
    }

    @Override
    public void disconnect()
    {
        this.hostname = null;
    }

    @Override
    public void identify(String password)
    {
    }

    @Override
    public boolean isConnected()
    {
        // We consider that the bot is connected if the connect method has been called.
        return this.hostname != null;
    }

    @Override
    public void joinChannel(String channel)
    {
    }

    @Override
    public void sendMessage(String target, String message)
    {
        // Store the message sent in memory
        this.messages.add(message);
    }

    @Override
    public void setName(String botName)
    {
    }

    @Override
    public ListenerManager<? extends PircBotX> getListenerManager()
    {
        return this.listenerManager;
    }
}
