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
import java.util.Iterator;
import java.util.List;
import java.util.regex.Pattern;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.ircbot.DocumentModifiedEventListenerConfiguration;
import org.xwiki.ircbot.IRCBot;
import org.xwiki.ircbot.IRCBotException;
import org.xwiki.ircbot.wiki.WikiIRCModel;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.event.Event;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Send notifications to an IRC channel when a document is modified in the wiki.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("ircdocumentmodified")
@Singleton
public class DocumentModifiedEventListener implements EventListener
{
    /**
     * Used to send messages to the Bot and to verify if the Bot is started.
     */
    @Inject
    private IRCBot bot;

    /**
     * Configuration data for this listener.
     */
    @Inject
    private DocumentModifiedEventListenerConfiguration configuration;

    /**
     * Used to generate a String out of the reference to the Document in received Events so that it can be compared
     * against exclusion Patterns to decide whether or not to send a message to the IRC channel for the received event.
     */
    @Inject
    private EntityReferenceSerializer<String> serializer;

    /**
     * The logger to log.
     */
    @Inject
    private Logger logger;

    /**
     * Used to get access to the XWiki Context.
     */
    @Inject
    private WikiIRCModel ircModel;

    /**
     * Used to get temporary information about the fact that a XAR import is in progress so that we don't send
     * document modification notifications to the IRC channel, to prevent flooding/spam.
     */
    @Inject
    private Execution execution;

    @Override
    public List<Event> getEvents()
    {
        return Arrays.<Event>asList(
            new DocumentUpdatedEvent(),
            new DocumentDeletedEvent(),
            new DocumentCreatedEvent());
    }

    @Override
    public String getName()
    {
        return "ircdocumentmodified";
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        // Only send an event if the bot is connected and the Event has a source being a XWikiDocument.
        if (this.bot.isConnected() && source instanceof XWikiDocument && data instanceof XWikiContext) {
            XWikiDocument document = (XWikiDocument) source;
            XWikiContext xcontext = (XWikiContext) data;
            DocumentReference reference = document.getDocumentReference();
            String referenceAsString = this.serializer.serialize(reference);

            try {
                // Send notification to the IRC channel if we're allowed.
                if (shouldSendNotification(referenceAsString)) {
                    String message = String.format("%s was %s by %s%s - %s",
                        referenceAsString,
                        getNotificationAction(event),
                        getNotificationAuthor(xcontext),
                        getNotificationComment(document),
                        getNotificationURL(event, document, xcontext));

                    // Get the channel to which to send to. If there's no channel name it means the Bot hasn't joined
                    // any channel yet so don't do anything!
                    Iterator<String> channelNameItator = this.bot.getChannelsNames().iterator();
                    if (channelNameItator.hasNext()) {
                        this.bot.sendMessage(channelNameItator.next(), message);
                    }
                }
            } catch (Exception e) {
                // Failed to handle the event, log an error
                this.logger.error("Failed to send IRC notification for document [{}], event [{}] and data [{}]",
                    reference, event, data, e);
            }
        }
    }

    /**
     * Decides if we should send a notification in the IRC channel or not. We don't send if there are some
     * defined exclusions (an example is to not notify when the IRC Archive documents are modified since that would
     * cause an infinite loop!) or if a XAR import is in progress (to prevent flooding/spam).
     *
     * @param referenceAsString the reference to the modified document as a String (eg "wiki:space.page")
     * @return true if we should send notifications on the IRC channel or false otherwise
     * @throws IRCBotException if there's been an error getting exclusion patterns
     */
    private boolean shouldSendNotification(String referenceAsString) throws IRCBotException
    {
        boolean shouldSendNotification = true;

        // Don't send notifications if a XAR import is in progress
        ExecutionContext ec = this.execution.getContext();
        Object importCounterObject = ec.getProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY);
        if (importCounterObject != null) {
            long newCounter = (Long) importCounterObject;
            newCounter++;
            ec.setProperty(XARImportEventListener.XAR_IMPORT_COUNTER_KEY, newCounter);
            shouldSendNotification = false;
        } else {
            // Verify if we should send this notification (i.e. that it's not in the exclusion list).
            // For example we may not want to send notifications when the Log Listener modifies an IRC Archive
            // since that would cause an infinite loop...
            for (Pattern pattern : this.configuration.getExclusionPatterns()) {
                if (pattern.matcher(referenceAsString).matches()) {
                    shouldSendNotification = false;
                    break;
                }
            }
        }

        return shouldSendNotification;
    }

    /**
     * Get the author name that we want to print in the notification message we send to the IRC channel.
     *
     * @param xcontext the XWiki Context from which we extract the current user
     * @return the author name
     * @throws IRCBotException if we cannot access the XWikiContext
     */
    private String getNotificationAuthor(XWikiContext xcontext) throws IRCBotException
    {
        String user;

        DocumentReference userReference = xcontext.getUserReference();
        if (userReference != null) {
            user = this.serializer.serialize(userReference);
        } else {
            user = "Guest";
        }

        return user;
    }

    /**
     * Get the action on the page (created, deleted, modified).
     *
     * @param event the XWiki Document event
     * @return the action (e.g. "created")
     */
    private String getNotificationAction(Event event)
    {
        String action;
        if (event instanceof DocumentDeletedEvent) {
            action = "deleted";
        } else if (event instanceof DocumentCreatedEvent) {
            action = "created";
        } else {
            action = "modified";
        }
        return action;
    }

    /**
     * Get a comment part that we want to print in the notification message we send to the IRC channel.
     *
     * @param source the source document from the Document event
     * @return the comment part
     */
    private String getNotificationComment(XWikiDocument source)
    {
        String comment;
        if (!StringUtils.isEmpty(source.getComment())) {
            comment = String.format(" (%s)", source.getComment());
        } else {
            comment = "";
        }
        return comment;
    }

    /**
     * Get the URL that we want to print in the notification message we send to the IRC channel.
     *
     * @param event the XWiki Document event
     * @param source the source document from the Document event
     * @param xcontext the XWiki Context that we use to compute the external URL
     * @return the notification URL
     * @throws IRCBotException if we cannot access the XWikiContext
     */
    private String getNotificationURL(Event event, XWikiDocument source, XWikiContext xcontext) throws IRCBotException
    {
        String url;

        try {
            String queryString = null;
            if (!(event instanceof DocumentCreatedEvent || event instanceof DocumentDeletedEvent)) {
                // Return a diff URL since the action done was a modification
                queryString = String.format("viewer=changes&amp;rev2=%s", source.getVersion());
            }
            url = source.getExternalURL("view", queryString, xcontext);
        } catch (Exception e) {
            // Ensures that an error in computing the URL won't prevent sending a message on the IRC channel
            url = "Failed to compute URL";
            this.logger.debug("Failed to compute URL for Document Modified Event Listener", e);
        }

        return url;
    }
}
