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

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.collections.Buffer;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.observation.EventListener;
import org.xwiki.rendering.transformation.linkchecker.LinkState;

/**
 * Send the last broken links found in the wiki on the IRC channel.
 *
 * @param <T> the reference to the PircBotX instance
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("brokenlink")
@Singleton
public class BrokenLinksBotListener<T extends PircBotX> extends ListenerAdapter<T> implements IRCBotListener<T>
{
    /**
     * The command to type in the IRC channel to trigger this listener.
     */
    private static final String COMMAND = "!bl";

    /**
     * Used to get the last broken links found in the wiki.
     */
    @Inject
    @Named("ircbrokenlink")
    private EventListener brokenLinkEventListener;

    @Override
    public String getName()
    {
        return "Broken Links";
    }

    @Override
    public String getDescription()
    {
        return String.format("%s: lists the latest broken links that have been found in the wiki", COMMAND);
    }

    @Override
    public void onMessage(MessageEvent<T> event) throws Exception
    {
        if (event.getMessage().startsWith(COMMAND)) {
            Buffer latestBrokenLinks = ((BrokenLinkEventListener) this.brokenLinkEventListener).getLastBrokenLinks();
            if (latestBrokenLinks.size() > 0) {
                event.respond("Latest broken links:");
                Iterator it = latestBrokenLinks.iterator();
                while (it.hasNext()) {
                    Map<String, Object> linkData = (Map<String, Object>) it.next();
                    String linkURL = (String) linkData.get("url");
                    String linkSource = (String) linkData.get("source");
                    LinkState linkState = (LinkState) linkData.get("state");
                    int responseCode = linkState.getResponseCode();

                    String message = String.format("%s on page %s (code = %s)", linkURL, linkSource, responseCode);
                    event.respond(message);
                }
            } else {
                event.respond("No broken links found so far or the Link Checker transformation is not active...");
            }
        }
    }
}
