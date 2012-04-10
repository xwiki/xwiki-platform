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
package org.xwiki.ircbot.internal.wiki;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.hooks.managers.ListenerManager;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.internal.BotData;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerManager;
import org.xwiki.ircbot.wiki.WikiIRCBotManager;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCModel;

/**
 * Default implementation of {@link org.xwiki.ircbot.wiki.WikiIRCBotManager}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultWikiIRCBotManager implements WikiIRCBotManager, WikiIRCBotConstants
{
    /**
     * Used to find all registered IRC Bot listener components in the system.
     */
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * The Bot to start/stop.
     */
    @Inject
    private IRCBot bot;

    /**
     * Used to load Bot configuration data.
     */
    @Inject
    private WikiIRCModel ircModel;

    /**
     * Used to find all Bot Listener Components in the system, in order to register them against the Bot itself.
     */
    @Inject
    private Provider<List<IRCBotListener>> botListenerComponents;

    /**
     * Used to register/unregister Wiki IRC Bot listeners.
     */
    @Inject
    private WikiIRCBotListenerManager botListenerManager;

    @Override
    public void startBot(boolean updateBotStatus) throws IRCBotException
    {
        if (isBotStarted()) {
            throw new IRCBotException("Bot is already started!");
        }

        // Get configuration data for the Bot
        BotData botData = this.ircModel.loadBotData();

        if (botData.isActive() || updateBotStatus) {
            // Register Bot Listener components to the Bot
            ListenerManager listenerManager = this.bot.getListenerManager();
            for (IRCBotListener botListener : this.botListenerComponents.get()) {
                listenerManager.addListener(botListener);
            }

            // Register all Wiki Bot Listeners before we start the Bot itself just because we want all Bot Listeners to
            // be ready when the Bot starts so that they can receive all the events and not miss any.
            this.botListenerManager.registerWikiBotListeners();

            // Connect to server if not already connected
            if (!this.bot.isConnected()) {
                this.bot.setName(botData.getName());
                try {
                    this.bot.connect(botData.getServer());
                } catch (Exception e) {
                    throw new IRCBotException(
                        String.format("Failed to connect to IRC server [%s]", botData.getServer()), e);
                }

                // Identify if a password is set
                if (!StringUtils.isEmpty(botData.getPassword())) {
                    this.bot.identify(botData.getPassword());
                }
            }

            // Join channel
            this.bot.joinChannel(botData.getChannel());

            // Mark the Bot as active if it's not already and updateBotStatus is true
            if (updateBotStatus && !botData.isActive()) {
                this.ircModel.setActive(true);
            }

        }
    }

    @Override
    public void stopBot(boolean updateBotStatus) throws IRCBotException
    {
        if (!isBotStarted()) {
            throw new IRCBotException("Bot is already stopped!");
        }

        this.bot.disconnect();

        // Wait for the IRC Server to be fully stopped
        while (this.bot.isConnected()) {
            try {
                Thread.sleep(100L);
            } catch (InterruptedException e) {
                throw new IRCBotException("Failed to fully wait for IRC client termination", e);
            }
        }

        // Unregister after disconnecting the Bot to allow the listeners to receive all the events till the last moment.
        // Note that Bot Listeners handling event after the disconnection should pay attention not to send anything to
        // the channel since the Bot is stopped!
        this.botListenerManager.unregisterWikiBotListeners();

        // Mark the Bot as inactive
        if (updateBotStatus) {
            this.ircModel.setActive(false);
        }
    }

    @Override
    public boolean isBotStarted()
    {
        return this.bot.isConnected();
    }

    @Override
    public List<BotListenerData> getBotListenerData() throws IRCBotException
    {
        Map<String, BotListenerData> data = new HashMap<String, BotListenerData>();

        // Step 1: Look for all wiki bot listeners in the wiki
        for (BotListenerData listenerData : this.ircModel.getWikiBotListenerData()) {
            data.put(listenerData.getId(), listenerData);
        }

        // Step 2: Look for all registered bot listeners in the component manager but not already in the data structure
        try {
            Map<String, IRCBotListener> botListeners = this.componentManager.getInstanceMap(IRCBotListener.class);
            for (Map.Entry<String, IRCBotListener> entry : botListeners.entrySet()) {
                BotListenerData listenerData =
                    new BotListenerData(entry.getKey(), entry.getValue().getName(), entry.getValue().getDescription());
                if (!data.containsKey(entry.getKey())) {
                    data.put(entry.getKey(), listenerData);
                }
            }
        } catch (ComponentLookupException e) {
            throw new IRCBotException("Failed to lookup IRC Bot Listeners", e);
        }

        return new ArrayList<BotListenerData>(data.values());
    }

    @Override
    public Map<String, Object> getContext() throws IRCBotException
    {
        return (Map<String, Object>) this.ircModel.getXWikiContext().get(
            WikiIRCBotListener.LISTENER_XWIKICONTEXT_PROPERTY);
    }
}
