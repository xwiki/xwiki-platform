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

import org.jibble.pircbot.PircBot;
import org.xwiki.ircbot.IRCBotMessageHandler;

public class ExtendedPircBot extends PircBot implements PircBotInterface
{
    private IRCBotMessageHandler messageHandler;

    public ExtendedPircBot(IRCBotMessageHandler messageHandler)
    {
        this.messageHandler = messageHandler;
    }

    public void setBotName(String botName)
    {
        super.setName(botName);
    }

    @Override
    public String[] getConnectedChannels()
    {
        return super.getChannels();
    }

    @Override
    protected void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        this.messageHandler.onMessage(channel, sender, login, hostname, message);
    }

    @Override
    protected void onDisconnect()
    {
        this.messageHandler.onDisconnect();
    }

    @Override
    protected void onJoin(String channel, String sender, String login, String hostname)
    {
        this.messageHandler.onJoin(channel, sender, login, hostname);
    }

    @Override
    protected void onPart(String channel, String sender, String login, String hostname)
    {
        this.messageHandler.onPart(channel, sender, login, hostname);
    }

    @Override
    protected void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
    {
        this.messageHandler.onQuit(sourceNick, sourceLogin, sourceHostname, reason);
    }

    @Override
    protected void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        this.messageHandler.onPrivateMessage(sender, login, hostname, message);
    }

    @Override
    protected void onNickChange(String oldNick, String login, String hostname, String newNick)
    {
        this.messageHandler.onNickChange(oldNick, login, hostname, newNick);
    }
}
