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

import java.util.List;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.mentions.MentionLocation;
import org.xwiki.mentions.MentionsConfiguration;
import org.xwiki.mentions.events.MentionEvent;
import org.xwiki.mentions.events.MentionEventParams;
import org.xwiki.mentions.events.NewMentionsEvent;
import org.xwiki.mentions.notifications.MentionNotificationParameter;
import org.xwiki.mentions.notifications.MentionNotificationParameters;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;
import org.xwiki.rendering.block.XDOM;
import org.xwiki.security.authorization.AuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceResolver;
import org.xwiki.user.UserReferenceSerializer;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.DocumentRevisionProvider;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseStringProperty;
import com.xpn.xwiki.objects.PropertyInterface;

import static java.util.Collections.singleton;
import static java.util.Collections.singletonList;
import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;
import static org.xwiki.mentions.MentionsConfiguration.USER_MENTION_TYPE;

/**
 * Event listener for the mentions to local wiki users.
 *
 * @version $Id$
 * @since 12.10
 */
@Component
@Named(UserMentionEventListener.TYPE)
@Singleton
public class UserMentionEventListener implements EventListener
{
    /**
     * Type of the component.
     */
    public static final String TYPE = "UserMentionEventListener";

    @Inject
    private ObservationManager observationManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    @Inject
    private UserReferenceSerializer<String> userReferenceSerializer;

    @Inject
    private QuoteService quote;

    @Inject
    private MentionsConfiguration configuration;

    @Inject
    private UserReferenceResolver<String> userReferenceResolver;

    @Inject
    private DocumentRevisionProvider documentRevisionProvider;

    @Inject
    private MentionXDOMService xdomService;

    @Inject
    private AuthorizationManager authorizationManager;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private Logger logger;

    @Override
    public String getName()
    {
        return TYPE;
    }

    @Override
    public List<Event> getEvents()
    {
        return singletonList(new NewMentionsEvent());
    }

    @Override
    public void onEvent(Event event, Object source, Object data)
    {
        MentionNotificationParameters mentionNotificationParameters = (MentionNotificationParameters) data;
        // We send notifications to the mentions typed as "user" (representing the local wiki users).
        Set<MentionNotificationParameter> newMentions =
            mentionNotificationParameters.getNewMentions().get(USER_MENTION_TYPE);
        if (newMentions != null) {
            Optional<XWikiDocument> optionalDoc;
            EntityReference entityReference = mentionNotificationParameters.getEntityReference();
            try {
                optionalDoc = Optional.of(this.documentRevisionProvider
                    .getRevision((DocumentReference) entityReference.extractReference(EntityType.DOCUMENT),
                        mentionNotificationParameters.getVersion()));
            } catch (XWikiException e) {
                this.logger.warn("Failed to send the mentions notifications. Cause: [{}]", getRootCauseMessage(e));
                optionalDoc = Optional.empty();
            }
            optionalDoc.ifPresent(doc -> {
                for (MentionNotificationParameter mentionNotificationParameter : newMentions) {
                    handleNotification(mentionNotificationParameters, entityReference, doc,
                        mentionNotificationParameter);
                }
            });
        }
    }

    private void handleNotification(MentionNotificationParameters mentionNotificationParameters,
        EntityReference entityReference, XWikiDocument doc, MentionNotificationParameter mentionNotificationParameter)
    {
        if (canView(mentionNotificationParameter.getReference(), entityReference)) {
            String extractedQuote =
                getQuoteFromXDOM(mentionNotificationParameters, entityReference, doc, mentionNotificationParameter);

            MentionEventParams params = new MentionEventParams()
                .setUserReference(mentionNotificationParameters.getAuthorReference())
                .setDocumentReference(
                    this.entityReferenceSerializer.serialize(entityReference.extractReference(EntityType.DOCUMENT)))
                .setLocation(mentionNotificationParameters.getLocation())
                .setAnchor(mentionNotificationParameter.getAnchorId())
                .setQuote(extractedQuote);
            String mentionedIdentity = mentionNotificationParameter.getReference();

            UserReference userReference = this.userReferenceResolver.resolve(mentionedIdentity,
                entityReference.extractReference(EntityType.WIKI));
            String identity = this.userReferenceSerializer.serialize(userReference);
            MentionEvent event =
                new MentionEvent(singleton(identity), params);
            this.observationManager.notify(event, "org.xwiki.contrib:mentions-notifications", MentionEvent.EVENT_TYPE);
        }
    }

    private boolean canView(String userReference, EntityReference entityReference)
    {
        return this.authorizationManager.hasAccess(Right.VIEW, this.documentReferenceResolver
            .resolve(userReference, entityReference.extractReference(EntityType.WIKI)), entityReference);
    }

    private String getQuoteFromXDOM(MentionNotificationParameters mentionNotificationParameters,
        EntityReference entityReference, XWikiDocument doc, MentionNotificationParameter mentionNotificationParameter)
    {
        String extractedQuote;
        if (this.configuration.isQuoteActivated()) {
            XDOM xdom;
            if (MentionLocation.DOCUMENT.equals(mentionNotificationParameters.getLocation())) {
                xdom = doc.getXDOM();
            } else {
                String name = entityReference.getName();
                PropertyInterface field =
                    doc.getXObject(entityReference.extractReference(EntityType.OBJECT)).getField(name);
                if (field instanceof BaseStringProperty) {
                    xdom =
                        this.xdomService.parse(((BaseStringProperty) field).getValue(), doc.getSyntax()).orElse(null);
                } else {
                    xdom = null;
                }
            }

            if (xdom != null) {
                extractedQuote = this.quote.extract(xdom, mentionNotificationParameter.getAnchorId()).orElse(null);
            } else {
                extractedQuote = null;
            }
        } else {
            extractedQuote = null;
        }
        return extractedQuote;
    }
}
