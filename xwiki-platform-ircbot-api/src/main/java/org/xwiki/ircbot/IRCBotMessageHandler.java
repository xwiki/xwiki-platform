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

public interface IRCBotMessageHandler
{
    void onConnect();

    void onDisconnect();

    /**
     * @param channel the channel to which the message was sent
     * @param sender the nick of the person who sent the message
     * @param login the login of the person who sent the message
     * @param hostname the hostname of the person who sent the message
     * @param message the actual message sent to the channel
     */
    void onMessage(String channel, String sender, String login, String hostname, String message);

    void onJoin(String channel, String sender, String login, String hostname);

    void onPart(String channel, String sender, String login, String hostname);

    void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason);

    void onPrivateMessage(String sender, String login, String hostname, String message);

    void onNickChange(String oldNick, String login, String hostname, String newNick);
}
