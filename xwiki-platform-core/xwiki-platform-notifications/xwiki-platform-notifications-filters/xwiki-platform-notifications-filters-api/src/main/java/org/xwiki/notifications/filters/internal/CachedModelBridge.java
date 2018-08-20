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
import java.util.Date;
import java.util.Map;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLookupException;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.context.Execution;
import org.xwiki.context.ExecutionContext;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;

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
public class CachedModelBridge implements ModelBridge, Initializable
{
    private static final String USER_TOGGLEABLE_FILTER_PREFERENCES = "userToggleableFilterPreference";

    private static final String CONTEXT_KEY_FORMAT = "%s_[%s]";

    private static final String CACHE_NAME = "NotificationsFiltersPreferencesCache";

    @Inject
    private ModelBridge modelBridge;

    @Inject
    private Execution execution;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    @Inject
    private CacheManager cacheManager;

    private Cache<Set<NotificationFilterPreference>> cache;

    @Override
    public void initialize() throws InitializationException
    {
        try {
            CacheConfiguration configuration = new CacheConfiguration(CACHE_NAME);
            CacheFactory cacheFactory = cacheManager.getCacheFactory();
            this.cache = cacheFactory.newCache(configuration);
        } catch (ComponentLookupException | CacheException e) {
            throw new InitializationException(
                    String.format("Failed to initialize the notification filters preferences cache [%s].", CACHE_NAME),
                    e);
        }
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference user) throws NotificationException
    {
        String userId = serializer.serialize(user);

        Set<NotificationFilterPreference> preferences = cache.get(userId);
        if (preferences != null) {
            return preferences;
        }

        preferences = modelBridge.getFilterPreferences(user);

        cache.set(userId, preferences);

        return preferences;
    }

    @Override
    public Map<String, Boolean> getToggeableFilterActivations(DocumentReference user) throws NotificationException
    {
        // We need to store the user reference in the cache's key, otherwise all users of the same context will share
        // the same cache, which can happen when a notification email is triggered.
        final String contextEntry = String.format(CONTEXT_KEY_FORMAT, USER_TOGGLEABLE_FILTER_PREFERENCES,
            serializer.serialize(user));

        ExecutionContext context = execution.getContext();
        if (context.hasProperty(contextEntry)) {
            return (Map<String, Boolean>) context.getProperty(contextEntry);
        }

        Map<String, Boolean> values = modelBridge.getToggeableFilterActivations(user);
        context.setProperty(contextEntry, values);

        return values;
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        modelBridge.deleteFilterPreference(user, filterPreferenceId);
        clearCache(user);
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
            throws NotificationException
    {
        modelBridge.setFilterPreferenceEnabled(user, filterPreferenceId, enabled);
        clearCache(user);
    }

    @Override
    public void saveFilterPreferences(DocumentReference user,
            Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        modelBridge.saveFilterPreferences(user, filterPreferences);
        clearCache(user);
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        modelBridge.setStartDateForUser(user, startDate);
        clearCache(user);
    }

    @Override
    public void createScopeFilterPreference(DocumentReference user, NotificationFilterType type,
            Set<NotificationFormat> formats, String eventType, EntityReference reference) throws NotificationException
    {
        modelBridge.createScopeFilterPreference(user, type, formats, eventType, reference);
        clearCache(user);
    }

    private void clearCache(DocumentReference user)
    {
        String userId = serializer.serialize(user);
        cache.remove(userId);

        ExecutionContext context = execution.getContext();
        for (String key: new ArrayList<>(context.getProperties().keySet())) {
            if (key.startsWith(USER_TOGGLEABLE_FILTER_PREFERENCES)) {
                context.removeProperty(key);
            }
        }
    }
}
