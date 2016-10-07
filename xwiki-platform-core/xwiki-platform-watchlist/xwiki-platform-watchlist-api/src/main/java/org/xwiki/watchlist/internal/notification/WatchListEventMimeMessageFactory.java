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
package org.xwiki.watchlist.internal.notification;

import java.util.Iterator;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.mail.MessagingException;
import javax.mail.internet.MimeMessage;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.mail.MimeMessageFactory;
import org.xwiki.mail.SessionFactory;
import org.xwiki.mail.internal.factory.AbstractIteratorMimeMessageFactory;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.watchlist.internal.UserAvatarAttachmentExtractor;
import org.xwiki.watchlist.internal.WatchListEventMatcher;

import com.xpn.xwiki.internal.plugin.rightsmanager.UserIterator;

/**
 * Takes a list of subscribers and a list of {@link org.xwiki.watchlist.internal.api.WatchListEvent WatchListEvent}s
 * occurred in a certain period and, for each subscriber, determines the sublist of events that are interesting to him
 * (i.e. that match his WatchList preferences) anc creates one {@link MimeMessage} to be used to notify him of these
 * events.
 *
 * @version $Id$
 * @since 7.1M1
 */
@Component
@Named(WatchListEventMimeMessageFactory.FACTORY_ID)
@Singleton
public class WatchListEventMimeMessageFactory extends AbstractIteratorMimeMessageFactory
{
    /**
     * Component ID.
     */
    public static final String FACTORY_ID = "watchlistevents";

    /**
     * Hint parameter name. String used to identify the
     * {@link org.xwiki.mail.internal.factory.template.TemplateMimeMessageFactory TemplateMimeMessageFactory}.
     */
    public static final String HINT_PARAMETER = "hint";

    /**
     * Template parameter name. String used to determine the source of the
     * {@link org.xwiki.mail.internal.factory.template.TemplateMimeMessageFactory TemplateMimeMessageFactory}.
     */
    public static final String TEMPLATE_PARAMETER = "template";

    /**
     * SkipContxtUser parameter name. Boolean used to skip notifying the current context user if he should be found in
     * the list of subscribers to be notified. Default is {@code false}.
     */
    public static final String SKIP_CONTEXT_USER_PARAMETER = "skipContextUser";

    /**
     * Attach author avatars parameter name. Boolean used to specify if the avatars of the authors of the events we are
     * notifying about should be attached to the message. Default is {@code false}.
     */
    public static final String ATTACH_AUTHOR_AVATARS_PARAMETER = "attachAuthorAvatars";

    /**
     * Parameters parameter name. Map used as parameters for the
     * {@link org.xwiki.mail.internal.factory.template.TemplateMimeMessageFactory TemplateMimeMessageFactory}.
     */
    public static final String PARAMETERS_PARAMETER = "parameters";

    @Inject
    @Named("explicit")
    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private Execution execution;

    @Inject
    private WatchListEventMatcher eventMatcher;

    @Inject
    private UserAvatarAttachmentExtractor avatarExtractor;

    @Inject
    private SessionFactory sessionFactory;

    @Override
    public Iterator<MimeMessage> createMessage(Object sourceObject, Map<String, Object> parameters)
        throws MessagingException
    {
        Map<String, Object> source = getTypedSource(sourceObject, Map.class);
        validateParameters(parameters, HINT_PARAMETER, TEMPLATE_PARAMETER, PARAMETERS_PARAMETER);

        // Extract from the passed parameters the MimeMessageFactory to use to create a single mail
        String factoryHint = (String) parameters.get(HINT_PARAMETER);

        // TODO: is this configurable or should we always use "template" instead?
        MimeMessageFactory factory = getInternalMimeMessageFactory(factoryHint);

        // Parse the source.
        EventsAndSubscribersSource sourceData = EventsAndSubscribersSource.parse(source);

        // UserDataExtractor to be used for each subscriber.
        WatchListMessageDataExtractor userDataExtractor =
            new WatchListMessageDataExtractor(sourceData, parameters, eventMatcher, execution,
                explicitDocumentReferenceResolver);

        // The iterator that will be checking each subscriber and that will extract the WatchListMessageData.
        UserIterator<WatchListMessageData> userIterator =
            new UserIterator<WatchListMessageData>(EventsAndSubscribersSource.parse(source).getSubscribers(), null,
                userDataExtractor, explicitDocumentReferenceResolver, execution);

        // The iterator that will be producing a MimeMessage for each WatchListMessageData produced by the userIterator.
        WatchListEventMimeMessageIterator messageIterator =
            new WatchListEventMimeMessageIterator(userIterator, factory, parameters, avatarExtractor, serializer,
                sessionFactory);

        return messageIterator;
    }
}
