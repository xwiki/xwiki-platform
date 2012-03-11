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

import java.lang.reflect.Type;
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
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerFactory;
import org.xwiki.ircbot.wiki.WikiIRCBotManager;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default implementation of {@link WikiIRCBotManager}.
 *
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultWikiIRCBotManager implements WikiIRCBotManager, WikiIRCBotConstants
{
    /**
     * Creates {@link WikiIRCBotListener} objects.
     */
    @Inject
    private WikiIRCBotListenerFactory listenerFactory;

    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentReferenceResolver;

    @Inject
    private IRCBot bot;

    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    @Inject
    private WikiIRCModel ircModel;

    @Inject
    private Provider<List<IRCBotListener>> botListenerComponents;

    @Override
    public void startBot() throws IRCBotException
    {
        if (isBotStarted()) {
            throw new IRCBotException("Bot is already started!");
        }

        // Get configuration data for the Bot
        BotData botData = this.ircModel.loadBotData();

        if (botData.isActive()) {
            // Register Bot Listener components to the Bot
            ListenerManager listenerManager = this.bot.getListenerManager();
            for (IRCBotListener botListener : this.botListenerComponents.get()) {
                listenerManager.addListener(botListener);
            }

            // Register all Wiki Bot Listeners before we start the Bot itself just because we want all Bot Listeners to
            // be ready when the Bot starts so that they can receive all the events and not miss any.
            registerWikiBotListeners();

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
        }
    }

    @Override
    public void stopBot() throws IRCBotException
    {
        if (this.bot.isConnected()) {
            this.bot.disconnect();
            // Wait for the IRC Server to be fully stopped
            while (this.bot.isConnected()) {
                try {
                    Thread.sleep(100L);
                } catch (InterruptedException e) {
                    throw new IRCBotException("Failed to fully wait for IRC client termination", e);
                }
            }
        }

        // Unregister after disconnecting the Bot to allow the listeners to receive all the events till the last moment.
        // Note that Bot Listeners handling event after the disconnection should pay attention not to send anything to
        // the channel since the Bot is stopped!
        unregisterWikiBotListeners();
    }

    @Override
    public void registerWikiBotListener(DocumentReference reference) throws IRCBotException
    {
        // Step 1: Verify if the bot listener is already registered
        String hint = this.compactWikiSerializer.serialize(reference);
        if (!this.componentManager.hasComponent((Type) IRCBotListener.class, hint)) {
            // Step 2: Create the Wiki Bot Listener component if the document has the correct objects
            if (this.listenerFactory.containsWikiListener(reference)) {
                WikiIRCBotListener wikiListener = this.listenerFactory.createWikiListener(reference);
                // Step 3: Register it!
                try {
                    DefaultComponentDescriptor<IRCBotListener> componentDescriptor =
                        new DefaultComponentDescriptor<IRCBotListener>();
                    componentDescriptor.setRoleType(IRCBotListener.class);
                    componentDescriptor.setRoleHint(this.compactWikiSerializer.serialize(reference));
                    this.componentManager.registerComponent(componentDescriptor, wikiListener);
                } catch (ComponentRepositoryException e) {
                    throw new IRCBotException(String.format("Unable to register Wiki IRC Bot Listener in document [%s]",
                        this.compactWikiSerializer.serialize(reference)), e);
                }
                // Step 4: Add the listener to the Bot
                this.bot.getListenerManager().addListener(wikiListener);
            }
        }
    }

    @Override
    public void unregisterWikiBotListener(DocumentReference reference) throws IRCBotException
    {
        String hint = this.compactWikiSerializer.serialize(reference);
        if (this.componentManager.hasComponent((Type) IRCBotListener.class, hint)) {
            IRCBotListener listener;
            try {
                listener = this.componentManager.lookupComponent(IRCBotListener.class, hint);
            } catch (ComponentLookupException e) {
                throw new IRCBotException("Failed to unregister Wiki IRC Bot Listener", e);
            }
            this.componentManager.unregisterComponent((Type) IRCBotListener.class, hint);
            // Remove the listener from the Bot
            this.bot.getListenerManager().removeListener(listener);
        }
    }

    @Override
    public void registerWikiBotListeners() throws IRCBotException
    {
        for (BotListenerData data : this.ircModel.getWikiBotListenerData()) {
            DocumentReference reference = this.currentReferenceResolver.resolve(data.getId());
            registerWikiBotListener(reference);
        }
    }

    @Override
    public void unregisterWikiBotListeners() throws IRCBotException
    {
        for (BotListenerData data : this.ircModel.getWikiBotListenerData()) {
            DocumentReference reference = this.currentReferenceResolver.resolve(data.getId());
            unregisterWikiBotListener(reference);
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
            Map<String, IRCBotListener> botListeners = this.componentManager.lookupMap((Type) IRCBotListener.class);
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
}
