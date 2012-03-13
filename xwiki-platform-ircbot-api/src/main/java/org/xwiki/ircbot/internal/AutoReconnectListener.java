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

import javax.inject.Inject;
import javax.inject.Named;

import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.DisconnectEvent;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.IRCBotListener;

/**
 * Automatically reconnects when the IRC Client connection to the IRC server is lost.
 *
 * @param <T> the reference to the PircBotX instance
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("autoreconnect")
public class AutoReconnectListener<T extends ExtendedPircBotX> extends ListenerAdapter<T> implements IRCBotListener<T>
{
    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Override
    public String getDescription()
    {
        return "Automatically reconnect the Bot to the channels it's been disconnected from";
    }

    @Override
    public String getName()
    {
        return "AutoReconnect";
    }

    @Override
    public void onDisconnect(DisconnectEvent<T> event) throws Exception
    {
        ExtendedPircBotX bot = event.getBot();
        if (!bot.shouldStop()) {
            this.logger.debug("IRC Bot has been disconnected");
            while (!bot.isConnected()) {
                try {
                    bot.reconnect();
                } catch (Exception e) {
                    // Cannot reconnect, wait for some time before trying to reconnect again
                    try {
                        Thread.sleep(1000L * 30);
                    } catch (InterruptedException ie) {
                        // Failed to sleep, just ignore
                    }
                }
            }
        }
    }
}
