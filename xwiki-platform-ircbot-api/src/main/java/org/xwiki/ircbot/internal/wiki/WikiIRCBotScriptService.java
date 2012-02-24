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

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCBotManager;
import org.xwiki.script.service.ScriptService;

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
    private DocumentAccessBridge bridge;

    public void start()
    {
        // For protection we're requiring Programming Rights to start the Bot
        if (this.bridge.hasProgrammingRights()) {
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
        // For protection we're requiring Programming Rights to start the Bot
        if (this.bridge.hasProgrammingRights()) {
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
}
