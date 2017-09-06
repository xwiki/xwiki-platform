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
package org.xwiki.notifications.filters.watch;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceResolver;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterProperty;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.filters.internal.DefaultNotificationFilterPreference;
import org.xwiki.notifications.filters.internal.ScopeNotificationFilter;
import org.xwiki.notifications.filters.internal.ScopeNotificationFilterPreference;
import org.xwiki.notifications.preferences.internal.UserProfileNotificationPreferenceProvider;
import org.xwiki.stability.Unstable;

/**
 * Reference of a location to watch.
 *
 * @version $Id$
 * @since 9.8RC1
 */
@Unstable
public class WatchedLocationReference implements WatchedEntityReference
{
    private static final Set<NotificationFormat> ALL_NOTIFICATION_FORMATS
            = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(NotificationFormat.values())));

    private EntityReference entityReference;

    private String serializedReference;

    private EntityReferenceResolver<String> resolver;

    /**
     * Construct a WatchedLocationReference.
     * @param entityReference the reference of the location to watch
     * @param serializedReference the serialized reference of the location to watch
     * @param resolver the default entity reference resolver
     */
    public WatchedLocationReference(EntityReference entityReference, String serializedReference,
            EntityReferenceResolver<String> resolver)
    {
        this.entityReference = entityReference;
        this.serializedReference = serializedReference;
        this.resolver = resolver;
    }

    @Override
    public boolean matchExactly(NotificationFilterPreference notificationFilterPreference)
    {
        if (ScopeNotificationFilter.FILTER_NAME.equals(notificationFilterPreference.getFilterName())) {
            ScopeNotificationFilterPreference scope
                    = new ScopeNotificationFilterPreference(notificationFilterPreference, resolver);
            return entityReference.equals(scope.getScopeReference());
        }

        return false;
    }

    @Override
    public boolean match(NotificationFilterPreference notificationFilterPreference)
    {
        if (notificationFilterPreference instanceof ScopeNotificationFilterPreference) {
            ScopeNotificationFilterPreference scope = (ScopeNotificationFilterPreference) notificationFilterPreference;
            EntityReference scopeReference = scope.getScopeReference();
            if (scopeReference != null) {
                return entityReference.equals(scopeReference) || entityReference.hasParent(scopeReference);
            }
        }

        return false;
    }

    @Override
    public NotificationFilterPreference createFilterPreference()
    {
        DefaultNotificationFilterPreference filterPreference
                = new DefaultNotificationFilterPreference(Long.toString(new Date().getTime()));

        // Fields
        filterPreference.setEnabled(true);
        filterPreference.setFilterType(NotificationFilterType.INCLUSIVE);
        filterPreference.setFilterName(ScopeNotificationFilter.FILTER_NAME);
        filterPreference.setNotificationFormats(ALL_NOTIFICATION_FORMATS);
        filterPreference.setProviderHint(UserProfileNotificationPreferenceProvider.NAME);

        // Properties
        Map<NotificationFilterProperty, List<String>> preferenceProperties = new HashMap<>();
        filterPreference.setPreferenceProperties(preferenceProperties);

        preferenceProperties.put(NotificationFilterProperty.EVENT_TYPE, Collections.emptyList());

        // Scope value
        List<String> value = Collections.singletonList(serializedReference);
        switch (entityReference.getType()) {
            case WIKI:
                preferenceProperties.put(NotificationFilterProperty.WIKI, value);
                break;
            case SPACE:
                preferenceProperties.put(NotificationFilterProperty.SPACE, value);
                break;
            case DOCUMENT:
                preferenceProperties.put(NotificationFilterProperty.PAGE, value);
                break;
            default:
                break;
        }

        return new ScopeNotificationFilterPreference(filterPreference, entityReference);
    }
}
