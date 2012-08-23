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
package org.xwiki.ircbot.internal;

import java.util.Arrays;
import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.internal.event.XARImportedEvent;
import com.xpn.xwiki.internal.event.XARImportingEvent;

/**
 * Send notifications to an IRC channel when a document is modified in the wiki.
 *
 * @version $Id$
 * @since 4.2M3
 */
@Component
@Named("ircxarimport")
@Singleton
public class XARImportEventListener implements EventListener
{
    /**
     * Key under which we register both the fact that a XAR import has started and in which we also store the
     * number of documents modified.
     */
    public static final String XAR_IMPORT_COUNTER_KEY = "ircxarcounter";

    /**
     * Used to send messages to the Bot and to verify if the Bot is started.
     */
    @Inject
    private IRCBot bot;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to generate a String out of the reference to send the name of the user who started the XAR import.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * Used to get access to the XWiki Context.
     */
    @Inject
    private WikiIRCModel ircModel;

    /**
     * Used to save temporary information about the fact that a XAR import is in progress so that we don't send
     * document modification notifications to the IRC channel, to prevent flooding/spam.
     */
    @Inject
    private Execution execution;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(
                new XARImportingEvent(),
                new XARImportedEvent());
    }

    @Override
    public String getName()
    {
        return "ircxarimport";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Only handle events if the bot is connected
        if (this.bot.isConnected()) {
            try {
                ExecutionContext ec = this.execution.getContext();
                String message;
                if (event instanceof XARImportingEvent) {
                    ec.setProperty(XAR_IMPORT_COUNTER_KEY, 0L);
                    message = String.format("A XAR import has been started by %s", getNotificationAuthor());
                } else {
                    long counter = (Long) ec.getProperty(XAR_IMPORT_COUNTER_KEY);
                    ec.removeProperty(XAR_IMPORT_COUNTER_KEY);
                    message = String.format(
                        "The XAR import started by %s is now finished, %d documents have been imported",
                        getNotificationAuthor(), counter);
                }
                this.bot.sendMessage(this.bot.getChannelsNames().iterator().next(), message);
            } catch (IRCBotException e) {
                // Failed to handle the event, log an error
                this.logger.error("Failed to handle event [{}] for source [{}]", new Object[] {event, source, e});
            }
        }
    }

    /**
     * Get the author name that we want to print in the notification message we send to the IRC channel.
     *
     * @return the author name
     * @throws IRCBotException if we cannot access the XWikiContext
     */
    private String getNotificationAuthor() throws IRCBotException
    {
        DocumentReference authorReference = this.ircModel.getXWikiContext().getUserReference();
        return this.serializer.serialize(authorReference);
    }
}
