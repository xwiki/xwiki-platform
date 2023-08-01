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
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterDisplayer;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.rendering.block.Block;
import org.xwiki.wiki.descriptor.WikiDescriptorManager;

/**
 * Default implementation of {@link NotificationFilterManager}.
 *
 * @version $Id$
 * @since 15.5RC1
 */
@Component
@Singleton
public class LegacyDefaultNotificationFilterManager extends DefaultNotificationFilterManager
{
    @Inject
    private NotificationConfiguration configuration;

    @Override
    public Collection<NotificationFilter> getAllFilters(DocumentReference user, boolean onlyEnabled,
        NotificationFilter.FilteringPhase filteringPhase) throws NotificationException
    {
        if (this.configuration.isEventPrefilteringEnabled()) {
            return super.getAllFilters(user, onlyEnabled, filteringPhase);
        } else {
            Collection<NotificationFilter> filters = getAllFilters(
                user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId()));
            Map<String, Boolean> filterActivations = getToggeableFilterActivations(user);
            Iterator<NotificationFilter> it = filters.iterator();
            while (it.hasNext()) {
                NotificationFilter filter = it.next();
                boolean filterActivation = filterActivations.getOrDefault(filter.getName(), true);
                if (!this.legacyShouldFilterBeSelected(filter, filterActivation, onlyEnabled, filteringPhase,
                    false))
                {
                    it.remove();
                }
            }
            return filters;
        }
    }

    private boolean legacyShouldFilterBeSelected(NotificationFilter filter, boolean filterActivation, boolean onlyEnabled,
        NotificationFilter.FilteringPhase filteringPhase, boolean onlyExactPhase)
    {
        return legacyShouldFilterBeSelectedBasedOnStatus(filterActivation, onlyEnabled)
            && legacyShouldFilterBeSelectedBasedOnFilteringPhase(filter, filteringPhase, onlyExactPhase);
    }

    private boolean legacyShouldFilterBeSelectedBasedOnStatus(boolean filterActivation, boolean onlyEnabled)
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

    private boolean legacyShouldFilterBeSelectedBasedOnFilteringPhase(NotificationFilter filter,
        NotificationFilter.FilteringPhase filteringPhase, boolean prefilteringEnabled)
    {
        boolean result;
        Set<NotificationFilter.FilteringPhase> filterFilteringPhases = filter.getFilteringPhases();
        // by definition if the filteringPhase provided is null we return all filters
        if (filteringPhase == null) {
            result = true;
        // we consider that filters should properly set the filtering phases to be selected.
        } else if (filterFilteringPhases == null || filterFilteringPhases.isEmpty()) {
            result = false;
        // if pre-filtering is enabled and we're requesting for post-filtering phases, we return only
        // the filters that only supports post-filtering: we don't need the other ones since they have been already
        // applied.
        } else if (prefilteringEnabled && filteringPhase == NotificationFilter.FilteringPhase.POST_FILTERING) {
            result = Collections.singleton(NotificationFilter.FilteringPhase.POST_FILTERING)
                .equals(filterFilteringPhases);
        // if pre-filtering is not enabled, but we request pre-filtering filters only, it doesn't really make sense
        // so we don't return any filters.
        } else if (!prefilteringEnabled
                && filteringPhase == NotificationFilter.FilteringPhase.PRE_FILTERING) {
            result = false;
        // In other cases, we only want to ensure that one of the request phase is supported.
        } else if (filterFilteringPhases.contains(filteringPhase)) {
            result = true;
        } else {
            result = false;
        }
        return result;
    }
}
