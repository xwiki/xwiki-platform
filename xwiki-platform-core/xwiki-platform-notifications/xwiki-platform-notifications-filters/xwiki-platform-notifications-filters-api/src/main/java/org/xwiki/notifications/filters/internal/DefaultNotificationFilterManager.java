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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.slf4j.Logger;
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
    public Collection<NotificationFilter> getAllFilters(DocumentReference user, boolean onlyEnabled)
        throws NotificationException
    {
        Collection<NotificationFilter> filters = getAllFilters(
                user.getWikiReference().getName().equals(wikiDescriptorManager.getMainWikiId()));
        Map<String, Boolean> filterActivations = getToggeableFilterActivations(user);
        Iterator<NotificationFilter> it = filters.iterator();
        while (it.hasNext()) {
            NotificationFilter filter = it.next();
            Boolean filterActivation = filterActivations.get(filter.getName());
            if (onlyEnabled && filterActivation != null && !filterActivation.booleanValue()) {
                it.remove();
            }
        }
        return filters;
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
    public Map<String, Boolean> getToggeableFilterActivations(DocumentReference user)
            throws NotificationException
    {
        return modelBridge.getToggeableFilterActivations(user);
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
