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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;

import org.slf4j.Logger;
import org.xwiki.bridge.DocumentAccessBridge;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.notifiers.email.NotificationEmailRenderer;
import org.xwiki.notifications.notifiers.internal.EventsSorter;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

import com.xpn.xwiki.api.Attachment;

/**
 * Abstract iterator for sending MIME notification messages (usually emails).
 *
 * @since 9.6RC1
 * @version $Id$
 */
public abstract class AbstractMimeMessageIterator implements Iterator<MimeMessage>, Iterable<MimeMessage>
{
    private static final String EVENTS = "events";

    private static final String HTML_EVENTS = "htmlEvents";

    private static final String PLAIN_TEXT_EVENTS = "plainTextEvents";

    private static final String SORTED_EVENTS = "sortedEvents";

    private static final String EMAIL_PROPERTY = "email";

    private static final String FROM = "from";

    private static final String TO = "to";

    private static final String VELOCITY_VARIABLES = "velocityVariables";

    private static final String ERROR_MESSAGE = "Failed to generate an email for the user [{}].";

    private static final String ATTACHMENTS = "attachments";

    @Inject
    protected Logger logger;

    @Inject
    protected EntityReferenceSerializer<String> serializer;

    @Inject
    @Named("template")
    private MimeMessageFactory<MimeMessage> factory;

    @Inject
    private DocumentAccessBridge documentAccessBridge;

    @Inject
    private NotificationEmailRenderer defaultNotificationEmailRenderer;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private MailSenderConfiguration mailSenderConfiguration;

    @Inject
    private UserAvatarAttachmentExtractor userAvatarAttachmentExtractor;

    @Inject
    private LogoAttachmentExtractor logoAttachmentExtractor;

    private NotificationUserIterator userIterator;

    private Map<String, Object> factoryParameters = new HashMap<>();

    private DocumentReference templateReference;

    private List<CompositeEvent> currentEvents = Collections.emptyList();

    private DocumentReference currentUser;

    private InternetAddress currentUserEmail;

    private boolean hasNext;

    /**
     * Initialize the iterator.
     * A class extending {@link AbstractMimeMessageIterator} should implement a same initialize method that calls
     * this one at the end of its execution.
     *
     * @param userIterator iterator that returns all users
     * @param factoryParameters parameters for the email factory
     * @param templateReference reference to the mail template
     */
    protected void initialize(NotificationUserIterator userIterator, Map<String, Object> factoryParameters,
            DocumentReference templateReference)
    {
        this.userIterator = userIterator;
        this.factoryParameters = factoryParameters;
        this.templateReference = templateReference;
        this.computeNext();
    }

    protected abstract List<CompositeEvent> retrieveCompositeEventList(DocumentReference user)
            throws NotificationException;

    /**
     * Compute the message that will be sent to the next user in the iterator.
     */
    protected void computeNext()
    {
        this.currentEvents = Collections.emptyList();
        this.currentUserEmail = null;
        while (this.currentEvents.isEmpty() && currentUserEmail == null && this.userIterator.hasNext()) {
            this.currentUser = this.userIterator.next();
            try {
                this.currentUserEmail = new InternetAddress(getUserEmail(this.currentUser));
            } catch (AddressException e) {
                // The user has not written a valid email
                continue;
            }

            try {
                // TODO: in a next version, it will be import to paginate these results and to send several emails
                // if there is too much content
                this.currentEvents = retrieveCompositeEventList(currentUser);
            } catch (NotificationException e) {
                logger.error(ERROR_MESSAGE, this.currentUser, e);
            }
        }

        this.hasNext = currentUserEmail != null && !this.currentEvents.isEmpty();
    }

    private void updateFactoryParameters() throws NotificationException, AddressException
    {
        handleEvents();
        handleWikiLogo();

        try {
            factoryParameters.put(FROM, new InternetAddress(mailSenderConfiguration.getFromAddress()));
        } catch (AddressException | NullPointerException e) {
            logger.warn("No default email address is configured in the administration.");
        }

        factoryParameters.put(TO, this.currentUserEmail);
    }

    private void handleEvents() throws NotificationException
    {
        String usedId = serializer.serialize(this.currentUser);
        // Render all the events both in HTML and Plain Text
        List<String> htmlEvents = new ArrayList<>();
        List<String> plainTextEvents = new ArrayList<>();
        EventsSorter eventsSorter = new EventsSorter();
        for (CompositeEvent event : currentEvents) {
            String html = defaultNotificationEmailRenderer.renderHTML(event, usedId);
            String plainText = defaultNotificationEmailRenderer.renderPlainText(event, usedId);
            htmlEvents.add(html);
            plainTextEvents.add(plainText);
            eventsSorter.add(event, html, plainText);
        }

        // Put in the velocity parameters all the events and their rendered version
        Map<String, Object> velocityVariables = getVelocityVariables();
        velocityVariables.put(EVENTS, currentEvents);
        velocityVariables.put(HTML_EVENTS, htmlEvents);
        velocityVariables.put(PLAIN_TEXT_EVENTS, plainTextEvents);
        velocityVariables.put(SORTED_EVENTS, eventsSorter.sort());

        handleAvatars();
    }

    private void handleWikiLogo()
    {
        try {
            getAttachments().add(logoAttachmentExtractor.getLogo());
        } catch (Exception e) {
            logger.warn("Failed to get the logo.", e);
        }
    }

    private Collection<Attachment> getAttachments()
    {
        Object attachments = factoryParameters.get(ATTACHMENTS);
        if (attachments != null) {
            return (Collection<Attachment>) attachments;
        }

        Collection<Attachment> newList = new ArrayList<>();
        factoryParameters.put(ATTACHMENTS, newList);
        return newList;
    }

    private void handleAvatars()
    {
        Set<DocumentReference> userAvatars = new HashSet<>();
        for (CompositeEvent event : currentEvents) {
            userAvatars.addAll(event.getUsers());
        }
        Collection<Attachment> attachments = getAttachments();
        for (DocumentReference userAvatar : userAvatars) {
            try {
                attachments.add(userAvatarAttachmentExtractor.getUserAvatar(userAvatar, 32));
            } catch (Exception e) {
                logger.warn("Failed to add the avatar of [{}] in the email.", userAvatar, e);
            }
        }
    }

    private Map<String, Object> getVelocityVariables()
    {
        Object velocityVariables = factoryParameters.get(VELOCITY_VARIABLES);
        if (velocityVariables == null) {
            velocityVariables = new HashMap<String, Object>();
            factoryParameters.put(VELOCITY_VARIABLES, velocityVariables);
        }

        return (Map<String, Object>) velocityVariables;
    }

    private String getUserEmail(DocumentReference user)
    {
        return (String) documentAccessBridge.getProperty(user,
                new DocumentReference(wikiDescriptorManager.getCurrentWikiId(), "XWiki", "XWikiUsers"),
                0,
                EMAIL_PROPERTY);
    }

    @Override
    public boolean hasNext()
    {
        return hasNext;
    }

    @Override
    public MimeMessage next()
    {
        MimeMessage message = null;
        try {
            updateFactoryParameters();
            message = this.factory.createMessage(templateReference, factoryParameters);
        } catch (Exception e) {
            logger.error(ERROR_MESSAGE, this.currentUser, e);
        }

        // Look for the next email to send
        this.computeNext();

        // But return the current email
        return message;
    }

    @Override
    public Iterator<MimeMessage> iterator()
    {
        return this;
    }
}
