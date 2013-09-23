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

import org.pircbotx.Channel;
import org.pircbotx.PircBotX;
import org.pircbotx.User;
import org.pircbotx.exception.IrcException;

/**
 * Extends PircBotX to set a flag when the bot is stopped so that we can differentiate between a user stop of the Bot
 * versus a disconnection. This is required for example in the AutoReconnect Bot Listener where we automatically
 * try to reconnect when the Bot is disconnected (but not voluntarily by a user).
 *
 * @version $Id$
 * @since 4.0M2
 */
public class ExtendedPircBotX extends PircBotX
{
    /**
     * @see #shouldStop()
     */
    private boolean shouldStop;

    /**
     * @return true if the Bot has been stopped by the user (ie disconnect has been called explicitly) or false
     *         otherwise (if the bot has been disconnected by the Server)
     */
    public boolean shouldStop()
    {
        return this.shouldStop;
    }

    @Override
    public synchronized void disconnect()
    {
        this.shouldStop = true;
        super.disconnect();
    }

    @Override
    public synchronized void connect(String hostname) throws IOException, IrcException
    {
        super.connect(hostname);
        this.shouldStop = false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * The message is sent line by line (split on newlines) with a maximum of 5 lines to prevent flooding.
     * </p>
     */
    @Override
    public void sendMessage(Channel channel, User user, String message)
    {
        String[] lines = message.split("[\\r\\n]+");
        int counter = 0;
        for (String line : lines) {
            super.sendMessage(channel, user, line);
            if (counter++ > 3) {
                break;
            }
        }
        if (counter < lines.length) {
            super.sendMessage(channel, user, "... and " + (lines.length - counter) + " more lines...");
        }
    }
}
