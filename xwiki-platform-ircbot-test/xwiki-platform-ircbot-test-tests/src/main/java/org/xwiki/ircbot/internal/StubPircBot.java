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
import java.util.List;

import org.jibble.pircbot.IrcException;
import org.jibble.pircbot.NickAlreadyInUseException;
import org.xwiki.ircbot.IRCBotListener;

/**
 * Prevent anything going on the wire.
 *
 * @version $Id$
 * @since 4.0M1
 */
public class StubPircBot implements PircBotInterface
{
    private IRCBotListener ircBotListener;

    private String hostname;

    private String channel;

    private List<String> messages = new ArrayList();

    public StubPircBot(IRCBotListener ircBotListener)
    {
        this.ircBotListener = ircBotListener;
    }

    public List<String> getMessages()
    {
        return this.messages;
    }

    @Override
    public void connect(String hostname) throws IOException, IrcException, NickAlreadyInUseException
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
        this.channel = channel;
    }

    @Override
    public void reconnect() throws IOException, IrcException, NickAlreadyInUseException
    {
    }

    @Override
    public void sendMessage(String target, String message)
    {
        // Store the message sent in memory
        this.messages.add(message);
    }

    @Override
    public void setBotName(String botName)
    {
    }
}
