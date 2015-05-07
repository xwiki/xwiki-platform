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

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.mail.Address;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.context.Execution;
import org.xwiki.mail.internal.factory.usersandgroups.AddressUserDataExtractor;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.watchlist.internal.DefaultWatchListStore;
import org.xwiki.watchlist.internal.WatchListEventMatcher;
import org.xwiki.watchlist.internal.api.WatchListEvent;
import org.xwiki.watchlist.internal.job.WatchListJob;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.internal.plugin.rightsmanager.UserDataExtractor;
import com.xpn.xwiki.objects.BaseObject;

/**
 * Extracts from the current subscriber the {@link WatchListMessageData} to be used when notifying him of events that he
 * is interested in. Handles duplicates and skips emails that have already been processed.
 *
 * @version $Id$
 * @since 7.1M1
 */
public class WatchListMessageDataExtractor implements UserDataExtractor<WatchListMessageData>
{
    private static final Logger LOGGER = LoggerFactory.getLogger(AddressUserDataExtractor.class);

    private EventsAndSubscribersSource source;

    private Set<Address> processedAddresses;

    private WatchListEventMatcher eventMatcher;

    private Execution execution;

    private AddressUserDataExtractor addressExtractor;

    private DocumentReferenceResolver<String> explicitDocumentReferenceResolver;

    private Map<String, Object> parameters;

    private boolean skipContextUser;

    /**
     * Constructor.
     *
     * @param source the input data to iterate over
     * @param parameters the message factory's parameters
     * @param eventMatcher user to determine if a subscriber is interested in an event
     * @param execution used for accessing the subscriber's profile
     * @param explicitDocumentReferenceResolver the resolver to use for transforming group member strings into
     *            {@link org.xwiki.model.reference.DocumentReference}
     */
    public WatchListMessageDataExtractor(EventsAndSubscribersSource source, Map<String, Object> parameters,
        WatchListEventMatcher eventMatcher, Execution execution,
        DocumentReferenceResolver<String> explicitDocumentReferenceResolver)
    {
        this.source = source;
        this.parameters = parameters;
        this.eventMatcher = eventMatcher;
        this.execution = execution;
        this.explicitDocumentReferenceResolver = explicitDocumentReferenceResolver;

        // Reuse the existing Address extractor
        this.addressExtractor = new AddressUserDataExtractor();

        // Init a clean set of processed addresses.
        this.processedAddresses = new HashSet<>();

        // Read and cache the skipContextUser's parameter value, if any was specified.
        Boolean skipContextUserValue =
            (Boolean) parameters.get(WatchListEventMimeMessageFactory.SKIP_CONTEXT_USER_PARAMETER);
        if (skipContextUserValue != null && Boolean.TRUE.equals(skipContextUserValue)) {
            skipContextUser = true;
        }
    }

    @Override
    public WatchListMessageData extractFromSuperadmin(DocumentReference reference)
    {
        return null;
    }

    @Override
    public WatchListMessageData extractFromGuest(DocumentReference reference)
    {
        return null;
    }

    @Override
    public WatchListMessageData extract(DocumentReference subscriberReference, XWikiDocument document,
        BaseObject userObject)
    {
        WatchListMessageData result = null;

        try {
            if (skipContextUser && subscriberReference.equals(getXWikiContext().getUserReference())) {
                // If the current context user should not be notified of events that apparently interest him, stop.
                return null;
            }

            // Get only the events that the current subscriber is interested in.
            List<WatchListEvent> matchingEvents =
                eventMatcher.getMatchingVisibleEvents(source.getEvents(), document.getPrefixedFullName());

            if (matchingEvents.size() == 0) {
                // If there are no interesting events, stop.
                return null;
            }

            String firstName = userObject.getStringValue("first_name");
            String lastName = userObject.getStringValue("last_name");

            Address address = addressExtractor.extract(subscriberReference, document, userObject);
            if (address == null || processedAddresses.contains(address)) {
                // Make sure we skip users with no email set or emails we have already sent to.
                return null;
            }
            // Remember emails we have already sent to.
            processedAddresses.add(address);

            DocumentReference templateReference = getTemplateReference(subscriberReference);

            result =
                new WatchListMessageData(subscriberReference, templateReference, firstName, lastName, address,
                    matchingEvents);

        } catch (Exception e) {
            LOGGER.error("Failed to retrieve information for user [{}]", subscriberReference, e);
        }

        return result;
    }

    private DocumentReference getTemplateReference(DocumentReference subscriberReference)
    {
        DocumentReference result = null;

        // In the case of WatchListJob for example, this is filled in by the user so we must interpret it to determine
        // which actual document to use as template.
        String templateStringReference = (String) parameters.get("template");

        if (templateStringReference.contains(DefaultWatchListStore.WIKI_SPACE_SEP)) {
            // If the configured template is already an absolute reference it's meant to force the template.
            result = explicitDocumentReferenceResolver.resolve(templateStringReference);
            return result;
        }

        // Try on the user's wiki
        WikiReference userWikiReference = subscriberReference.getWikiReference();
        result = explicitDocumentReferenceResolver.resolve(templateStringReference, userWikiReference);
        XWikiContext context = getXWikiContext();
        XWiki wiki = context.getWiki();
        if (wiki.exists(result, context)) {
            return result;
        }

        // Try on the current (context) wiki.
        WikiReference currentWikiReference = new WikiReference(context.getWikiId());
        result = explicitDocumentReferenceResolver.resolve(templateStringReference, currentWikiReference);
        if (wiki.exists(result, context)) {
            return result;
        }

        /* Try the default locations. */

        // Try the default on the context wiki
        result = explicitDocumentReferenceResolver.resolve(WatchListJob.DEFAULT_EMAIL_TEMPLATE, currentWikiReference);
        if (wiki.exists(result, context)) {
            return result;
        }

        // Try the default on the main wiki (final case)
        WikiReference mainWikiReference = new WikiReference(context.getMainXWiki());
        result = explicitDocumentReferenceResolver.resolve(WatchListJob.DEFAULT_EMAIL_TEMPLATE, mainWikiReference);

        return result;
    }

    private XWikiContext getXWikiContext()
    {
        XWikiContext context =
            (XWikiContext) this.execution.getContext().getProperty(XWikiContext.EXECUTIONCONTEXT_KEY);

        return context;
    }

}
