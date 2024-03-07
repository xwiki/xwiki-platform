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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.ModelContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterDisplayer;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.user.UserReference;
import org.xwiki.user.UserReferenceSerializer;
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
    protected WikiDescriptorManager wikiDescriptorManager;

    @Inject
    private ComponentManager componentManager;

    @Inject
    private ModelContext modelContext;

    @Inject
    private NotificationFilterDisplayer defaultNotificationFilterDisplayer;

    @Inject
    @Named("cached")
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @Inject
    @Named("document")
    private UserReferenceSerializer<DocumentReference> documentReferenceUserReferenceSerializer;

    @Override
    public Collection<NotificationFilter> getAllFilters(boolean allWikis) throws NotificationException
    {
        if (allWikis) {
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
            // Get filters from the current wiki only
            try {
                return new HashSet<>(componentManager.getInstanceList(NotificationFilter.class));
            }  catch (Exception e) {
                throw new NotificationException(ERROR_MESSAGE, e);
            }
        }
    }

    @Override
    public Collection<NotificationFilter> getAllFilters(WikiReference wikiReference) throws NotificationException
    {
        WikiReference currentWikiReference = wikiDescriptorManager.getCurrentWikiReference();
        List<NotificationFilter> filters;
        try {
            modelContext.setCurrentEntityReference(wikiReference);

            filters = componentManager.getInstanceList(NotificationFilter.class);
        } catch (Exception e) {
            throw new NotificationException(ERROR_MESSAGE, e);
        } finally {
            modelContext.setCurrentEntityReference(currentWikiReference);
        }
        return new HashSet<>(filters);
    }

    @Override
    public Collection<NotificationFilter> getAllFilters(DocumentReference user, boolean onlyEnabled)
        throws NotificationException
    {
        return getAllFilters(user, onlyEnabled, null);
    }

    @Override
    public Collection<NotificationFilter> getAllFilters(DocumentReference user, boolean onlyEnabled,
        NotificationFilter.FilteringPhase filteringPhase) throws NotificationException
    {
        Collection<NotificationFilter> filters = getAllFilters(
            user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId()));
        Map<String, Boolean> filterActivations = getToggeableFilterActivations(user);
        Iterator<NotificationFilter> it = filters.iterator();
        while (it.hasNext()) {
            NotificationFilter filter = it.next();
            boolean filterActivation = filterActivations.getOrDefault(filter.getName(), true);
            if (!this.shouldFilterBeSelected(filter, filterActivation, onlyEnabled, filteringPhase)) {
                it.remove();
            }
        }
        return filters;
    }

    private boolean shouldFilterBeSelected(NotificationFilter filter, boolean filterActivation, boolean onlyEnabled,
        NotificationFilter.FilteringPhase filteringPhase)
    {
        return shouldFilterBeSelectedBasedOnStatus(filterActivation, onlyEnabled)
            && shouldFilterBeSelectedBasedOnFilteringPhase(filter, filteringPhase);
    }

    private boolean shouldFilterBeSelectedBasedOnStatus(boolean filterActivation, boolean onlyEnabled)
    {
        boolean result = false;
        // if we don't care to get only the enabled one, we can return all of them.
        if (!onlyEnabled) {
            result = true;
        // else we just need to ensure that the filter is activated
        } else if (filterActivation) {
            result = true;
        }
        return result;
    }

    private boolean shouldFilterBeSelectedBasedOnFilteringPhase(NotificationFilter filter,
        NotificationFilter.FilteringPhase filteringPhase)
    {
        boolean result;
        Set<NotificationFilter.FilteringPhase> filterFilteringPhases = filter.getFilteringPhases();
        // by definition if the filteringPhase provided is null we return all filters
        if (filteringPhase == null) {
            result = true;
        // we consider that filters should properly set the filtering phases to be selected.
        } else if (filterFilteringPhases == null || filterFilteringPhases.isEmpty()) {
            result = false;
        // if we're requesting for post-filtering phases, we return only
        // the filters that only supports post-filtering: we don't need the other ones since they have been already
        // applied.
        } else if (filteringPhase == NotificationFilter.FilteringPhase.POST_FILTERING) {
            result = Collections.singleton(NotificationFilter.FilteringPhase.POST_FILTERING)
                .equals(filterFilteringPhases);
        // In other cases, we only want to ensure that one of the request phase is supported.
        } else if (filterFilteringPhases.contains(filteringPhase)) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public Stream<NotificationFilter> getFiltersRelatedToNotificationPreference(Collection<NotificationFilter> filters,
            NotificationPreference preference)
    {
        return filters.stream().filter(filter -> filter.matchesPreference(preference));
    }

    @Override
    public Stream<NotificationFilter> getToggleableFilters(Collection<NotificationFilter> filters)
    {
        return filters.stream().filter(filter -> filter instanceof ToggleableNotificationFilter);
    }

    @Override
    public List<ToggleableNotificationFilter> getToggleableFilters(UserReference userReference)
        throws NotificationException
    {
        DocumentReference userDoc = this.documentReferenceUserReferenceSerializer.serialize(userReference);
        List<ToggleableNotificationFilter> result = new ArrayList<>();
        getAllFilters(userDoc, false)
            .stream().filter(filter -> filter instanceof ToggleableNotificationFilter)
            .forEach(item -> result.add((ToggleableNotificationFilter) item));
        return result;
    }

    @Override
    public List<ToggleableNotificationFilter> getToggleableFilters(WikiReference wikiReference)
        throws NotificationException
    {
        List<ToggleableNotificationFilter> result = new ArrayList<>();
        getAllFilters(wikiReference)
            .stream().filter(filter -> filter instanceof ToggleableNotificationFilter)
            .forEach(item -> result.add((ToggleableNotificationFilter) item));
        return result;
    }

    @Override
    public Map<String, Boolean> getToggeableFilterActivations(DocumentReference user)
            throws NotificationException
    {
        return filterPreferencesModelBridge.getToggleableFilterActivations(user);
    }

    @Override
    public Stream<NotificationFilter> getEnabledFilters(Collection<NotificationFilter> filters,
            Map<String, Boolean> filterActivations)
    {
        return filters.stream().filter(
            filter -> {
                if (filter instanceof ToggleableNotificationFilter
                        && filterActivations.containsKey(filter.getName())) {
                    return filterActivations.get(filter.getName());
                }
                return true;
            }
        );
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
}
