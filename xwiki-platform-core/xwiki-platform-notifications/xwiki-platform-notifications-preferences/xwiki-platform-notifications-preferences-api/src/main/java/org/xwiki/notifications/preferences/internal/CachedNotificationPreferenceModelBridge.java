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
package org.xwiki.notifications.preferences.internal;

import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.preferences.NotificationPreference;
import org.xwiki.notifications.preferences.internal.cache.UnboundedEntityCacheManager;

/**
 * Wrap the default {@link NotificationPreferenceModelBridge} to store in the execution context the notification
 * preferences to avoid fetching them several time during the same HTTP request.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named("cached")
@Singleton
public class CachedNotificationPreferenceModelBridge implements NotificationPreferenceModelBridge, Initializable
{
    private static final String CACHE_NAME = "NotificationsPreferences";
    private static final String EVENT_GROUPING_CACHE_NAME = "EventGroupingNotificationsPreferences";

    @Inject
    private NotificationPreferenceModelBridge notificationPreferenceModelBridge;

    @Inject
    private UnboundedEntityCacheManager cacheManager;

    @Inject
    private EntityReferenceFactory referenceFactory;

    private Map<EntityReference, List<NotificationPreference>> preferenceCache;
    private Map<EntityReference, Map<String, String>> eventGroupingStrategyCache;

    @Override
    public void initialize() throws InitializationException
    {
        // TODO: optimize the invalidation not not be on every user document modification
        this.preferenceCache = this.cacheManager.createCache(CACHE_NAME, true);
        this.eventGroupingStrategyCache = this.cacheManager.createCache(EVENT_GROUPING_CACHE_NAME, true);
    }

    @Override
    public List<NotificationPreference> getNotificationsPreferences(DocumentReference userReference)
        throws NotificationException
    {
        if (userReference == null) {
            return Collections.emptyList();
        }

        List<NotificationPreference> preferences = this.preferenceCache.get(userReference);
        if (preferences != null) {
            return preferences;
        }

        preferences = this.notificationPreferenceModelBridge.getNotificationsPreferences(userReference);

        this.preferenceCache.put(this.referenceFactory.getReference(userReference), preferences);

        return preferences;
    }

    @Override
    public List<NotificationPreference> getNotificationsPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        if (wikiReference == null) {
            return Collections.emptyList();
        }

        List<NotificationPreference> preferences = this.preferenceCache.get(wikiReference);
        if (preferences != null) {
            return preferences;
        }

        preferences = this.notificationPreferenceModelBridge.getNotificationsPreferences(wikiReference);

        this.preferenceCache.put(this.referenceFactory.getReference(wikiReference), preferences);

        return preferences;
    }

    @Override
    public void setStartDateForUser(DocumentReference userReference, Date startDate) throws NotificationException
    {
        this.notificationPreferenceModelBridge.setStartDateForUser(userReference, startDate);
    }

    @Override
    public void saveNotificationsPreferences(DocumentReference userReference,
        List<NotificationPreference> notificationPreferences) throws NotificationException
    {
        this.notificationPreferenceModelBridge.saveNotificationsPreferences(userReference, notificationPreferences);
    }

    /**
     * @param reference the reference of the entity to invalidate
     * @since 13.4.4
     * @since 12.10.10
     * @since 13.8RC1
     */
    public void invalidatePreference(EntityReference reference)
    {
        this.preferenceCache.remove(reference);
        this.eventGroupingStrategyCache.remove(reference);
    }

    @Override
    public String getEventGroupingStrategyHint(DocumentReference userDocReference, String target)
        throws NotificationException
    {
        Map<String, String> strategyHints =
                this.eventGroupingStrategyCache.computeIfAbsent(userDocReference, reference -> new HashMap<>());
        if (!strategyHints.containsKey(target)) {
            String strategyHint =
                this.notificationPreferenceModelBridge.getEventGroupingStrategyHint(userDocReference, target);
            strategyHints.put(target, strategyHint);
        }
        return strategyHints.get(target);
    }
}
