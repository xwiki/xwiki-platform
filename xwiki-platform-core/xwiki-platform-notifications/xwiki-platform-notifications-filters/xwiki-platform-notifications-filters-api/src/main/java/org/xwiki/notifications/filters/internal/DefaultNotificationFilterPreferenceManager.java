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
package org.xwiki.notifications.filters.internal;

import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterPreferenceManager;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterType;

/**
 * Default implementation of {@link NotificationFilterPreferenceManager}.
 *
 * @version $Id$
 * @since 10.9RC1
 */
@Component
@Singleton
public class DefaultNotificationFilterPreferenceManager implements NotificationFilterPreferenceManager
{
    @Inject
    private ComponentManager componentManager;

    @Inject
    private Logger logger;

    @Override
    public Collection<NotificationFilterPreference> getFilterPreferences(DocumentReference user)
            throws NotificationException
    {
        Set<NotificationFilterPreference> filterPreferences = new HashSet<>();

        try {
            List<NotificationFilterPreferenceProvider> providers
                    = componentManager.getInstanceList(NotificationFilterPreferenceProvider.class);

            for (NotificationFilterPreferenceProvider provider : providers) {
                filterPreferences.addAll(provider.getFilterPreferences(user));
            }

            return filterPreferences;
        } catch (ComponentLookupException e) {
            throw new NotificationException(String.format("Unable to fetch a list of notification preference "
                    + "providers with user [%s].", user));
        }
    }

    @Override
    public Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter)
    {
        return filterPreferences.stream().filter(preference -> filter.getName().equals(preference.getFilterName()));
    }

    @Override
    public Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType)
    {
        return getFilterPreferences(filterPreferences, filter).filter(
            preference -> preference.getFilterType() == filterType);
    }

    @Override
    public Stream<NotificationFilterPreference> getFilterPreferences(
            Collection<NotificationFilterPreference> filterPreferences, NotificationFilter filter,
            NotificationFilterType filterType, NotificationFormat format)
    {
        return getFilterPreferences(filterPreferences, filter, filterType).filter(
            preference -> preference.getNotificationFormats().contains(format));
    }

    @Override
    public void saveFilterPreferences(Set<NotificationFilterPreference> filterPreferences)
    {
        Map<String, Set<NotificationFilterPreference>> preferencesMapping = new HashMap<>();

        for (NotificationFilterPreference filterPreference : filterPreferences) {
            // Try to get the corresponding provider, if no provider can be found, discard the save of the preference
            String providerHint = filterPreference.getProviderHint();
            if (componentManager.hasComponent(NotificationFilterPreferenceProvider.class, providerHint)) {
                if (!preferencesMapping.containsKey(providerHint)) {
                    preferencesMapping.put(providerHint, new HashSet<>());
                }

                preferencesMapping.get(providerHint).add(filterPreference);
            }
        }

        // Once we have created the mapping, save all the preferences using their correct providers
        for (String providerHint : preferencesMapping.keySet()) {
            try {
                NotificationFilterPreferenceProvider provider =
                        componentManager.getInstance(NotificationFilterPreferenceProvider.class, providerHint);

                provider.saveFilterPreferences(preferencesMapping.get(providerHint));

            } catch (ComponentLookupException e) {
                logger.error("Unable to retrieve the notification filter preference provider for hint [{}]: [{}]",
                        providerHint, e);
            } catch (NotificationException e) {
                logger.warn("Unable save the filter preferences [{}] against the provider [{}]: [{}]",
                        preferencesMapping.get(providerHint), providerHint, ExceptionUtils.getRootCauseMessage(e));
            }
        }
    }

    @Override
    public void deleteFilterPreference(String filterPreferenceId) throws NotificationException
    {
        try {
            for (NotificationFilterPreferenceProvider provider
                    : componentManager.<NotificationFilterPreferenceProvider>getInstanceList(
                    NotificationFilterPreferenceProvider.class)) {
                provider.deleteFilterPreference(filterPreferenceId);
            }

        } catch (ComponentLookupException e) {
            logger.info("Failed to remove the filter preference [{}].", filterPreferenceId, e);
        }
    }


    @Override
    public void setFilterPreferenceEnabled(String filterPreferenceId, boolean enabled)
            throws NotificationException
    {
        try {
            for (NotificationFilterPreferenceProvider provider
                    : componentManager.<NotificationFilterPreferenceProvider>getInstanceList(
                    NotificationFilterPreferenceProvider.class)) {
                provider.setFilterPreferenceEnabled(filterPreferenceId, enabled);
            }

        } catch (ComponentLookupException e) {
            logger.info("Failed to enable or disabled the filter preference [{}].", filterPreferenceId, e);
        }
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        try {
            List<NotificationFilterPreferenceProvider> providers
                    = componentManager.getInstanceList(NotificationFilterPreferenceProvider.class);

            for (NotificationFilterPreferenceProvider provider : providers) {
                provider.setStartDateForUser(user, startDate);
            }
        } catch (ComponentLookupException e) {
            throw new NotificationException(String.format("Unable to set the starting date for filter preferences"
                    + " with user [%s].", user));
        }
    }
}
