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

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;
import org.xwiki.model.reference.LocalDocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.internal.FilterPreferencesModelBridge;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilter;
import org.xwiki.notifications.filters.internal.ToggleableNotificationFilterActivation;
import org.xwiki.notifications.filters.internal.livedata.NotificationFilterLiveDataTranslationHelper;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;

import com.xpn.xwiki.XWikiContext;

/**
 * Dedicated {@link LiveDataEntryStore} for the {@link NotificationSystemFiltersLiveDataSource}.
 * This component is in charge of performing the actual HQL queries to display the live data.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(NotificationSystemFiltersLiveDataSource.NAME)
public class NotificationSystemFiltersLiveDataEntryStore implements LiveDataEntryStore
{
    private static final String TARGET_SOURCE_PARAMETER = "target";
    private static final String WIKI_SOURCE_PARAMETER = "wiki";
    private static final String UNAUTHORIZED_EXCEPTION_MSG = "You don't have rights to access those information.";
    private static final LocalDocumentReference NOTIFICATION_ADMINISTRATION_REF =
        new LocalDocumentReference(List.of("XWiki", "Notifications", "Code"), "NotificationAdministration");

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    @Inject
    private NotificationFilterLiveDataTranslationHelper translationHelper;

    @Inject
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        Optional<Map<String, Object>> result = Optional.empty();

        return result;
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
            this.displayNotificationFormats(notificationFilter),
            NotificationSystemFiltersLiveDataConfigurationProvider.IS_ENABLED_FIELD,
            this.displayIsEnabled(notificationFilter, filterActivation),
            "isEnabled_data",
            this.displayIsEnabledData(notificationFilter, filterActivation),
            "isEnabled_checked",
            this.isEnabled(notificationFilter, filterActivation)
        );
    }

    private String getObjectNumber(ToggleableNotificationFilterActivation filterActivation)
    {
        return (filterActivation != null && filterActivation.getObjectNumber() != -1) ?
            String.valueOf(filterActivation.getObjectNumber()) : "";
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
        return notificationFilter.isEnabledByDefault() || (filterActivation != null && filterActivation.isEnabled());
    }

    private String displayIsEnabled(ToggleableNotificationFilter notificationFilter,
        ToggleableNotificationFilterActivation filterActivation)
    {
        String checked = (this.isEnabled(notificationFilter, filterActivation)) ? "checked=\"checked\"" : "";
        String html = "<input type=\"checkbox\" class=\"toggleableFilterPreferenceCheckbox\" "
            + "data-filterName=\"%s\" data-objectNumber='%s' %s />";
        return String.format(html, notificationFilter.getName(), getObjectNumber(filterActivation), checked);
    }

    private String displayNotificationFormats(ToggleableNotificationFilter filter)
    {
        StringBuilder result = new StringBuilder("<ul class=\"list-unstyled\">");

        for (NotificationFormat notificationFormat : filter.getFormats()) {
            result.append("<li>");
            result.append(this.translationHelper.getFormatTranslation(notificationFormat));
            result.append("</li>");
        }
        result.append("</ul>");

        return result.toString();
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        if (query.getOffset() > Integer.MAX_VALUE) {
            throw new LiveDataException("Currently only integer offsets are supported.");
        }
        Map<String, Object> sourceParameters = query.getSource().getParameters();
        if (!sourceParameters.containsKey(TARGET_SOURCE_PARAMETER)) {
            throw new LiveDataException("The target source parameter is mandatory.");
        }
        String target = String.valueOf(sourceParameters.get(TARGET_SOURCE_PARAMETER));
        List<ToggleableNotificationFilter> filters = null;
        Map<String, ToggleableNotificationFilterActivation> filtersActivations = null;
        boolean isAuthorized = false;
        try {
            if (WIKI_SOURCE_PARAMETER.equals(target)) {
                WikiReference wikiReference =
                    new WikiReference(String.valueOf(sourceParameters.get(WIKI_SOURCE_PARAMETER)));
                if (this.contextualAuthorizationManager.hasAccess(Right.ADMIN, wikiReference)) {
                    isAuthorized = true;
                    filters = this.notificationFilterManager.getAllFilters(wikiReference)
                        .stream()
                        .filter(filter -> filter instanceof ToggleableNotificationFilter)
                        .map(item -> (ToggleableNotificationFilter) item)
                        .collect(Collectors.toList());
                    filtersActivations =
                        this.filterPreferencesModelBridge.getToggleableFilterActivations(
                            new DocumentReference(wikiReference, NOTIFICATION_ADMINISTRATION_REF));
                }
            } else {
                XWikiContext context = this.contextProvider.get();
                String targetValue = String.valueOf(sourceParameters.get(target));
                DocumentReference userDoc =
                    this.documentReferenceResolver.resolve(targetValue);
                if (this.contextualAuthorizationManager.hasAccess(Right.ADMIN)
                    || context.getUserReference().equals(userDoc)) {
                    isAuthorized = true;
                    filters = this.notificationFilterManager.getAllFilters(userDoc, false)
                        .stream()
                        .filter(filter -> filter instanceof ToggleableNotificationFilter)
                        .map(item -> (ToggleableNotificationFilter) item)
                        .collect(Collectors.toList());
                    filtersActivations = this.filterPreferencesModelBridge.getToggleableFilterActivations(userDoc);
                }
            }
        } catch (NotificationException e) {
            throw new LiveDataException("Error when getting list of filters", e);
        }

        if (!isAuthorized) {
            throw new LiveDataException(UNAUTHORIZED_EXCEPTION_MSG);
        }
        filters.sort(Comparator.comparing(NotificationFilter::getName));

        LiveData liveData = new LiveData();
        int offset = query.getOffset().intValue();
        if (offset < filters.size()) {
            List<Map<String, Object>> entries = liveData.getEntries();
            for (int i = offset; i < Math.min(filters.size(), offset + query.getLimit()); i++) {
                ToggleableNotificationFilter notificationFilter = filters.get(i);
                entries.add(getPreferencesInformation(notificationFilter,
                    filtersActivations.get(notificationFilter.getName())));
            }
            liveData.setCount(entries.size());
        }
        return liveData;
    }
}
