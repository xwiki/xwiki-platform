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

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.icon.IconException;
import org.xwiki.icon.IconManager;
import org.xwiki.livedata.LiveDataActionDescriptor;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.livedata.NotificationFilterLiveDataTranslationHelper;

/**
 * Configuration of the {@link NotificationCustomFiltersLiveDataSource}.
 *
 * @version $Id$
 */
@Component
@Singleton
@Named(NotificationCustomFiltersLiveDataSource.NAME)
public class NotificationCustomFiltersLiveDataConfigurationProvider implements Provider<LiveDataConfiguration>
{
    public static final String ALL_EVENTS_OPTION_VALUE = "__ALL_EVENTS__";
    static final String ID_FIELD = "filterPreferenceId";
    static final String SCOPE_FIELD = "scope";
    static final String LOCATION_FIELD = "location";
    static final String DISPLAY_FIELD = "display";
    static final String FILTER_TYPE_FIELD = "filterType";
    static final String EVENT_TYPES_FIELD = "eventTypes";
    static final String NOTIFICATION_FORMATS_FIELD = "notificationFormats";
    static final String IS_ENABLED_FIELD = "isEnabled";
    static final String ACTIONS_FIELD = "actions";
    static final String DOC_HAS_DELETE_FIELD = "doc_hasdelete";
    private static final String TRANSLATION_PREFIX = "notifications.settings.filters.preferences.custom.table.";
    private static final String DELETE = "delete";
    private static final String STRING_TYPE = "String";
    private static final String HTML_DISPLAYER = "html";
    private static final String VALUE_KEY = "value";
    private static final String LABEL_KEY = "label";

    public enum Scope
    {
        WIKI("wiki"),
        SPACE("page"),
        PAGE("pageOnly"),
        USER("user");

        private final String fieldName;
        Scope(String fieldName)
        {
            this.fieldName = fieldName;
        }

        String getFieldName()
        {
            return this.fieldName;
        }
    };

    @Inject
    private ContextualLocalizationManager l10n;

    @Inject
    private NotificationFilterLiveDataTranslationHelper translationHelper;

    @Inject
    private IconManager iconManager;

    @Inject
    private Logger logger;

    @Override
    public LiveDataConfiguration get()
    {
        LiveDataConfiguration input = new LiveDataConfiguration();
        LiveDataMeta meta = new LiveDataMeta();
        input.setMeta(meta);

        LiveDataPaginationConfiguration pagination = new LiveDataPaginationConfiguration();
        pagination.setShowPageSizeDropdown(true);
        meta.setPagination(pagination);

        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty(ID_FIELD);
        meta.setEntryDescriptor(entryDescriptor);

        LiveDataActionDescriptor deleteAction = new LiveDataActionDescriptor();
        deleteAction.setName(this.l10n.getTranslationPlain("liveData.action.delete"));
        deleteAction.setId(DELETE);
        deleteAction.setAllowProperty(DOC_HAS_DELETE_FIELD);
        try {
            deleteAction.setIcon(this.iconManager.getMetaData(DELETE));
        } catch (IconException e) {
            this.logger.error("Error while getting icon for the remove action", e);
        }
        meta.setActions(List.of(deleteAction));

        meta.setPropertyDescriptors(List.of(
            getDisplayDescriptor(),
            getScopeDescriptor(),
            getLocationDescriptor(),
            getFilterTypeDescriptor(),
            getNotificationFormatsDescriptor(),
            getEventTypesDescriptor(),
            getIsEnabledDescriptor(),
            getActionDescriptor()
        ));

        return input;
    }

    private LiveDataPropertyDescriptor getDisplayDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "name"));
        descriptor.setId(DISPLAY_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor(HTML_DISPLAYER));
        descriptor.setVisible(false);
        descriptor.setEditable(false);
        descriptor.setSortable(false);
        descriptor.setFilterable(false);

        return descriptor;
    }

    private LiveDataPropertyDescriptor.FilterDescriptor createFilterList(List<Map<String, String>> options)
    {
        LiveDataPropertyDescriptor.FilterDescriptor filterList =
            new LiveDataPropertyDescriptor.FilterDescriptor("list");
        filterList.addOperator("empty", null);
        filterList.setParameter("options", options);
        filterList.addOperator("equals", null);
        filterList.setDefaultOperator("equals");
        return filterList;
    }

    private LiveDataPropertyDescriptor getScopeDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "scope"));
        descriptor.setId(SCOPE_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor("scope"));
        descriptor.setFilter(createFilterList(Stream.of(Scope.values())
            .map(item -> Map.of(
                VALUE_KEY, item.name(),
                LABEL_KEY, this.translationHelper.getScopeTranslation(item)
            )).collect(Collectors.toList())));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);

        return descriptor;
    }

    private LiveDataPropertyDescriptor getLocationDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "location"));
        descriptor.setId(LOCATION_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor(HTML_DISPLAYER));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);

        return descriptor;
    }

    private LiveDataPropertyDescriptor getFilterTypeDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "filterType"));
        descriptor.setId(FILTER_TYPE_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setFilter(createFilterList(Stream.of(NotificationFilterType.values())
            .map(item -> Map.of(
                VALUE_KEY, item.name(),
                LABEL_KEY, this.translationHelper.getFilterTypeTranslation(item)
            )).collect(Collectors.toList())));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);

        return descriptor;
    }

    private LiveDataPropertyDescriptor getNotificationFormatsDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "notificationFormats"));
        descriptor.setId(NOTIFICATION_FORMATS_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor("html"));
        descriptor.setFilter(createFilterList(Stream.of(NotificationFormat.values()).map(item ->
            Map.of(
                VALUE_KEY, item.name(),
                LABEL_KEY, this.translationHelper.getFormatTranslation(item)
            )).collect(Collectors.toList())));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);

        return descriptor;
    }

    private LiveDataPropertyDescriptor getEventTypesDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "eventTypes"));
        descriptor.setId(EVENT_TYPES_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor(HTML_DISPLAYER));
        List<Map<String, String>> options = new ArrayList<>();
        options.add(Map.of(
            VALUE_KEY, ALL_EVENTS_OPTION_VALUE,
            LABEL_KEY, this.translationHelper.getAllEventTypesTranslation()));
        try {
            options.addAll(this.translationHelper.getAllEventTypesOptions(true));
        } catch (LiveDataException e) {
            this.logger.error("Cannot provide event filter options", e);
        }
        descriptor.setFilter(createFilterList(options));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);

        return descriptor;
    }

    private LiveDataPropertyDescriptor getIsEnabledDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "isEnabled"));
        descriptor.setId(IS_ENABLED_FIELD);
        descriptor.setType("Boolean");
        LiveDataPropertyDescriptor.FilterDescriptor filterBoolean =
            new LiveDataPropertyDescriptor.FilterDescriptor("boolean");
        descriptor.setFilter(filterBoolean);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor("toggle"));
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(true);
        descriptor.setFilterable(true);

        return descriptor;
    }

    private LiveDataPropertyDescriptor getActionDescriptor()
    {
        LiveDataPropertyDescriptor actionDescriptor = new LiveDataPropertyDescriptor();
        actionDescriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + "_actions"));
        actionDescriptor.setId(ACTIONS_FIELD);
        LiveDataPropertyDescriptor.DisplayerDescriptor displayer =
            new LiveDataPropertyDescriptor.DisplayerDescriptor(ACTIONS_FIELD);
        displayer.setParameter(ACTIONS_FIELD, List.of(DELETE));
        actionDescriptor.setDisplayer(displayer);
        actionDescriptor.setVisible(true);
        actionDescriptor.setEditable(false);
        actionDescriptor.setSortable(false);
        actionDescriptor.setFilterable(false);
        return actionDescriptor;
    }
}
