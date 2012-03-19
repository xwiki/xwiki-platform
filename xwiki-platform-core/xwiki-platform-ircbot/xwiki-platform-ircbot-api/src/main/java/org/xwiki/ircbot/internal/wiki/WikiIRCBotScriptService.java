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
import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.internal.BotListenerData;
import org.xwiki.ircbot.wiki.WikiIRCBotManager;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;

/**
 * Allows scripts to easily access IRC Bot APIs.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("ircbot")
@Singleton
public class WikiIRCBotScriptService implements ScriptService
{
    /**
     * The key under which the last encountered error is stored in the current execution context.
     */
    private static final String ERROR_KEY = "scriptservice.ircbot.error";

    /**
     * Message to display when the user doesn't have the permission to start/stop the bot.
     */
    private static final String RIGHTS_REQUIRED = "This action requires to be logged on the main wiki and to have "
        + "Admin rights there.";

    /**
     * Allows starting/stopping the Bot and other actions on the Bot.
     */
    @Inject
    private WikiIRCBotManager wikiBotManager;

    /**
     * Used to find which Bot Listeners are registered (ie active).
     */
    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    /**
     * Used to find the name of the main wiki.
     */
    @Inject
    private EntityReferenceValueProvider valueProvider;

    /**
     * Used to serialize references because of old APIs taking only Strings.
     */
    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Used to get the XWiki Context.
     */
    @Inject
    private WikiIRCModel ircModel;

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    /**
     * @param updateBotStatus see {@link WikiIRCBotManager#startBot(boolean)}
     */
    public void start(boolean updateBotStatus)
    {
        if (hasPermission()) {
            try {
                this.wikiBotManager.startBot(updateBotStatus);
            } catch (IRCBotException e) {
                setError(e);
            }
        } else {
            setError(new IRCBotException(RIGHTS_REQUIRED));
        }
    }

    /**
     * @param updateBotStatus see {@link WikiIRCBotManager#stopBot(boolean)}
     */
    public void stop(boolean updateBotStatus)
    {
        if (hasPermission()) {
            try {
                this.wikiBotManager.stopBot(updateBotStatus);
            } catch (IRCBotException e) {
                setError(e);
            }
        } else {
            setError(new IRCBotException(RIGHTS_REQUIRED));
        }
    }

    /**
     * @return see {@link WikiIRCBotManager#isBotStarted()}
     */
    public boolean isStarted()
    {
        return this.wikiBotManager.isBotStarted();
    }

    /**
     * Provides information about all Bot Listeners (whether they are Wiki Bot Listeners or standard Java components)
     * along with their status (i.e. whether they're active or not).
     *
     * @return the information about all Bot Listeners (such as id, name, description, etc) and their status
     */
    public Map<BotListenerData, Boolean> getBotListenerStatuses()
    {
        Map<BotListenerData, Boolean> statuses = new HashMap<BotListenerData, Boolean>();

        try {
            for (BotListenerData listenerData : this.wikiBotManager.getBotListenerData()) {
                statuses.put(listenerData,
                    this.componentManager.hasComponent((Type) IRCBotListener.class, listenerData.getId()));
            }
        } catch (IRCBotException e) {
            statuses = null;
        }

        return statuses;
    }

    /**
     * @return see {@link WikiIRCBotManager#getContext()}
     */
    public Map<String, Object> getContext()
    {
        Map<String, Object> context;
        try {
            context = this.wikiBotManager.getContext();
        } catch (IRCBotException e) {
            context = null;
        }
        return context;
    }

    /**
     * Get the error generated while performing the previously called action.
     *
     * @return the last exception or {@code null} if no exception was thrown
     */
    public Exception getLastError()
    {
        return (Exception) this.execution.getContext().getProperty(ERROR_KEY);
    }

    /**
     * Store a caught exception in the context, so that it can be later retrieved using {@link #getLastError()}.
     *
     * @param exception the exception to store, can be {@code null} to clear the previously stored exception
     * @see #getLastError()
     */
    private void setError(Exception exception)
    {
        this.execution.getContext().setProperty(ERROR_KEY, exception);
    }

    /**
     * @return true if the user is allowed to start/stop the Bot ie if the user is logged on the main wiki and has
     *         Admin rights on the main wiki
     */
    public boolean hasPermission()
    {
        boolean hasPermission = false;

        try {
            XWikiContext context = this.ircModel.getXWikiContext();
            DocumentReference userReference = context.getUserReference();

            if (userReference != null) {
                String mainWiki = this.valueProvider.getDefaultValue(EntityType.WIKI);

                // Check if the current user is logged on the main wiki.
                if (userReference.getWikiReference().getName().equals(mainWiki))
                {
                    DocumentReference mainXWikiPreferencesReference =
                        new DocumentReference(mainWiki, "XWiki", "XWikiPreferences");

                    // Check if the user has Admin rights on the main wiki
                    hasPermission = context.getWiki().getRightService().hasAccessLevel("admin",
                        this.entityReferenceSerializer.serialize(userReference),
                        this.entityReferenceSerializer.serialize(mainXWikiPreferencesReference), context);
                }
            }
        } catch (Exception e) {
            // Don't allow access (permission is false by default)
        }

        return hasPermission;
    }
}
