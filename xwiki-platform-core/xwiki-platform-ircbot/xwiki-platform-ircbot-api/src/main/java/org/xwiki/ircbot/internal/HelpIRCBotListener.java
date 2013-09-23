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

import java.lang.reflect.Type;
import java.util.Collections;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.pircbotx.PircBotX;
import org.pircbotx.hooks.ListenerAdapter;
import org.pircbotx.hooks.events.MessageEvent;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ircbot.IRCBotListener;

/**
 * Finds all other Bot Listeners and send their description to the IRC Channel allowing a user to see all the actions
 * that can be done.
 *
 * @param <T> the reference to the PircBotX instance
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("help")
@Singleton
public class HelpIRCBotListener<T extends PircBotX> extends ListenerAdapter<T> implements IRCBotListener<T>
{
    /**
     * The command to type in the IRC channel to trigger this listener.
     */
    private static final String COMMAND = "!help";

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to find all Bot listeners available.
     */
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Override
    public String getName()
    {
        return "Help";
    }

    @Override
    public String getDescription()
    {
        return String.format("%s: List all commands available", COMMAND);
    }

    @Override
    public void onMessage(MessageEvent<T> event) throws Exception
    {
        if (event.getMessage().startsWith(COMMAND)) {
            event.respond("Available Bot listeners:");
            for (IRCBotListener listener : getIRCBotListeners()) {
                event.respond(" - " + listener.getDescription());
            }
        }
    }

    /**
     * @return all the available Bot listeners
     */
    private List<IRCBotListener> getIRCBotListeners()
    {
        List<IRCBotListener> result;
        try {
            result = this.componentManager.getInstanceList((Type) IRCBotListener.class);
        } catch (ComponentLookupException e) {
            this.logger.warn("Failed to look up IRC Bot Listeners", e);
            result = Collections.emptyList();
        }
        return result;
    }
}
