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
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections.Buffer;
import org.apache.commons.collections.BufferUtils;
import org.apache.commons.collections.buffer.CircularFifoBuffer;
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
     * Property name for the broken link URL in broken link data map.
     */
    private static final String URL = "url";

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

    /**
     * Save the last 4 broken links so that they can be printed quickly on the IRC Channel by the Broken Links Bot
     * Listener.
     */
    private Buffer lastBrokenLinks = BufferUtils.synchronizedBuffer(new CircularFifoBuffer(4));

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

            // Make sure we don't save duplicates (this is because every time a page with a broken link is rendered
            // an event is sent so we can receive several events for the same broken link
            String linkURL = (String) brokenLinkData.get(URL);
            removeDuplicateLinkData(linkURL);

            // Save the broken link data for later retrieval by the Broken Links Bot Listener.
            this.lastBrokenLinks.add(brokenLinkData);

            String linkSource = (String) brokenLinkData.get("source");
            LinkState linkState = (LinkState) brokenLinkData.get("state");
            int responseCode = linkState.getResponseCode();

            // Get Link extra data and serialize them
            String dataString = getSerializedLinkData((Map<String, Object>) brokenLinkData.get("contextData"));

            String message;
            if (dataString.length() > 0) {
                message = String.format("Invalid link %s on page %s (code = %s, %s)", linkURL, linkSource,
                    responseCode, dataString);
            } else {
                message = String.format("Invalid link %s on page %s (code = %s)", linkURL, linkSource, responseCode);
            }

            // Get the channel to which to send to. If there's no channel name it means the Bot hasn't joined
            // any channel yet so don't do anything!
            Iterator<String> channelNameItator = this.bot.getChannelsNames().iterator();
            if (channelNameItator.hasNext()) {
                this.bot.sendMessage(channelNameItator.next(), message);
            }
        }
    }

    /**
     * @param contextData the extra context data to serialize
     * @return the serialized version of context data, ready to be printed to the user, separated by commas
     *         (eg "param1 = value1, param2 = value2")
     */
    private String getSerializedLinkData(Map<String, Object> contextData)
    {
        StringBuilder dataString = new StringBuilder();
        if (contextData != null) {
            Iterator<Map.Entry<String, Object>> it = contextData.entrySet().iterator();
            while (it.hasNext()) {
                Map.Entry<String, Object> entry = it.next();
                dataString.append(entry.getKey()).append(" = ").append(entry.getValue());
                if (it.hasNext()) {
                    dataString.append(", ");
                }
            }
        }
        return dataString.toString();
    }

    /**
     * @param linkURL the link url for which we check if it's already saved in the Buffer and if so we remove it
     */
    private void removeDuplicateLinkData(String linkURL)
    {
        Iterator it = this.lastBrokenLinks.iterator();
        Map<String, Object> duplicateBrokenLinkData = null;
        while (it.hasNext()) {
            Map<String, Object> savedLinkData = (Map<String, Object>) it.next();
            String savedLinkURL = (String) savedLinkData.get(URL);
            if (linkURL.equals(savedLinkURL)) {
                duplicateBrokenLinkData = savedLinkData;
                break;
            }
        }
        if (duplicateBrokenLinkData != null) {
            this.lastBrokenLinks.remove(duplicateBrokenLinkData);
        }
    }

    /**
     * @return the latest broken links found in the wiki
     */
    // TODO: Expose this through a user-public interface since this is currently in the internal package only!
    public Buffer getLastBrokenLinks()
    {
        return BufferUtils.unmodifiableBuffer(this.lastBrokenLinks);
    }
}
