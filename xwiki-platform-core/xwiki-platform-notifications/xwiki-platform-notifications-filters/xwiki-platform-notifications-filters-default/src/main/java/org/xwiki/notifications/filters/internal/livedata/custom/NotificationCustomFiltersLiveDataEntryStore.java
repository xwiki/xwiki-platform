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
package org.xwiki.notifications.filters.internal.livedata.custom;

import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.script.ScriptContext;

import org.apache.commons.lang3.StringUtils;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.livedata.LiveData;
import org.xwiki.livedata.LiveDataEntryStore;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataQuery;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.NotificationFilterPreferenceStore;
import org.xwiki.notifications.filters.internal.livedata.AbstractNotificationFilterLiveDataEntryStore;
import org.xwiki.query.QueryException;
import org.xwiki.rendering.block.Block;
import org.xwiki.rendering.renderer.BlockRenderer;
import org.xwiki.rendering.renderer.printer.DefaultWikiPrinter;
import org.xwiki.rendering.renderer.printer.WikiPrinter;
import org.xwiki.script.ScriptContextManager;
import org.xwiki.template.TemplateManager;

/**
 * Dedicated {@link LiveDataEntryStore} for the {@link NotificationCustomFiltersLiveDataSource}.
 * This component is in charge of performing the actual HQL queries to display the live data.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(NotificationCustomFiltersLiveDataSource.NAME)
public class NotificationCustomFiltersLiveDataEntryStore extends AbstractNotificationFilterLiveDataEntryStore
{
    private static final String WIKI = "wiki";
    private static final String LOCATION_TEMPLATE = "notification/filters/livedatalocation.vm";

    @Inject
    private NotificationFilterPreferenceStore notificationFilterPreferenceStore;

    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    @Named("context")
    private ComponentManager componentManager;

    @Inject
    @Named("html/5.0")
    private BlockRenderer blockRenderer;

    @Inject
    private TemplateManager templateManager;

    @Inject
    private ScriptContextManager scriptContextManager;

    @Inject
    private NotificationCustomFiltersQueryHelper queryHelper;

    @Override
    public Optional<Map<String, Object>> get(Object entryId) throws LiveDataException
    {
        Optional<Map<String, Object>> result = Optional.empty();
        Optional<NotificationFilterPreference> filterPreferenceOpt;
        try {
            filterPreferenceOpt = this.notificationFilterPreferenceStore
                .getFilterPreference(String.valueOf(entryId), getCurrentWikiReference());
            if (filterPreferenceOpt.isPresent()) {
                DefaultNotificationFilterPreference filterPreference =
                    (DefaultNotificationFilterPreference) filterPreferenceOpt.get();
                checkAccessFilterPreference(filterPreference);
                result = Optional.of(getPreferenceInformation(filterPreference));
            }
        } catch (NotificationException e) {
            throw new LiveDataException(
                String.format("Error while retrieving LiveData entry for notification filter with id [%s]",
                entryId), e);
        }

        return result;
    }

    private Map<String, Object> getPreferenceInformation(NotificationFilterPreference filterPreference)
        throws NotificationException, LiveDataException
    {
        NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope = getScope(filterPreference);

        Map<String, Object> result = new LinkedHashMap<>();
        // Map.of only accept 10 args
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.ID_FIELD, filterPreference.getId());
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.EVENT_TYPES_FIELD,
            this.displayEventTypes(filterPreference));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.NOTIFICATION_FORMATS_FIELD,
            displayNotificationFormats(filterPreference.getNotificationFormats()));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.SCOPE_FIELD, getScopeInfo(scope));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.LOCATION_FIELD,
            this.displayLocation(filterPreference, scope));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.DISPLAY_FIELD,
            this.renderDisplay(filterPreference));
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.FILTER_TYPE_FIELD,
            this.translationHelper.getFilterTypeTranslation(filterPreference.getFilterType()));
        result.put("isEnabled_checked", filterPreference.isEnabled());
        result.put("isEnabled_disabled", filterPreference.getId().startsWith("watchlist_"));
        result.put("isEnabled_data", Map.of(
            "preferenceId", filterPreference.getId()
        ));
        // We don't care: if we access the LD we do have delete.
        result.put(NotificationCustomFiltersLiveDataConfigurationProvider.DOC_HAS_DELETE_FIELD, true);
        return result;
    }

    private String displayLocation(NotificationFilterPreference filterPreference,
        NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope)
    {
        EntityReference location = switch (scope) {
            case USER -> this.entityReferenceResolver.resolve(filterPreference.getUser(), EntityType.DOCUMENT);
            case WIKI -> this.entityReferenceResolver.resolve(filterPreference.getWiki(), EntityType.WIKI);
            case SPACE -> this.entityReferenceResolver.resolve(filterPreference.getPage(), EntityType.SPACE);
            case PAGE ->
                this.entityReferenceResolver.resolve(filterPreference.getPageOnly(), EntityType.DOCUMENT);
        };
        // TODO: Create an improvment ticket for having a displayer
        ScriptContext currentScriptContext = this.scriptContextManager.getCurrentScriptContext();
        currentScriptContext.setAttribute("location", location, ScriptContext.ENGINE_SCOPE);
        return this.templateManager.renderNoException(LOCATION_TEMPLATE);
    }

    private Map<String, Object> displayEventTypes(NotificationFilterPreference filterPreference)
    {
        List<String> items;
        if (filterPreference.getEventTypes().isEmpty()) {
            items = List.of(this.translationHelper.getAllEventTypesTranslation());
        } else {
            items = filterPreference.getEventTypes()
                .stream()
                .sorted(Comparator.naturalOrder())
                .map(eventType -> this.translationHelper.getEventTypeTranslation(eventType))
                .toList();
        }

        return getStaticListInfo(items);
    }

    private Map<String, String> getScopeInfo(NotificationCustomFiltersLiveDataConfigurationProvider.Scope scope)
    {
        String icon = switch (scope) {
            case USER -> "user";
            case WIKI -> WIKI;
            case SPACE -> "chart-organisation";
            case PAGE -> "page";
        };
        return Map.of("icon", icon, "name", this.translationHelper.getScopeTranslation(scope));
    }

    private String renderDisplay(NotificationFilterPreference filterPreference) throws LiveDataException
    {
        String result = "";
        WikiPrinter printer = new DefaultWikiPrinter();
        String missingComponentExceptionMessage = String.format("Cannot find NotificationFilter component for "
            + "preference named [%s]", filterPreference.getFilterName());
        if (this.componentManager.hasComponent(NotificationFilter.class, filterPreference.getFilterName())) {
            try {
                NotificationFilter filter =
                    this.componentManager.getInstance(NotificationFilter.class, filterPreference.getFilterName());
                Block block = this.notificationFilterManager.displayFilter(filter, filterPreference);
                blockRenderer.render(block, printer);
                result = printer.toString();
            } catch (Exception e) {
                throw new LiveDataException("Error while rendering a block for notification filter", e);
            }
        } else {
            throw new LiveDataException(missingComponentExceptionMessage);
        }

        return result;
    }

    private NotificationCustomFiltersLiveDataConfigurationProvider.Scope getScope(NotificationFilterPreference
        filterPreference)
    {
        if (!StringUtils.isBlank(filterPreference.getUser())) {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.USER;
        } else if (!StringUtils.isBlank(filterPreference.getPageOnly())) {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.PAGE;
        } else if (!StringUtils.isBlank(filterPreference.getPage())) {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.SPACE;
        } else {
            return NotificationCustomFiltersLiveDataConfigurationProvider.Scope.WIKI;
        }
    }

    @Override
    public LiveData get(LiveDataQuery query) throws LiveDataException
    {
        if (query.getOffset() > Integer.MAX_VALUE) {
            throw new LiveDataException("Currently only integer offsets are supported.");
        }
        TargetInformation targetInformation = getTargetInformation(query);
        String serializedOwner = this.entityReferenceSerializer.serialize(targetInformation.ownerReference);
        WikiReference wikiReference =
            new WikiReference(targetInformation.ownerReference.extractReference(EntityType.WIKI));

        LiveData liveData = new LiveData();
        try {
            long filterCount = this.queryHelper.countTotalFilters(serializedOwner, wikiReference);
            List<NotificationFilterPreference> filterPreferences =
                this.queryHelper.getFilterPreferences(query, serializedOwner, wikiReference);
            liveData.setCount(filterCount);
            List<Map<String, Object>> entries = liveData.getEntries();
            for (NotificationFilterPreference notificationFilterPreference : filterPreferences) {
                entries.add(getPreferenceInformation(notificationFilterPreference));
            }
        } catch (QueryException | NotificationException e) {
            throw new LiveDataException("Error when querying notification filter preferences", e);
        }
        return liveData;
    }
}
