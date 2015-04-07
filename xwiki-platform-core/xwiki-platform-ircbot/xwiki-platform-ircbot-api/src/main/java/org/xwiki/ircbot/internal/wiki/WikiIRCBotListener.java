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

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.pircbotx.PircBotX;
import org.pircbotx.hooks.Event;
import org.pircbotx.hooks.ListenerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.IRCBotListener;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.RenderingContext;
import org.xwiki.rendering.transformation.Transformation;

import com.xpn.xwiki.XWikiContext;

/**
 * Represents a dynamic component instance of a Wiki Bot Listener (ie a Listener defined in a Wiki page) that we
 * register against the Component Manager.
 *
 * @param <T> the reference to the PircBotX instance
 *
 * @version $Id$
 * @since 4.0M2
 */
public class WikiIRCBotListener<T extends PircBotX> extends ListenerAdapter<T>
    implements IRCBotListener<T>, WikiIRCBotConstants
{
    /**
     * The variable name under which we save the Event bindings in the XWiki Context.
     * @see #addBindings(org.pircbotx.hooks.Event)
     */
    public static final String LISTENER_XWIKICONTEXT_PROPERTY = "irclistener";

    /**
     * The logger to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(WikiIRCBotListener.class);

    /**
     * @see #WikiIRCBotListener
     */
    private WikiBotListenerData listenerData;

    /**
     * @see #WikiIRCBotListener
     */
    private Map<String, XDOM> events;

    /**
     * @see #WikiIRCBotListener
     */
    private Syntax syntax;

    /**
     * @see #WikiIRCBotListener
     */
    private RenderingContext renderingContext;

    /**
     * @see #WikiIRCBotListener
     */
    private Transformation macroTransformation;

    /**
     * @see #WikiIRCBotListener
     */
    private BlockRenderer plainTextBlockRenderer;

    /**
     * @see #WikiIRCBotListener
     */
    private WikiIRCModel ircModel;

    /**
     * @see #WikiIRCBotListener
     */
    private DocumentReference executingUserReference;

    /**
     * @see #initialize()
     */
    private Map<String, Object> initializationBindings = new HashMap<String, Object>();

    /**
     * @param listenerData the listener data that have been extracted from the wiki page XObject
     * @param events the event scripts that have been extracted from the wiki page XObjects
     * @param syntax the syntax of the wiki page that contained the Bot Listener XObjects
     * @param renderingContext the rendering context that we need to keep updated for right management.
     * @param macroTransformation the macro transformation that we'll run to execute the event scripts
     * @param plainTextBlockRenderer the renderer that we'll use to render the parsed event scripts into plain text.
     *        If the rendering has non empty text then this text is sent to the IRC channel
     * @param ircModel used to access the XWiki Context
     * @param executingUserReference the reference to the user under which the Wiki Bot Listener will executed its
     */
    public WikiIRCBotListener(WikiBotListenerData listenerData, Map<String, XDOM> events, Syntax syntax,
        RenderingContext renderingContext, Transformation macroTransformation, BlockRenderer plainTextBlockRenderer,
        WikiIRCModel ircModel, DocumentReference executingUserReference)
    {
        this.listenerData = listenerData;
        this.events = events;
        this.syntax = syntax;
        this.renderingContext = renderingContext;
        this.macroTransformation = macroTransformation;
        this.plainTextBlockRenderer = plainTextBlockRenderer;
        this.ircModel = ircModel;
        this.executingUserReference = executingUserReference;
    }

    @Override
    public String getName()
    {
        return this.listenerData.getName();
    }

    @Override
    public String getDescription()
    {
        return this.listenerData.getDescription();
    }

    /**
     * Initialize the Bot Listener by calling its "onRegistration" Event Object (if any). This allows the Listener
     * to bind properties to the LISTENER_XWIKICONTEXT_PROPERTY property name and these bindings will be available
     * to the other Event Objects when they execute.
     */
    public void initialize()
    {
        XDOM xdom = this.events.get("onRegistration");
        if (xdom != null) {
            try {
                // Step 1: Execute the onRegistration XProperty content. This allows for example binding variables
                // in the execution context.
                // Note that FTM we don't care about any returned result since onRegistration is usually not supposed
                // to send back anything to the channel.
                executeAsUser(xdom, null);

                // Step 2: Save the bindings that the content could have set so that when other events execute later on
                // they'll have those bindings set. This allow onRegistration content to set some initialization
                // variables.
                XWikiContext context = this.ircModel.getXWikiContext();
                Map<String, Object> bindings = (Map<String, Object>) context.get(LISTENER_XWIKICONTEXT_PROPERTY);
                if (bindings != null) {
                    this.initializationBindings.putAll(bindings);
                }
            } catch (Exception e) {
                getLogger().warn("Failed to initialize Wiki Bot Listener [{}]", getName(), e);
            }
        }
    }

    /**
     * Execute the Wiki bot listener written in wiki syntax by executing the Macros and send the result to the IRC
     * server.
     *
     * @param event the IRC Bot Event
     */
    @Override
    public void onEvent(final Event event)
    {
        // Get the Event class name, remove the "Event" suffix, and prefix with "on". For example for "MessageEvent"
        // this gives "onMessage".
        // Find the XDOM to execute, using this key.
        String eventName = "on" + StringUtils.removeEnd(event.getClass().getSimpleName(), "Event");

        final XDOM xdom = this.events.get(eventName);
        if (xdom != null) {
            LOGGER.debug("Start processing Wiki IRC Bot Listener [{}] for Event [{}], for thread [{}]",
                this.listenerData.getReference(), event, Thread.currentThread());

            // Note that if a Bot Listener script needs access to the IRC Bot (for example to send a message to the
            // IRC channel), it can access it through the "ircbot" Script Service.

            try {
                // Add bindings to the XWiki Context so that the Bot Script Service can access them and thus give access
                // to them to the Bot Listener.
                addBindings(event);

                // Execute the Macro Transformation on XDOM and send the result to the IRC server
                executeAsUser(xdom, event);
            } catch (Exception e) {
                // An error happened, try to send a message to the channel about the error and log a warning.
                try {
                    event.respond(String.format("An error occurred while executing Wiki IRC Bot Listener [%s]. "
                        + "Check your server logs.", this.listenerData.getReference()));
                } catch (Exception nestedException) {
                    // We tried our best, give up!
                }
                getLogger().warn(String.format("Failed to execute IRC Bot Listener script [%s]", eventName), e);
            }

            LOGGER.debug("Stop processing Wiki IRC Bot Listener [{}] for Event [{}], for thread [{}]",
                this.listenerData.getReference(), event, Thread.currentThread());
        }
    }

    /**
     * Apply Macro transformations on the passed XDOM, render it with a plain text renderer and send the result
     * to the IRC channel.
     *
     * @param xdom the XDOM for which to run the Macro transformations
     * @param event the PircBotX event to use to send the message back to the channel
     * @throws Exception in case of an error while rendering the content
     */
    private void executeAsUser(final XDOM xdom, final Event event) throws Exception
    {
        // Execute the Macro Transformation on XDOM and send the result to the IRC server
        // Note: We execute the macros with a current user being the user that was used to register the Wiki
        // Bot Listener. The reason is that the XDOM might use privileged API that require some special rights
        // (like Programming Rights if it contains a Groovy macro for example).
        this.ircModel.executeAsUser(this.executingUserReference, this.listenerData.getReference(),
            new DefaultExecutor(xdom, this.syntax, event, this.renderingContext, this.macroTransformation,
                this.plainTextBlockRenderer));
    }

    /**
     * Bind variables related to the IRC event in the XWiki Context under the {@link #LISTENER_XWIKICONTEXT_PROPERTY}
     * key so that they can be made available to wiki scripts through a Script Service.
     *
     * @param event the IRC event from which to get the data to bind
     * @throws IRCBotException if an error happens when retrieving the XWiki Context
     */
    private void addBindings(Event event) throws IRCBotException
    {
        Map<String, Object> params = new HashMap<String, Object>();

        // Add the initialization bindings
        params.putAll(this.initializationBindings);

        // Bind variables
        bindVariable("getMessage", event, params, "message");
        bindVariable("getUser", event, params, "user", "source");
        bindVariable("getChannel", event, params, "channel");
        bindVariable("getReason", event, params, "reason");
        bindVariable("getRecipient", event, params, "recipient");
        bindVariable("getOldNick", event, params, "oldNick");
        bindVariable("getNewNick", event, params, "newNick");

        // Bind the PircBotX instance
        params.put("bot", event.getBot());

        XWikiContext context = this.ircModel.getXWikiContext();
        context.put(LISTENER_XWIKICONTEXT_PROPERTY, params);
    }

    /**
     * Bind a single variable located in the Event, using reflection.
     *
     * @param methodName the name of the method to call in the Event instance to get the value to bind
     * @param event the event from whih to get the data from
     * @param params the map in which to save the bindings
     * @param variableNames the names of the variable to put in the context and that will contain the result of
     *        executing the Method corresponding to the passed method name
     */
    private void bindVariable(String methodName, Event event, Map<String, Object> params, String... variableNames)
    {
        try {
            Method getMessageMethod = event.getClass().getMethod(methodName);
            Object value = getMessageMethod.invoke(event);
            for (String variableName : variableNames) {
                params.put(variableName, value);
            }
        } catch (Exception e) {
            // No such method, don't bind anything
        }
    }

    /**
     * Useful for unit tests that need to capture logs; they can return a mock logger instead of the real logger and
     * thus assert what's been logged.
     *
     * @return the Logger instance to use to log
     */
    protected Logger getLogger()
    {
        return LOGGER;
    }
}
