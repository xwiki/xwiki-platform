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

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataConfiguration;
import org.xwiki.livedata.LiveDataEntryDescriptor;
import org.xwiki.livedata.LiveDataMeta;
import org.xwiki.livedata.LiveDataPaginationConfiguration;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * Configuration of the {@link NotificationSystemFiltersLiveDataSource}.
 *
 * @version $Id$
 * @since 16.3.0RC1
 */
@Component
@Singleton
@Named(NotificationSystemFiltersLiveDataSource.NAME)
public class NotificationSystemFiltersLiveDataConfigurationProvider implements Provider<LiveDataConfiguration>
{
    static final String NAME_FIELD = "name";
    static final String DESCRIPTION_FIELD = "filterDescription";
    static final String NOTIFICATION_FORMATS_FIELD = "notificationFormats";
    static final String IS_ENABLED_FIELD = "isEnabled";
    private static final String TRANSLATION_PREFIX = "notifications.settings.filters.preferences.system.table.";
    private static final String STRING_TYPE = "String";

    @Inject
    private ContextualLocalizationManager l10n;

    @Override
    public LiveDataConfiguration get()
    {
        LiveDataConfiguration input = new LiveDataConfiguration();
        LiveDataMeta meta = new LiveDataMeta();
        input.setMeta(meta);

        LiveDataPaginationConfiguration pagination = new LiveDataPaginationConfiguration();
        pagination.setShowPageSizeDropdown(false);
        meta.setPagination(pagination);

        LiveDataEntryDescriptor entryDescriptor = new LiveDataEntryDescriptor();
        entryDescriptor.setIdProperty(NAME_FIELD);
        meta.setEntryDescriptor(entryDescriptor);

        meta.setPropertyDescriptors(List.of(
            getNameDescriptor(),
            getDescriptionDescriptor(),
            getNotificationFormatsDescriptor(),
            getIsEnabledDescriptor()
        ));

        return input;
    }

    private void setDescriptorValues(LiveDataPropertyDescriptor descriptor)
    {
        descriptor.setVisible(true);
        descriptor.setEditable(false);
        descriptor.setSortable(false);
        descriptor.setFilterable(false);
    }

    private LiveDataPropertyDescriptor getNameDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + NAME_FIELD));
        descriptor.setId(NAME_FIELD);
        descriptor.setType(STRING_TYPE);
        this.setDescriptorValues(descriptor);
        return descriptor;
    }

    private LiveDataPropertyDescriptor getDescriptionDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + DESCRIPTION_FIELD));
        descriptor.setId(DESCRIPTION_FIELD);
        descriptor.setType(STRING_TYPE);
        this.setDescriptorValues(descriptor);
        return descriptor;
    }

    private LiveDataPropertyDescriptor getNotificationFormatsDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + NOTIFICATION_FORMATS_FIELD));
        descriptor.setId(NOTIFICATION_FORMATS_FIELD);
        descriptor.setType(STRING_TYPE);
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor("staticList"));
        this.setDescriptorValues(descriptor);
        return descriptor;
    }

    private LiveDataPropertyDescriptor getIsEnabledDescriptor()
    {
        LiveDataPropertyDescriptor descriptor = new LiveDataPropertyDescriptor();
        descriptor.setName(this.l10n.getTranslationPlain(TRANSLATION_PREFIX + IS_ENABLED_FIELD));
        descriptor.setId(IS_ENABLED_FIELD);
        descriptor.setType("boolean");
        descriptor.setDisplayer(new LiveDataPropertyDescriptor.DisplayerDescriptor("toggle"));
        this.setDescriptorValues(descriptor);
        return descriptor;
    }
}
