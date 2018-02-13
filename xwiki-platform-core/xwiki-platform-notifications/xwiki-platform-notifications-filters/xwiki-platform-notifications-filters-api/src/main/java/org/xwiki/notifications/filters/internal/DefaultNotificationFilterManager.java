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

import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterDisplayer;
import org.xwiki.notifications.filters.NotificationFilterPreferenceProvider;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link NotificationFilterManager}.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Singleton
public class DefaultNotificationFilterManager implements NotificationFilterManager
{
    private static final String ERROR_MESSAGE = "Failed to get all the notification filters.";

    @Inject
    private ComponentManager componentManager;

    @Inject
    private WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ModelContext modelContext;

    @Inject
    private NotificationFilterDisplayer defaultNotificationFilterDisplayer;

    @Inject
    @Named("cached")
    private ModelBridge modelBridge;

    @Inject
    private Logger logger;

    @Override
    public Set<NotificationFilter> getAllFilters(DocumentReference user)
            throws NotificationException
    {
        return removeDisabledFilters(user, fetchAllFilters(user));
    }

    @Override
    public Set<NotificationFilter> getFilters(DocumentReference user,
            NotificationPreference preference) throws NotificationException
    {
        Set<NotificationFilter> filters = getAllFilters(user);

        Iterator<NotificationFilter> it = filters.iterator();

        while (it.hasNext()) {
            NotificationFilter filter = it.next();

            if (!filter.matchesPreference(preference)) {
                it.remove();
            }
        }

        return filters;
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException
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
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user, NotificationFilter filter)
            throws NotificationException
    {
        Set<NotificationFilterPreference> preferences = getFilterPreferences(user);

        Iterator<NotificationFilterPreference> it = preferences.iterator();
        while (it.hasNext()) {
            NotificationFilterPreference preference = it.next();

            if (!filter.getName().equals(preference.getFilterName())) {
                it.remove();
            }
        }

        return preferences;
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user, NotificationFilter filter,
            NotificationFilterType filterType) throws NotificationException
    {
        Set<NotificationFilterPreference> preferences = getFilterPreferences(user);

        Iterator<NotificationFilterPreference> it = preferences.iterator();
        while (it.hasNext()) {
            NotificationFilterPreference preference = it.next();

            if (!(filter.getName().equals(preference.getFilterName())
                    && preference.getFilterType().equals(filterType))) {
                it.remove();
            }
        }

        return preferences;
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user, NotificationFilter filter,
            NotificationFilterType filterType, NotificationFormat format) throws NotificationException
    {
        Set<NotificationFilterPreference> preferences = getFilterPreferences(user, filter);

        Iterator<NotificationFilterPreference> it = preferences.iterator();

        while (it.hasNext()) {
            NotificationFilterPreference preference = it.next();

            if (!preference.getFilterFormats().contains(format) || !preference.getFilterType().equals(filterType)) {
                it.remove();
            }
        }

        return preferences;
    }

    @Override
    public Set<NotificationFilter> getToggleableFilters(DocumentReference user) throws NotificationException
    {
        Set<NotificationFilter> userFilters = fetchAllFilters(user);

        Iterator<NotificationFilter> it = userFilters.iterator();

        while (it.hasNext()) {
            NotificationFilter filter = it.next();

            if (!(filter instanceof ToggleableNotificationFilter)) {
                it.remove();
            }
        }

        return userFilters;
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
    public Block displayFilter(NotificationFilter filter, NotificationFilterPreference preference)
            throws NotificationException
    {
        /**
         * Try to find a {@link NotificationFilterDisplayer} that corresponds to the given filter.
         * If no renderer is found, fallback on the default one.
         */
        try {
            List<NotificationFilterDisplayer> renderers =
                    componentManager.getInstanceList(NotificationFilterDisplayer.class);

            for (NotificationFilterDisplayer renderer : renderers) {
                if (renderer.getSupportedFilters().contains(filter.getName())) {
                    return renderer.display(filter, preference);
                }
            }

            return defaultNotificationFilterDisplayer.display(filter, preference);
        } catch (ComponentLookupException e) {
            throw new NotificationException(String.format(
                    "Unable get a list of NotificationFilterDisplayer for filter [%s].", filter), e);
        }
    }

    @Override
    public void deleteFilterPreference(String filterPreferenceName) throws NotificationException
    {
        try {
            for (NotificationFilterPreferenceProvider provider
                    : componentManager.<NotificationFilterPreferenceProvider>getInstanceList(
                            NotificationFilterPreferenceProvider.class)) {
                provider.deleteFilterPreference(filterPreferenceName);
            }

        } catch (ComponentLookupException e) {
            logger.info("Failed to remove the filter preference [{}].", filterPreferenceName, e);
        }
    }

    @Override
    public void setFilterPreferenceEnabled(String filterPreferenceName, boolean enabled)
            throws NotificationException
    {
        try {
            for (NotificationFilterPreferenceProvider provider
                    : componentManager.<NotificationFilterPreferenceProvider>getInstanceList(
                    NotificationFilterPreferenceProvider.class)) {
                provider.setFilterPreferenceEnabled(filterPreferenceName, enabled);
            }

        } catch (ComponentLookupException e) {
            logger.info("Failed to enable or disabled the filter preference [{}].", filterPreferenceName, e);
        }
    }

    /**
     * Goes through every given {@link NotificationFilter}. One of the filters implements
     * {@link ToggleableNotificationFilter}, checks if the given user has disabled this filter. If so, remove the
     * filter from the set.
     *
     * @param user the user to use
     * @param filters the filters that should be examined
     * @return a set of filters that are not marked as disabled by the user
     * @throws NotificationException if an error occurs
     *
     * @since 9.7RC1
     */
    private Set<NotificationFilter> removeDisabledFilters(DocumentReference user, Set<NotificationFilter> filters)
            throws NotificationException
    {
        Iterator<NotificationFilter> it = filters.iterator();

        Map<String, Boolean> filterActivations = modelBridge.getToggeableFilterActivations(user);

        while (it.hasNext()) {
            NotificationFilter filter = it.next();

            if (filter instanceof ToggleableNotificationFilter && filterActivations.containsKey(filter.getName())
                    && !filterActivations.get(filter.getName())) {
                it.remove();
            }
        }

        return filters;
    }

    /**
     * Fetches every filter available to the user, without taking care of whether the filter is disabled by the user
     * or not.
     *
     * @param user the user to use
     * @return a set of filters
     * @throws NotificationException if an error occurs
     */
    private Set<NotificationFilter> fetchAllFilters(DocumentReference user) throws NotificationException
    {
        // If the user is from the main wiki, get filters from all wikis
        if (user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId())) {

            String currentWikiId = wikiDescriptorManager.getCurrentWikiId();

            Map<String, NotificationFilter> filters = new HashMap<>();
            try {
                for (String wikiId : wikiDescriptorManager.getAllIds()) {
                    modelContext.setCurrentEntityReference(new WikiReference(wikiId));

                    filters.putAll(componentManager.getInstanceMap(NotificationFilter.class));
                }
            } catch (Exception e) {
                throw new NotificationException(ERROR_MESSAGE, e);
            } finally {
                modelContext.setCurrentEntityReference(new WikiReference(currentWikiId));
            }

            return new HashSet<>(filters.values());
        } else {
            // If the user is local, get filters from the current wiki only (we assume it's the wiki of the user).
            try {
                return new HashSet<>(componentManager.getInstanceList(NotificationFilter.class));
            }  catch (Exception e) {
                throw new NotificationException(ERROR_MESSAGE, e);
            }
        }
    }
}
