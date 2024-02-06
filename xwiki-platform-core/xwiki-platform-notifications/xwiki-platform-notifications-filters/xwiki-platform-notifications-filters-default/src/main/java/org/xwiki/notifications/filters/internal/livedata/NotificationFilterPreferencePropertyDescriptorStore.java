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

import java.util.Collection;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.livedata.LiveDataException;
import org.xwiki.livedata.LiveDataPropertyDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptor.DisplayerDescriptor;
import org.xwiki.livedata.LiveDataPropertyDescriptorStore;
import org.xwiki.localization.ContextualLocalizationManager;

/**
 * @version $Id$
 * @since 16.1.0RC1
 */
@Component
@Named(NotificationFilterPreferenceLiveDataSource.NOTIFICATION_FILTER_PREFERENCE)
@Singleton
public class NotificationFilterPreferencePropertyDescriptorStore implements LiveDataPropertyDescriptorStore
{
    public static final String NAME_PROPERTY = "name";

    public static final String ID_PROPERTY = "notificationFormats";

    public static final String FILTER_PREFERENCE_ID = "filterPreferenceId";

    public static final String IS_ENABLED_PROPERTY = "isEnabled";

    private static final String L10N_PREFIX = "notifications.settings.filters.preferences.system.table";

    private static final String STRING_TYPE = "String";

    private static final String HTML_DISPLAYER_ID = "html";

    private static final String TOGGLE_DISPLAYER_ID = "toggle";

    @Inject
    private ContextualLocalizationManager l10n;

    @Override
    public Collection<LiveDataPropertyDescriptor> get() throws LiveDataException
    {
        return Set.of(
            getName(),
            getFilterType(),
            getNotificationFormats(),
            getIsEnabled()
        );
    }

    private LiveDataPropertyDescriptor getFilterType()
    {
        LiveDataPropertyDescriptor liveDataPropertyDescriptor = new LiveDataPropertyDescriptor();
        String id = "filterType";
        liveDataPropertyDescriptor.setId(id);
        liveDataPropertyDescriptor.setName(getTranslationFromId(id));
        liveDataPropertyDescriptor.setType(STRING_TYPE);
        return liveDataPropertyDescriptor;
    }

    private LiveDataPropertyDescriptor getName()
    {
        LiveDataPropertyDescriptor liveDataPropertyDescriptor = new LiveDataPropertyDescriptor();
        liveDataPropertyDescriptor.setId(NAME_PROPERTY);
        liveDataPropertyDescriptor.setName(getTranslationFromId(NAME_PROPERTY));
        liveDataPropertyDescriptor.setType(STRING_TYPE);
        return liveDataPropertyDescriptor;
    }

    private LiveDataPropertyDescriptor getIsEnabled()
    {
        LiveDataPropertyDescriptor liveDataPropertyDescriptor = new LiveDataPropertyDescriptor();
        liveDataPropertyDescriptor.setId(IS_ENABLED_PROPERTY);
        liveDataPropertyDescriptor.setName(getTranslationFromId(IS_ENABLED_PROPERTY));
        liveDataPropertyDescriptor.setType(STRING_TYPE);
        liveDataPropertyDescriptor.setDisplayer(new DisplayerDescriptor(TOGGLE_DISPLAYER_ID));
        return liveDataPropertyDescriptor;
    }

    private LiveDataPropertyDescriptor getNotificationFormats()
    {
        LiveDataPropertyDescriptor liveDataPropertyDescriptor = new LiveDataPropertyDescriptor();
        liveDataPropertyDescriptor.setId(ID_PROPERTY);
        liveDataPropertyDescriptor.setName(getTranslationFromId(ID_PROPERTY));
        liveDataPropertyDescriptor.setType(STRING_TYPE);
        liveDataPropertyDescriptor.setDisplayer(new DisplayerDescriptor(HTML_DISPLAYER_ID));
        return liveDataPropertyDescriptor;
    }

    private String getTranslationFromId(String id)
    {
        return this.l10n.getTranslationPlain("%s.%s".formatted(L10N_PREFIX, id));
    }
}
