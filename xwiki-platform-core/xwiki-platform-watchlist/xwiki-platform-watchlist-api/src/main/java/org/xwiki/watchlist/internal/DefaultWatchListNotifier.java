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
package org.xwiki.watchlist.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.mail.Session;
import javax.mail.internet.MimeMessage;

import org.apache.commons.collections4.iterators.IteratorIterable;
import org.xwiki.component.annotation.Component;
import org.xwiki.mail.MailListener;
import org.xwiki.mail.MailSender;
import org.xwiki.mail.MailSenderConfiguration;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.internal.SessionFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.script.service.ScriptServiceManager;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.api.WatchListException;
import org.xwiki.watchlist.internal.api.WatchListNotifier;
import org.xwiki.watchlist.internal.notification.EventsAndSubscribersSource;
import org.xwiki.watchlist.internal.notification.WatchListEventMimeMessageFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Context;

/**
 * Default implementation for {@link WatchListNotifier}. The current implementation offers email notifications only.
 *
 * @version $Id$
 */
@Component
@Singleton
public class DefaultWatchListNotifier implements WatchListNotifier
{
    /**
     * Previous fire time velocity context variable.
     */
    public static final String PREVIOUS_FIRE_TIME_VARIABLE = "previousFireTime";

    /**
     * Wiki page which contains the default watchlist email template.
     */
    public static final String DEFAULT_EMAIL_TEMPLATE = "XWiki.WatchListMessage";

    /**
     * XWiki User Class.
     */
    public static final String XWIKI_USER_CLASS = "XWiki.XWikiUsers";

    /**
     * XWiki User Class email property.
     */
    public static final String XWIKI_USER_CLASS_EMAIL_PROP = "email";

    /**
     * Context provider.
     */
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * Email service configuration.
     */
    @Inject
    private MailSenderConfiguration mailConfiguration;

    @Inject
    private ScriptServiceManager scriptServiceManager;

    @Inject
    @Named(WatchListEventMimeMessageFactory.FACTORY_ID)
    private MimeMessageFactory<Iterator<MimeMessage>> messageFactory;

    @Inject
    private MailSender mailSender;

    @Inject
    @Named("database")
    private Provider<MailListener> mailListenerProvider;

    @Inject
    private SessionFactory sessionFactory;

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Override
    public void sendNotification(String subscriber, List<WatchListEvent> events, String templateDocument,
        Date previousFireTime) throws XWikiException
    {
        Map<String, Object> notificationData = new HashMap<>();
        notificationData.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER, templateDocument);
        notificationData.put(PREVIOUS_FIRE_TIME_VARIABLE, previousFireTime);

        try {
            this.sendNotification(Arrays.asList(subscriber), events, notificationData);
        } catch (WatchListException e) {
            throw new XWikiException("", e);
        }
    }

    @Override
    public void sendNotification(Collection<String> subscribers, List<WatchListEvent> events,
        Map<String, Object> notificationData) throws WatchListException
    {
        try {
            // FIXME: Temporary, until we move to all references.
            List<DocumentReference> subscriberReferences = getSubscriberReferences(subscribers);

            // Source
            Map<String, Object> source = new HashMap<>();
            source.put(EventsAndSubscribersSource.SUBSCRIBERS_PARAMETER, subscriberReferences);
            source.put(EventsAndSubscribersSource.EVENTS_PARAMETER, events);

            // Parameters
            Map<String, Object> parameters = new HashMap<>();
            parameters.put(WatchListEventMimeMessageFactory.HINT_PARAMETER, "template");
            parameters.put(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER,
                notificationData.get(WatchListEventMimeMessageFactory.TEMPLATE_PARAMETER));
            parameters.put(WatchListEventMimeMessageFactory.SKIP_CONTEXT_USER_PARAMETER,
                notificationData.get(WatchListEventMimeMessageFactory.SKIP_CONTEXT_USER_PARAMETER));
            Map<String, Object> templateFactoryParameters = getTemplateFactoryParameters(notificationData);
            parameters.put(WatchListEventMimeMessageFactory.PARAMETERS_PARAMETER, templateFactoryParameters);

            // Create the message iterator and the other mail sender parameters.
            Iterator<MimeMessage> messageIterator = messageFactory.createMessage(null, source, parameters);
            Session session = this.sessionFactory.create(Collections.<String, String>emptyMap());
            MailListener mailListener = mailListenerProvider.get();

            // Pass it to the message sender to send it asynchronously.
            // FIXME: !? There must be a better way instead of using IteratorIterable.
            mailSender.sendAsynchronously(new IteratorIterable<MimeMessage>(messageIterator), session, mailListener);
        } catch (Exception e) {
            throw new WatchListException(String.format("Failed to send notification to subscribers [%s]", subscribers),
                e);
        }
    }

    private List<DocumentReference> getSubscriberReferences(Collection<String> subscribers)
    {
        List<DocumentReference> result = new ArrayList<>();

        XWikiContext context = contextProvider.get();
        WikiReference currentWikiReference = new WikiReference(context.getWikiId());

        for (String subscriber : subscribers) {
            DocumentReference subscriberReference =
                explicitDocumentReferenceResolver.resolve(subscriber, currentWikiReference);
            result.add(subscriberReference);
        }
        return result;
    }

    private Map<String, Object> getTemplateFactoryParameters(Map<String, Object> notificationData)
    {
        Map<String, Object> parameters = new HashMap<String, Object>();

        XWikiContext context = contextProvider.get();

        // Prepare email template (wiki page) context
        Map<String, Object> velocityVariables = new HashMap<>();
        velocityVariables.put(PREVIOUS_FIRE_TIME_VARIABLE, notificationData.get(PREVIOUS_FIRE_TIME_VARIABLE));
        // XWiki API
        velocityVariables.put("xwiki", new com.xpn.xwiki.api.XWiki(context.getWiki(), context));
        velocityVariables.put("util", new com.xpn.xwiki.api.Util(context.getWiki(), context));
        Context xcontext = new Context(context);
        velocityVariables.put("xcontext", xcontext);
        velocityVariables.put("services", scriptServiceManager);
        // Deprecated XWiki API
        velocityVariables.put("msg", context.getMessageTool());
        velocityVariables.put("context", xcontext);
        // Note: The other variables that are context dependent will be updated for each subscriber by the iterator,
        // since they are different for each subscriber.
        // Add to parameters
        parameters.put("velocityVariables", velocityVariables);

        // Get the wiki's default language (default en).
        String language = context.getWiki().getXWikiPreference("default_language", "en", context);
        parameters.put("language", language);

        // Add the template document's attachments to the email.
        parameters.put("includeTemplateAttachments", true);

        // Set the mail's type to "watchlist".
        parameters.put("type", "watchlist");

        // Get from email address from the configuration (default : mailer@xwiki.localdomain.com)
        String from = getFromAddress();
        parameters.put("from", from);

        return parameters;
    }

    private String getFromAddress()
    {
        // Get from email address from the configuration (default : mailer@xwiki.localdomain.com)
        String from = mailConfiguration.getFromAddress();
        if (from == null) {
            from = "mailer@xwiki.localdomain.com";
        }
        return from;
    }
}
