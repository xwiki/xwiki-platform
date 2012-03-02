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
import org.xwiki.ircbot.wiki.WikiIRCBotManager;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.EntityReferenceValueProvider;
import org.xwiki.script.service.ScriptService;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;

/**
 * Allows scripts to easily access IRC Bot APIs.
 *
 * @version $Id$
 * @since 4.0M1
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

    private static final String PR_REQUIRED = "This action requires Programming Rights";

    /**
     * Provides access to the current context.
     */
    @Inject
    private Execution execution;

    @Inject
    private WikiIRCBotManager botManager;

    @Inject
    @Named("wiki")
    private ComponentManager componentManager;

    @Inject
    private EntityReferenceValueProvider valueProvider;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    public void start()
    {
        if (hasPermission()) {
            try {
                this.botManager.startBot();
            } catch (IRCBotException e) {
                setError(e);
            }
        } else {
            setError(new IRCBotException(PR_REQUIRED));
        }
    }

    public void stop()
    {
        if (hasPermission()) {
            try {
                this.botManager.stopBot();
            } catch (IRCBotException e) {
                setError(e);
            }
        } else {
            setError(new IRCBotException(PR_REQUIRED));
        }
    }

    public boolean isStarted()
    {
        return this.botManager.isBotStarted();
    }

    public Map<BotListenerData, Boolean> getBotListenerStatuses()
    {
        Map<BotListenerData, Boolean> statuses = new HashMap<BotListenerData, Boolean>();

        try {
            for (BotListenerData listenerData : this.botManager.getBotListenerData()) {
                statuses.put(listenerData,
                    this.componentManager.hasComponent(IRCBotListener.class, listenerData.getId()));
            }
        } catch (IRCBotException e) {
            statuses = null;
        }

        return statuses;
    }

    public Map<String, Object> getContext()
    {
        return (Map<String, Object>) getXWikiContext().get(WikiIRCBotListener.LISTENER_XWIKICONTEXT_PROPERTY);
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
     * Utility method for accessing XWikiContext.
     *
     * @return the XWikiContext.
     */
    private XWikiContext getXWikiContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    private boolean hasPermission()
    {
        boolean hasPermission = false;

        XWikiContext context = getXWikiContext();
        DocumentReference userReference = context.getUserReference();

        if (userReference != null) {
            String mainWiki = this.valueProvider.getDefaultValue(EntityType.WIKI);

            // Check if the current user is logged on the main wiki.
            if (userReference.getWikiReference().getName().equals(mainWiki))
            {
                DocumentReference mainXWikiPreferencesReference =
                    new DocumentReference(mainWiki, "XWiki", "XWikiPreferences");

                // Check if the user has Admin rights on the main wiki
                try {
                    hasPermission = context.getWiki().getRightService().hasAccessLevel("admin",
                        this.entityReferenceSerializer.serialize(userReference),
                        this.entityReferenceSerializer.serialize(mainXWikiPreferencesReference), context);
                } catch (XWikiException e) {
                    // Don't allow access (permission is false by default)
                }
            }
        }

        return hasPermission;
    }
}
