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

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.context.Execution;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.macro.wikibridge.WikiMacroException;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.transformation.Transformation;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of {@link WikiIRCBotListenerFactory}.
 * 
 * @version $Id$
 * @since 4.0M1
 */
@Component
@Singleton
public class DefaultWikiIRCBotListenerFactory implements WikiIRCBotListenerFactory, WikiIRCBotListenerConstants
{
    /**
     * The {@link org.xwiki.component.manager.ComponentManager} component.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The {@link org.xwiki.context.Execution} component used for accessing XWikiContext.
     */
    @Inject
    private Execution execution;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    @Inject
    private IRCBot bot;

    /**
     * Utility method for accessing XWikiContext.
     * 
     * @return the XWikiContext.
     */
    private XWikiContext getContext()
    {
        return (XWikiContext) this.execution.getContext().getProperty("xwikicontext");
    }

    @Override
    public WikiIRCBotListener createWikiListener(DocumentReference documentReference) throws IRCBotException
    {
        XWikiDocument doc;
        try {
            doc = getContext().getWiki().getDocument(documentReference, getContext());
        } catch (XWikiException ex) {
            throw new IRCBotException(String.format("Could not build Bot Listener from: [%s], unable to load document",
                documentReference), ex);
        }
        return buildWikiListener(doc);
    }

    /**
     * Creates a {@link WikiIRCBotListener} from an {@link XWikiDocument} which contains a wiki listener definition.
     * 
     * @param doc the {@link XWikiDocument} to look for a wiki listener definition
     * @return the {@link WikiIRCBotListener} found inside the document
     * @throws WikiMacroException when an invalid listener definition or no listener definition was found
     */
    private WikiIRCBotListener buildWikiListener(XWikiDocument doc) throws IRCBotException
    {
        // Check whether this document contains a listener definition.
        BaseObject listenerDefinition = doc.getXObject(WIKI_BOT_LISTENER_CLASS);

        // Extract listener definition.
        String description = listenerDefinition.getStringValue(DESCRIPTION_PROPERTY);

        // Extract listener events.
        Map<String, XDOM> events = new HashMap<String, XDOM>();
        List<BaseObject> listenerEvents = doc.getXObjects(WIKI_BOT_LISTENER_EVENT_CLASS);
        if (null != listenerEvents) {
            for (BaseObject listenerEvent : listenerEvents) {
                if (null == listenerEvent) {
                    continue;
                }

                // Extract event definition.
                String eventName = listenerEvent.getStringValue(EVENT_NAME_PROPERTY);
                String eventScript = listenerEvent.getStringValue(EVENT_SCRIPT_PROPERTY);

                XDOM eventScriptXDOM;
                try {
                    Parser parser = this.componentManager.lookup(Parser.class, doc.getSyntax().toIdString());
                    eventScriptXDOM = parser.parse(new StringReader(eventScript));
                } catch (ComponentLookupException e) {
                    throw new IRCBotException(
                        String.format("Could not find a parser for the event content [%s]", eventName), e);
                } catch (ParseException e) {
                    throw new IRCBotException(String.format("Error while parsing event script [%s]", eventName), e);
                }

                events.put(eventName, eventScriptXDOM);
            }
        }

        return new WikiIRCBotListener(description, events, doc.getSyntax(), this.macroTransformation,
            this.plainTextBlockRenderer, this.bot);
    }

    @Override
    public boolean containsWikiListener(DocumentReference documentReference)
    {
        boolean result;
        try {
            // Look for a Listener Class
            XWikiDocument doc = getContext().getWiki().getDocument(documentReference, getContext());
            BaseObject listenerDefinition = doc.getXObject(WIKI_BOT_LISTENER_CLASS);
            result = (null != listenerDefinition);

            // Look for a Listener Event Class
            List<BaseObject> listenerEventDefinitions = doc.getXObjects(WIKI_BOT_LISTENER_EVENT_CLASS);
            result = result && (listenerEventDefinitions != null) && (listenerEventDefinitions.size() > 0);
        } catch (XWikiException ex) {
            result = false;
        }
        return result;
    }
}
