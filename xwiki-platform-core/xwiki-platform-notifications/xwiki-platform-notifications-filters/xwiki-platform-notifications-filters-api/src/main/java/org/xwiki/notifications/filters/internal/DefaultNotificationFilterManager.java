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

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.manager.ComponentManager;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilter;
import org.xwiki.notifications.filters.NotificationFilterManager;
import org.xwiki.notifications.filters.NotificationFilterProvider;
import org.xwiki.notifications.preferences.NotificationPreference;

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
    @Inject
    private ComponentManager componentManager;

    @Override
    public Set<NotificationFilter> getAllFilters(DocumentReference user) throws NotificationException
    {
        Set<NotificationFilter> filters = new HashSet<>();

        /**
         * TODO: Handle filter conflicts using {@link NotificationFilterProvider#getProviderPriority()}.
         */
        for (NotificationFilterProvider provider : getProviders()) {
            filters.addAll(provider.getAllFilters(user));
        }

        return filters;
    }

    @Override
    public Set<NotificationFilter> getFilters(DocumentReference user,
            NotificationPreference preference) throws NotificationException
    {
        Set<NotificationFilter> filters = new HashSet<>();

        for (NotificationFilterProvider provider : getProviders()) {
            filters.addAll(provider.getFilters(user, preference));
        }

        return filters;
    }

    /**
     * @return a list of available {@link NotificationFilterProvider}.
     * @throws NotificationException if an error occurred
     */
    private List<NotificationFilterProvider> getProviders() throws NotificationException
    {
        try {
            return componentManager.getInstanceList(NotificationFilterProvider.class);
        } catch (ComponentLookupException e) {
            throw new NotificationException("Unable to retrieve a list of NotificationFilterProvider", e);
        }
    }
}
