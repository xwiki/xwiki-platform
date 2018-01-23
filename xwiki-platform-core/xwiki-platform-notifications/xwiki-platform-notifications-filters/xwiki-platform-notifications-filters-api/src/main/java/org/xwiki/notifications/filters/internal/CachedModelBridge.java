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
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
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

    private static final String CONTEXT_KEY_FORMAT = "%s_[%s]";

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException
    {
        // We need to store the user reference in the cache's key, otherwise all users of the same context will share
        // the same cache, which can happen when a notification email is triggered.
        final String contextEntry = String.format(CONTEXT_KEY_FORMAT, USER_FILTER_PREFERENCES,
                serializer.serialize(user));

        ExecutionContext context = execution.getContext();
        Object cachedPreferences = context.getProperty(contextEntry);
        if (cachedPreferences != null && cachedPreferences instanceof Set) {
            return (Set<NotificationFilterPreference>) cachedPreferences;
        }

        Set<NotificationFilterPreference> preferences = modelBridge.getFilterPreferences(user);
        context.setProperty(contextEntry, preferences);

        return preferences;
    }

    @Override
    public Set<String> getDisabledNotificationFiltersHints(DocumentReference user)
            throws NotificationException
    {
        // We need to store the user reference in the cache's key, otherwise all users of the same context will share
        // the same cache, which can happen when a notification email is triggered.
        final String contextEntry = String.format(CONTEXT_KEY_FORMAT, USER_TOGGLEABLE_FILTER_PREFERENCES,
            serializer.serialize(user));

        ExecutionContext context = execution.getContext();
        if (context.hasProperty(contextEntry)) {
            return (Set<String>) context.getProperty(contextEntry);
        }

        Set<String> disabledFiltersHints = modelBridge.getDisabledNotificationFiltersHints(user);
        context.setProperty(contextEntry, disabledFiltersHints);

        return disabledFiltersHints;
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceName) throws NotificationException
    {
        modelBridge.deleteFilterPreference(user, filterPreferenceName);
        clearCache();
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceName, boolean enabled)
            throws NotificationException
    {
        modelBridge.setFilterPreferenceEnabled(user, filterPreferenceName, enabled);
        clearCache();
    }

    @Override
    public void saveFilterPreferences(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        modelBridge.saveFilterPreferences(user, filterPreferences);
        clearCache();
    }

    private void clearCache()
    {
        ExecutionContext context = execution.getContext();
        for (String key: context.getProperties().keySet()) {
            if (key.startsWith(USER_FILTER_PREFERENCES) || key.startsWith(USER_TOGGLEABLE_FILTER_PREFERENCES)) {
                context.removeProperty(key);
            }
        }
    }
}
