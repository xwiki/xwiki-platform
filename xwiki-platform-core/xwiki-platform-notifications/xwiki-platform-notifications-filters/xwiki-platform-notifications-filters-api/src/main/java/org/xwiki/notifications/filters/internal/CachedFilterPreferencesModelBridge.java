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
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.internal.reference.EntityReferenceFactory;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.notifications.NotificationException;
import org.xwiki.notifications.NotificationFormat;
import org.xwiki.notifications.filters.NotificationFilterPreference;
import org.xwiki.notifications.filters.NotificationFilterType;
import org.xwiki.notifications.preferences.internal.cache.UnboundedEntityCacheManager;

/**
 * Wrap the default {@link FilterPreferencesModelBridge} to store in the execution context the notification preferences to avoid fetching
 * them several time during the same HTTP request.
 *
 * @version $Id$
 * @since 9.5RC1
 */
@Component
@Named("cached")
@Singleton
public class CachedFilterPreferencesModelBridge implements FilterPreferencesModelBridge, Initializable
{
    private static final String PREFERENCEFILTERCACHE_NAME = "NotificationsFilterPreferences";

    private static final String TOGGLECACHE_NAME = "ToggeableFilterActivations";

    @Inject
    private FilterPreferencesModelBridge filterPreferencesModelBridge;

    @Inject
    private UnboundedEntityCacheManager cacheManager;

    @Inject
    private EntityReferenceFactory referenceFactory;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    private Map<EntityReference, Set<NotificationFilterPreference>> preferenceFilterCache;

    private Map<EntityReference, Map<String, ToggleableNotificationFilterActivation>> toggleCache;

    void invalidatePreferencefilter(EntityReference reference)
    {
        this.preferenceFilterCache.remove(reference);
    }

    @Override
    public void initialize() throws InitializationException
    {
        this.preferenceFilterCache = this.cacheManager.createCache(PREFERENCEFILTERCACHE_NAME, false);

        // TODO: optimize the invalidation to not be done on every user document modification
        this.toggleCache = this.cacheManager.createCache(TOGGLECACHE_NAME, true);
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(DocumentReference userReference)
        throws NotificationException
    {
        if (userReference == null) {
            return Collections.emptySet();
        }

        Set<NotificationFilterPreference> preferences = this.preferenceFilterCache.get(userReference);
        if (preferences != null) {
            return preferences;
        }

        preferences = this.filterPreferencesModelBridge.getFilterPreferences(userReference);

        this.preferenceFilterCache.put(this.referenceFactory.getReference(userReference), preferences);

        return preferences;
    }

    @Override
    public Set<NotificationFilterPreference> getFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        if (wikiReference == null) {
            return Collections.emptySet();
        }

        Set<NotificationFilterPreference> preferences = this.preferenceFilterCache.get(wikiReference);
        if (preferences != null) {
            return preferences;
        }

        preferences = this.filterPreferencesModelBridge.getFilterPreferences(wikiReference);

        this.preferenceFilterCache.put(this.referenceFactory.getReference(wikiReference), preferences);

        return preferences;
    }

    @Override
    public Map<String, ToggleableNotificationFilterActivation> getToggleableFilterActivations(
        DocumentReference userReference) throws NotificationException
    {
        Map<String, ToggleableNotificationFilterActivation> values = this.toggleCache.get(userReference);
        if (values != null) {
            return values;
        }

        values = this.filterPreferencesModelBridge.getToggleableFilterActivations(userReference);

        this.toggleCache.put(this.referenceFactory.getReference(userReference), values);

        return values;
    }

    @Override
    public void deleteFilterPreference(DocumentReference user, String filterPreferenceId) throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreference(user, filterPreferenceId);
    }

    @Override
    public void deleteFilterPreferences(DocumentReference user, Set<String> filterPreferenceIds)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreferences(user, filterPreferenceIds);
    }

    @Override
    public void deleteFilterPreferences(DocumentReference user) throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreferences(user);
        invalidateUserPreferencesFilters(user);
    }

    @Override
    public void deleteFilterPreference(WikiReference wikiReference, String filterPreferenceId)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.deleteFilterPreference(wikiReference, filterPreferenceId);
    }

    @Override
    public void deleteFilterPreferences(WikiReference wikiReference)
        throws NotificationException
    {
        // Remove the preferences from the database, then remove the preferences from the cache.
        this.filterPreferencesModelBridge.deleteFilterPreferences(wikiReference);
        invalidateWikiPreferenceFilters(wikiReference);
    }

    @Override
    public void setFilterPreferenceEnabled(DocumentReference user, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.setFilterPreferenceEnabled(user, filterPreferenceId, enabled);
    }

    @Override
    public void setFilterPreferenceEnabled(WikiReference wikiReference, String filterPreferenceId, boolean enabled)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.setFilterPreferenceEnabled(wikiReference, filterPreferenceId, enabled);
    }

    @Override
    public void saveFilterPreferences(WikiReference wikiReference,
        Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        this.filterPreferencesModelBridge.saveFilterPreferences(wikiReference, filterPreferences);
    }

    @Override
    public void saveFilterPreferences(DocumentReference user,
        Collection<NotificationFilterPreference> filterPreferences) throws NotificationException
    {
        this.filterPreferencesModelBridge.saveFilterPreferences(user, filterPreferences);
    }

    @Override
    public void setStartDateForUser(DocumentReference user, Date startDate) throws NotificationException
    {
        this.filterPreferencesModelBridge.setStartDateForUser(user, startDate);
    }

    @Override
    public void createScopeFilterPreference(DocumentReference user, NotificationFilterType type,
        Set<NotificationFormat> formats, List<String> eventTypes, EntityReference reference)
        throws NotificationException
    {
        this.filterPreferencesModelBridge.createScopeFilterPreference(user, type, formats, eventTypes, reference);
    }

    @Override
    public void createScopeFilterPreference(WikiReference wikiReference, NotificationFilterType type,
        Set<NotificationFormat> formats, List<String> eventTypes, EntityReference reference)
        throws NotificationException
    {
        this.filterPreferencesModelBridge
            .createScopeFilterPreference(wikiReference, type, formats, eventTypes, reference);
    }

    /**
     * Clear the whole cache.
     * 
     * @since 11.3RC1
     */
    public void clearCache()
    {
        this.preferenceFilterCache.clear();
        this.toggleCache.clear();
    }

    /**
     * Remove all the {@link NotificationFilterPreference}s related to entities from {@code wikiReference} from the
     * cache.
     *
     * @param wikiReference the wiki reference to invalidate
     */
    private void invalidateWikiPreferenceFilters(WikiReference wikiReference)
    {
        this.preferenceFilterCache.values()
            .forEach(set -> set.removeIf(filter -> filter.isFromWiki(wikiReference.getName())));
    }

    /**
     * Remove all the {@link  NotificationFilterPreference}s related to the given user form the cache.
     *
     * @param user the document reference of the user to remove from the cache
     */
    private void invalidateUserPreferencesFilters(DocumentReference user)
    {
        String serializedUserReference = this.entityReferenceSerializer.serialize(user);
        // Remove the filter preferences of the user from the cache.
        this.preferenceFilterCache.remove(user);
        // Remove the user from the filter preferences of the other entities of the cache. 
        this.preferenceFilterCache.values()
            .forEach(set -> set.removeIf(filter -> Objects.equals(filter.getUser(), serializedUserReference)));
    }
}
