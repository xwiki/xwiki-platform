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
package org.xwiki.mentions.internal;

import java.util.Collections;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.MentionNotificationService;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.ObservationManager;

/**
 * Default implementation of {@link MentionNotificationService}.
 *
 * @version $Id$
 * @since 12.5RC1
 */
@Component
@Singleton
public class DefaultMentionNotificationService implements MentionNotificationService
{
    @Inject
    private ObservationManager observationManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private QuoteService quote;

    @Inject
    private MentionsConfiguration configuration;

    @Override
    public void sendNotification(MentionNotificationParameters mentionNotificationParameters)
    {
        String extractedQuote;
        if (this.configuration.isQuoteActivated()) {
            extractedQuote = this.quote.extract(mentionNotificationParameters.getXdom(),
                mentionNotificationParameters.getAnchorId()).orElse(null);
        } else {
            extractedQuote = null;
        }

        MentionEventParams params = new MentionEventParams()
                                        .setUserReference(this.serializer.serialize(
                                            mentionNotificationParameters.getAuthorReference()))
                                        .setDocumentReference(this.serializer.serialize(
                                            mentionNotificationParameters.getDocumentReference()))
                                        .setLocation(mentionNotificationParameters.getLocation())
                                        .setAnchor(mentionNotificationParameters.getAnchorId())
                                        .setQuote(extractedQuote);
        MentionEvent event =
            new MentionEvent(
                Collections.singleton(this.serializer.serialize(mentionNotificationParameters.getMentionedIdentity())),
                params);
        this.observationManager.notify(event, "org.xwiki.contrib:mentions-notifications", MentionEvent.EVENT_TYPE);
    }
}
