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
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.syntax.Syntax;
import org.xwiki.rendering.transformation.Transformation;
import org.xwiki.rendering.transformation.TransformationContext;
import org.xwiki.rendering.transformation.TransformationException;

import com.xpn.xwiki.XWikiContext;

public class WikiIRCBotListener<T extends PircBotX> extends ListenerAdapter<T>
    implements IRCBotListener<T>, WikiIRCBotConstants
{
    public static final String LISTENER_XWIKICONTEXT_PROPERTY = "irclistener";

    private static final Logger LOGGER = LoggerFactory.getLogger(WikiIRCBotListener.class);

    private BotListenerData listenerData;

    private Map<String, XDOM> events;

    private Syntax syntax;

    private Transformation macroTransformation;

    private BlockRenderer plainTextBlockRenderer;

    private WikiIRCModel ircModel;

    public WikiIRCBotListener(BotListenerData listenerData, Map<String, XDOM> events, Syntax syntax,
        Transformation macroTransformation, BlockRenderer plainTextBlockRenderer, WikiIRCModel ircModel)
    {
        this.listenerData = listenerData;
        this.events = events;
        this.syntax = syntax;
        this.macroTransformation = macroTransformation;
        this.plainTextBlockRenderer = plainTextBlockRenderer;
        this.ircModel = ircModel;
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
     * Execute the Wiki bot listener written in wiki syntax by executing the Macros and send the result to the IRC
     * server.
     *
     * @param event the IRC Bot Event
     */
    @Override
    public void onEvent(Event event) throws Exception
    {
        // Get the Event class name, remove the "Event" suffix, and prefix with "on". For example for "MessageEvent"
        // this gives "onMessage".
        // Find the XDOM to execute, using this key.
        String eventName = "on" + StringUtils.removeEnd(event.getClass().getSimpleName(), "Event");

        XDOM xdom = this.events.get(eventName);
        if (xdom != null) {
            // Note that if a Bot Listener script needs access to the IRC Bot (for example to send a message to the
            // IRC channel), it can access it through the "ircbot" Script Service.

            try {
                // Add bindings to the XWiki Context so that the Bot Script Service can access them and thus give access
                // to them to the Bot Listener.
                addBindings(event);

                // Important: we clone the XDOM so that the transformation will not modify it. Otherwise next time
                // this listener runs, it'll simply return the already transformed XDOM.
                XDOM temporaryXDOM = xdom.clone();

                // Execute the Macro Transformation on XDOM and send the result to the IRC server
                TransformationContext txContext = new TransformationContext(temporaryXDOM, this.syntax);
                this.macroTransformation.transform(temporaryXDOM, txContext);
                DefaultWikiPrinter printer = new DefaultWikiPrinter();
                this.plainTextBlockRenderer.render(temporaryXDOM, printer);

                String executionResult = StringUtils.trim(printer.toString());
                if (!StringUtils.isEmpty(executionResult)) {
                    event.respond(printer.toString());
                }
            } catch (IRCBotException e) {
                // An error happened, log a warning and do nothing
                LOGGER.warn(String.format("Failed to execute IRC Bot Listener script [%s]", eventName), e);
            } catch (TransformationException e) {
                // The transformation failed to execute, log an error and continue
                LOGGER.error("Failed to render Wiki IRC Bot Listener script", e);
            }
        }
    }

    private void addBindings(Event event) throws IRCBotException
    {
        Map<String, Object> params = new HashMap<String, Object>();

        // Bind variables
        bindVariable("message", "getMessage", event, params);
        bindVariable("user", "getUser", event, params);
        bindVariable("channel", "getChannel", event, params);
        bindVariable("source", "getUser", event, params);
        bindVariable("reason", "getReason", event, params);
        bindVariable("recipient", "getRecipient", event, params);
        bindVariable("oldNick", "getOldNick", event, params);
        bindVariable("newNick", "getNewNick", event, params);

        // Bind the PircBotX instance
        params.put("bot", event.getBot());

        XWikiContext context = this.ircModel.getXWikiContext();
        if (context != null) {
            context.put(LISTENER_XWIKICONTEXT_PROPERTY, params);
        }
    }

    private void bindVariable(String variableName, String methodName, Event event, Map<String, Object> params)
    {
        try {
            Method getMessageMethod = event.getClass().getMethod(methodName);
            params.put(variableName, getMessageMethod.invoke(event));
        } catch (Exception e) {
            // No such method, don't bind anything
        }
    }
}
