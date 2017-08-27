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

import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.filters.NotificationFilterPreference;

/**
 * Wrap the default {@link ModelBridge} to store in the execution context the notification preferences to avoid
 * fetching them several time during the same HTTP request.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named("cached")
@Singleton
public class CachedModelBridge implements ModelBridge
{
    private static final String USER_TOGGLEABLE_FILTER_PREFERENCES = "userToggleableFilterPreference";

    private static final String USER_FILTER_PREFERENCES = "userAllNotificationFilterPreferences";

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Execution execution;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException
    {
        final String contextEntry = USER_FILTER_PREFERENCES;

        ExecutionContext context = execution.getContext();
        // TODO: Somehow, this cache doesn't work : the context returns null instead of a set.
        /*
        if (context.hasProperty(contextEntry)) {
            return (Set<NotificationFilterPreference>) context.getProperty(contextEntry);
        }
        */

        Set<NotificationFilterPreference> preferences = modelBridge.getFilterPreferences(user);
        context.setProperty(contextEntry, preferences);

        return preferences;
    }

    @Override
    public Set<String> getDisabledNotificationFiltersHints(DocumentReference user)
            throws NotificationException
    {
        final String contextEntry = USER_TOGGLEABLE_FILTER_PREFERENCES;

        ExecutionContext context = execution.getContext();
        if (context.hasProperty(contextEntry)) {
            return (Set<String>) context.getProperty(contextEntry);
        }

        Set<String> disabledFiltersHints = modelBridge.getDisabledNotificationFiltersHints(user);
        context.setProperty(contextEntry, disabledFiltersHints);

        return disabledFiltersHints;
    }
}
