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
package org.xwiki.notifications.filters.internal.livedata;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.notifications.filters.internal.livedata.NotificationFilterPreferenceLiveDataSource.NOTIFICATION_FILTER_PREFERENCE;
import static org.xwiki.notifications.filters.internal.livedata.NotificationFilterPreferencePropertyDescriptorStore.FILTER_PREFERENCE_ID;
import static org.xwiki.notifications.filters.internal.livedata.NotificationFilterPreferencePropertyDescriptorStore.ID_PROPERTY;
import static org.xwiki.notifications.filters.internal.livedata.NotificationFilterPreferencePropertyDescriptorStore.IS_ENABLED_PROPERTY;
import static org.xwiki.notifications.filters.internal.livedata.NotificationFilterPreferencePropertyDescriptorStore.NAME_PROPERTY;

/**
 * @version $Id$
 * @since 16.1.0RC1
 */
@Component
@Named(NOTIFICATION_FILTER_PREFERENCE)
@Singleton
public class NotificationFilterPreferenceEntryStore implements LiveDataEntryStore
{
    private static final String USER_SOURCE_PARAM = "user";

    @Inject
    private DocumentReferenceResolver<String> entityReferenceResolver;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Inject
    private NotificationFilterPreferenceManager notificationFilterPreferenceManager;

    @Inject
    private Provider<XWikiContext> xWikiContextProvider;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private ContextualLocalizationManager l10n;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        return Optional.empty();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        // TODO port the select logic here
        Map<String, Object> sourceParameters = query.getSource().getParameters();

        boolean isAccessGranted = true;
        DocumentReference requestedUserDocRef = null;
        XWikiContext xWikiContext = this.xWikiContextProvider.get();
        WikiReference wikiReference = null;
        String user = (String) sourceParameters.getOrDefault(USER_SOURCE_PARAM, "");
        XWikiDocument document;
        try {
            if (!user.isEmpty()) {
                requestedUserDocRef = this.entityReferenceResolver.resolve(user);
                isAccessGranted = this.contextualAuthorizationManager.hasAccess(Right.ADMIN, requestedUserDocRef)
                    || xWikiContext.getUserReference().equals(requestedUserDocRef);
                document = xWikiContext.getWiki().getDocument(requestedUserDocRef, xWikiContext);
            } else {
                wikiReference = xWikiContext.getWikiReference();
                document = xWikiContext.getWiki().getDocument(
                    new LocalDocumentReference(List.of("XWiki", "Notifications", "Code"), "NotificationAdministration"),
                    xWikiContext);
            }
        } catch (XWikiException e) {
            // TODO: improve message
            throw new LiveDataException("Failed to access document...", e);
        }

        if (!isAccessGranted) {
            // TODO: improve error message.
            throw new LiveDataException("Access not allowed");
        }

        Object type = sourceParameters.getOrDefault("type", "");
        boolean displaySystem = true;
        boolean displayCustom = true;
        if (type.equals("custom")) {
            displaySystem = false;
        } else if (type.equals("system")) {
            displayCustom = false;
        }

        long index = 0;
        Long offset = query.getOffset();
        long limitOffset = offset + query.getLimit();

        List<Map<String, Object>> entries = new ArrayList<>();
        if (displaySystem) {
            Set<NotificationFilter> systemFilters = getSystemFilters(requestedUserDocRef, wikiReference);
            for (NotificationFilter filter : systemFilters) {
                index += 1;
                if (isInRange(index, offset, limitOffset)) {
                    boolean isEnabled = filter instanceof ToggleableNotificationFilter
                        && ((ToggleableNotificationFilter) filter).isEnabledByDefault();
                    String objectNumber = "";
                    BaseObject obj =
                        document.getXObject(new LocalDocumentReference(List.of("XWiki", "Notifications", "Code"),
                            "ToggleableFilterPreferenceClass"));
                    if (obj != null) {
                        isEnabled = obj.getIntValue("isEnabled") != 0;
                        objectNumber = String.valueOf(obj.getNumber());
                    }

                    entries.add(buildEntry(filter, isEnabled, objectNumber));
                }
            }
        }

        // Continue with the display custom fields only 
        if (displayCustom && isInRange(index, offset, limitOffset)) {

        }

        LiveData liveData = new LiveData();
        // TODO: define count...
        liveData.setCount(0);
//        entries.sort(Comparator.comparing(o -> ((String) o.getOrDefault("name", ""))));
        liveData.getEntries().addAll(entries);
        return liveData;
    }

    private static boolean isInRange(long index, Long offset, long limitOffset)
    {
        return index > offset && index <= limitOffset;
    }

    private Map<String, Object> buildEntry(NotificationFilter systemFilter, boolean isEnabled,
        String objectNumber)
    {
        String name = this.l10n.getTranslationPlain(
            "notifications.filters.name.%s".formatted(systemFilter.getName()));
        Map<String, Object> entry = Map.of(
            FILTER_PREFERENCE_ID, name,
            NAME_PROPERTY, name,
            ID_PROPERTY, "notificationFormats",
            "%s_checked".formatted(IS_ENABLED_PROPERTY), isEnabled,
            "%s_data".formatted(IS_ENABLED_PROPERTY), Map.of(
                "objectNumber", objectNumber,
                "filterName", systemFilter.getName()
            )
        );
        return entry;
    }

    private List<NotificationFilter> getSystemFilters(DocumentReference requestedUserDocRef,
        WikiReference wikiReference)
        throws LiveDataException
    {
        try {
            Collection<NotificationFilter> notificationFilters;
            if (requestedUserDocRef != null) {
                notificationFilters = this.notificationFilterManager.getAllFilters(requestedUserDocRef, false);
            } else {
                notificationFilters = this.notificationFilterManager.getAllFilters(wikiReference);
            }
            return this.notificationFilterManager.getToggleableFilters(notificationFilters)
                .sorted(Comparator.comparing(NotificationFilter::getName))
                .toList();
        } catch (NotificationException e) {
            throw new LiveDataException("Failed to get system filters", e);
        }
    }

    private List<NotificationFilterPreference> getCustomFilters(DocumentReference requestedUserDocRef,
        WikiReference wikiReference, NotificationFilter filter)
        throws LiveDataException
    {
        // TODO: that's the second step, the fist one is to get the first list of filters
        // ALso, need to re-read the velocity code to check that the parameters is correctly used.
        try {
            Collection<NotificationFilterPreference> notificationFilters;
            if (requestedUserDocRef != null) {
                notificationFilters =
                    this.notificationFilterPreferenceManager.getFilterPreferences(requestedUserDocRef);
            } else {
                notificationFilters = this.notificationFilterPreferenceManager.getFilterPreferences(wikiReference);
            }
            return this.notificationFilterPreferenceManager.getFilterPreferences(notificationFilters, filter)
                .sorted(Comparator.comparing(NotificationFilterPreference::getId))
                .toList();
        } catch (NotificationException e) {
            throw new LiveDataException("Failed to get custom filters", e);
        }
    }
}
