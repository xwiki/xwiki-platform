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
package org.xwiki.notifications.filters.internal.user;

import java.util.ArrayList;
import java.util.Collection;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * @version $Id$
 * @since 9.10RC1
 */
@Component(roles = EventUserFilterPreferencesGetter.class)
@Singleton
public class EventUserFilterPreferencesGetter
{
    @Inject
    private NotificationFilterManager notificationFilterManager;

    @Inject
    private Logger logger;

    public Collection<String> getExcludedUsers(DocumentReference user, NotificationFormat format)
    {
        return collect(getPreferencesWithNoEventType(user, format, NotificationFilterType.EXCLUSIVE));
    }

    private Stream<NotificationFilterPreference> getPreferencesWithNoEventType(DocumentReference user,
            NotificationFormat format, NotificationFilterType filterType)
    {
        return getPreferences(user, format, filterType).filter(
                pref -> pref.getProperties(NotificationFilterProperty.EVENT_TYPE).isEmpty());
    }

    private Collection<String> collect(Stream<NotificationFilterPreference> stream)
    {
        return stream.map(fp -> fp.getProperties(NotificationFilterProperty.USER))
                .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
    }

    private Stream<NotificationFilterPreference> getPreferences(DocumentReference user,
            NotificationFormat format, NotificationFilterType filterType)
    {
        try {
            return notificationFilterManager.getFilterPreferences(user).stream().filter(
                    pref -> pref.isEnabled() && pref.getFilterFormats().contains(format)
                            && EventUserFilter.FILTER_NAME.equals(pref.getFilterName())
                            && pref.getFilterType() == filterType);
        } catch (Exception e) {
            logger.warn("Failed to get the list of UserFilter notification preferences.", e);
            return Stream.empty();
        }
    }
}
