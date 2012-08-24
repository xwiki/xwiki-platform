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
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCBotConstants;
import org.xwiki.ircbot.wiki.WikiIRCBotListenerFactory;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.rendering.parser.ParseException;
import org.xwiki.rendering.parser.Parser;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.transformation.Transformation;

import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

/**
 * The default implementation of {@link org.xwiki.ircbot.wiki.WikiIRCBotListenerFactory}.
 * 
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultWikiIRCBotListenerFactory implements WikiIRCBotListenerFactory, WikiIRCBotConstants
{
    /**
     * The {@link org.xwiki.component.manager.ComponentManager} component.
     */
    @Inject
    private ComponentManager componentManager;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to execute the Bot Listener's event scripts.
     */
    @Inject
    @Named("macro")
    private Transformation macroTransformation;

    /**
     * Used to execute the Bot Listener's event scripts.
     */
    @Inject
    @Named("plain/1.0")
    private BlockRenderer plainTextBlockRenderer;

    /**
     * Used to compute the id for a Wiki Bot Listener (it's the serialized form of the page containing the Objects).
     * Note that we use a compact serialization since Wiki Bot Listeners are registered for a given wiki only.
     */
    @Inject
    @Named("compactwiki")
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Provides APIs to easily access data stored in wiki pages.
     */
    @Inject
    private WikiIRCModel ircModel;

    @Override
    public WikiIRCBotListener createWikiListener(DocumentReference documentReference) throws IRCBotException
    {
        XWikiDocument doc = this.ircModel.getDocument(documentReference);

        // Check whether this document contains a listener definition.
        BaseObject listenerDefinition = doc.getXObject(WIKI_BOT_LISTENER_CLASS);

        // Extract listener definition.
        String name = listenerDefinition.getStringValue(NAME_PROPERTY);
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
                    Parser parser = this.componentManager.getInstance(Parser.class, doc.getSyntax().toIdString());
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

        WikiBotListenerData botListenerData = new WikiBotListenerData(documentReference,
            this.entityReferenceSerializer.serialize(doc.getDocumentReference()), name, description);

        // Note: We save the current user since the Wiki Bot listeners will execute on the PircBotX threads. As a
        // consequence they'll need to set that user as the current user when the Wiki Bot Listener receives an event
        // and renders its XDOM. The reason is that the XDOM might use privileged API that require some special rights
        // (like Programming Rights if it contains a Groovy macro for example).
        WikiIRCBotListener listener = new WikiIRCBotListener(botListenerData, events, doc.getSyntax(),
            this.macroTransformation, this.plainTextBlockRenderer, this.ircModel,
            this.ircModel.getXWikiContext().getUserReference());

        // Call Wiki Bot Listener initialization.
        listener.initialize();

        return listener;
    }

    @Override
    public boolean containsWikiListener(DocumentReference documentReference)
    {
        boolean result;
        try {
            // Look for a Listener Class
            XWikiDocument doc = this.ircModel.getDocument(documentReference);
            BaseObject listenerDefinition = doc.getXObject(WIKI_BOT_LISTENER_CLASS);
            result = (null != listenerDefinition);

            // Only return true if the listener is active
            result = result && (listenerDefinition.getIntValue(INACTIVE_PROPERTY) != 1);

            // Look for a Listener Event Class
            List<BaseObject> listenerEventDefinitions = doc.getXObjects(WIKI_BOT_LISTENER_EVENT_CLASS);
            result = result && (listenerEventDefinitions != null) && (listenerEventDefinitions.size() > 0);
        } catch (Exception ex) {
            result = false;
        }
        return result;
    }
}
