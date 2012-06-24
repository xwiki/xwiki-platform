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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.descriptor.DefaultComponentDescriptor;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.component.manager.ComponentRepositoryException;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerFactory;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerManager;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;

/**
 * Default implementation of {@link org.xwiki.ircbot.wiki.WikiIRCBotManager}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultWikiIRCBotListenerManager implements WikiIRCBotListenerManager, WikiIRCBotConstants
{
    /**
     * Creates {@link org.xwiki.ircbot.internal.wiki.WikiIRCBotListener} objects.
     */
    @Inject
    private WikiIRCBotListenerFactory listenerFactory;

    /**
     * The Component Manager against which to register/unregister Wiki Bot listeners.
     */
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    /**
     * Used to transform a wiki page name into a proper Document Reference.
     */
    @Inject
    @Named("current")
    private DocumentReferenceResolver<String> currentReferenceResolver;

    /**
     * Used to register the Wiki IRC Bot Listener against the Bot itself.
     */
    @Inject
    private IRCBot bot;

    /**
     * Used to transform the wiki page reference where the Wiki Bot Listener is defined into a Component Role Hint.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> compactWikiSerializer;

    /**
     * Used to retrieve all Wiki IRC Bot Listener data from wiki pages.
     */
    @Inject
    private WikiIRCModel ircModel;

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
                    componentDescriptor.setRoleHint(hint);
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
                listener = this.componentManager.getInstance(IRCBotListener.class, hint);
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
}
