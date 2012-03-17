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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.BrokenLinkEventListenerConfiguration;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.transformation.linkchecker.InvalidURLEvent;
import org.xwiki.rendering.transformation.linkchecker.LinkState;

/**
 * Whenever a broken link is found in the wiki, send the notification on the IRC channel.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("ircbrokenlink")
@Singleton
public class BrokenLinkEventListener implements EventListener
{
    /**
     * Used to verify if the Bot is connected since we don't want to send messages if the Bot isn't connected.
     */
    @Inject
    private IRCBot bot;

    /**
     * Used to decide if this listener is active or not.
     */
    @Inject
    private BrokenLinkEventListenerConfiguration configuration;

    @Override
    public String getName()
    {
        return "ircbrokenlink";
    }

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(new InvalidURLEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        if (this.bot.isConnected() && this.configuration.isActive()) {
            Map<String, Object> brokenLinkData = (Map<String, Object>) source;
            String linkURL = (String) brokenLinkData.get("url");
            String linkSource = (String) brokenLinkData.get("source");
            LinkState linkState = (LinkState) brokenLinkData.get("state");
            int responseCode = linkState.getResponseCode();

            String message = String.format("Invalid link found %s on page %s (code = %s)",
                linkURL, linkSource, responseCode);
            this.bot.sendMessage(this.bot.getChannelsNames().iterator().next(), message);
        }
    }
}
