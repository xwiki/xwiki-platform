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

/**
 * IRC Bot listeners should extend this class since it provides already implemented methods.
 *
 * @version $Id$
 * @since 4.0M1
 */
public abstract class AbstractIRCBotListener implements IRCBotListener
{
    /**
     * @see org.xwiki.ircbot.IRCBotListener#getPriority()
     */
    private int priority = 1000;

    @Override
    public int getPriority()
    {
        return this.priority;
    }

    /**
     * @param priority the priority to use (lower means execute before others)
     */
    public void setPriority(int priority)
    {
        this.priority = priority;
    }

    @Override
    public void onRegistration()
    {
        // Do nothing by default
    }

    @Override
    public void onUnregistration()
    {
        // Do nothing by default
    }

    @Override
    public void onConnect()
    {
        // Do nothing by default
    }

    @Override
    public void onDisconnect()
    {
        // Do nothing by default
    }

    @Override
    public void onJoin(String channel, String sender, String login, String hostname)
    {
        // Do nothing by default
    }

    @Override
    public void onMessage(String channel, String sender, String login, String hostname, String message)
    {
        // Do nothing by default
    }

    @Override
    public void onNickChange(String oldNick, String login, String hostname, String newNick)
    {
        // Do nothing by default
    }

    @Override
    public void onPart(String channel, String sender, String login, String hostname)
    {
        // Do nothing by default
    }

    @Override
    public void onPrivateMessage(String sender, String login, String hostname, String message)
    {
        // Do nothing by default
    }

    @Override
    public void onQuit(String sourceNick, String sourceLogin, String sourceHostname, String reason)
    {
        // Do nothing by default
    }

    @Override
    public int compareTo(IRCBotListener ircBotListener)
    {
        return getPriority() - ircBotListener.getPriority();
    }
}
