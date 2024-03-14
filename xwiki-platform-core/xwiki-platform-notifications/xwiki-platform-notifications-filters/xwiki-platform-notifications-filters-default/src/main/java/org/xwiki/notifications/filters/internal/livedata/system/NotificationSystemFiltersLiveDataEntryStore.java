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
package org.xwiki.notifications.filters.internal.livedata.system;

import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilterActivation;
import org.xwiki.notifications.filters.internal.livedata.AbstractNotificationFilterLiveDataEntryStore;

/**
 * Dedicated {@link LiveDataEntryStore} for the {@link NotificationSystemFiltersLiveDataSource}.
 * This component is in charge of performing the actual HQL queries to display the live data.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(NotificationSystemFiltersLiveDataSource.NAME)
public class NotificationSystemFiltersLiveDataEntryStore extends AbstractNotificationFilterLiveDataEntryStore
{
    private static final LocalDocumentReference NOTIFICATION_ADMINISTRATION_REF =
        new LocalDocumentReference(List.of("XWiki", "Notifications", "Code"), "NotificationAdministration");

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        // We don't need to retrieve a single element for now.
        return Optional.empty();
    }

    private Map<String, Object> getPreferencesInformation(ToggleableNotificationFilter notificationFilter,
        ToggleableNotificationFilterActivation filterActivation)
    {
        return Map.of(
            NotificationSystemFiltersLiveDataConfigurationProvider.NAME_FIELD,
            this.translationHelper.getTranslationWithPrefix("notifications.filters.name.",
                notificationFilter.getName()),
            NotificationSystemFiltersLiveDataConfigurationProvider.DESCRIPTION_FIELD,
            this.translationHelper.getTranslationWithPrefix("notifications.filters.description.",
                notificationFilter.getName()),
            NotificationSystemFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD,
            displayNotificationFormats(notificationFilter.getFormats()),
            // We don't need to return isEnabled, but only the metadata defined below.
            "isEnabled_data",
            displayIsEnabledData(notificationFilter, filterActivation),
            "isEnabled_checked",
            isEnabled(notificationFilter, filterActivation)
        );
    }

    private String getObjectNumber(ToggleableNotificationFilterActivation filterActivation)
    {
        return (filterActivation != null && filterActivation.getObjectNumber() != -1)
            ? String.valueOf(filterActivation.getObjectNumber()) : "";
    }

    private Map<String, String> displayIsEnabledData(NotificationFilter notificationFilter,
        ToggleableNotificationFilterActivation filterActivation)
    {
        return Map.of(
            "objectNumber", getObjectNumber(filterActivation),
            "filterName", notificationFilter.getName()
        );
    }

    private boolean isEnabled(ToggleableNotificationFilter notificationFilter,
        ToggleableNotificationFilterActivation filterActivation)
    {
        return (filterActivation != null && filterActivation.isEnabled())
            || (filterActivation == null && notificationFilter.isEnabledByDefault());
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        TargetInformation targetInformation = getTargetInformation(query);
        Map<String, ToggleableNotificationFilterActivation> filtersActivations;

        Collection<NotificationFilter> notificationFilters;
        try {
            if (targetInformation.isWikiTarget) {
                WikiReference wikiReference = new WikiReference(targetInformation.ownerReference);
                notificationFilters = this.notificationFilterManager.getAllFilters(wikiReference);
                filtersActivations =
                    this.filterPreferencesModelBridge.getToggleableFilterActivations(
                        new DocumentReference(NOTIFICATION_ADMINISTRATION_REF, wikiReference));
            } else {
                DocumentReference userDoc = new DocumentReference(targetInformation.ownerReference);
                notificationFilters = this.notificationFilterManager.getAllFilters(userDoc, false);
                filtersActivations = this.filterPreferencesModelBridge.getToggleableFilterActivations(userDoc);
            }
        } catch (NotificationException e) {
            throw new LiveDataException("Error when getting list of filters", e);
        }

        List<ToggleableNotificationFilter> allFilters = notificationFilters.stream()
            .filter(filter -> filter instanceof ToggleableNotificationFilter)
            .map(item -> (ToggleableNotificationFilter) item)
            .toList();

        int totalFilters = allFilters.size();
        LiveData liveData = new LiveData();
        liveData.setCount(totalFilters);
        List<Map<String, Object>> entries = liveData.getEntries();

        allFilters
            .stream()
            .sorted(Comparator.comparing(NotificationFilter::getName))
            .skip(query.getOffset())
            .limit(query.getLimit())
            .forEach(filter ->
                entries.add(getPreferencesInformation(filter, filtersActivations.get(filter.getName()))));

        return liveData;
    }
}
