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
package org.xwiki.notifications.notifiers.internal;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.LRUCacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Disposable;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.notifications.CompositeEvent;
import org.xwiki.notifications.NotificationConfiguration;
import org.xwiki.notifications.sources.NotificationParameters;

/**
 * A cache manager dedicated to Notifications.
 * This component handles two caches: one for the {@link CompositeEvent} instances and another for the count of events.
 * It avoids to have to reload in memory the events each time. The caches are handled with a key
 * computed with an instance of {@link NotificationParameters}.
 *
 * Note that this component is useless if the property {@code notifications.rest.cache} is set to true.
 *
 * @since 12.2
 * @version $Id$
 */
@Component(roles = DefaultNotificationCacheManager.class)
@Singleton
public class DefaultNotificationCacheManager implements Initializable, Disposable
{
    private static final String CACHE_KEY_SEPARATOR = "/";

    @Inject
    private NotificationConfiguration configuration;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private EntityReferenceSerializer<String> entityReferenceSerializer;

    /**
     * Cache used to store events result until the result might change (for example when a new notification is
     * created).
     */
    private Cache<List<Object>> longEventCache;

    /**
     * Cache used to store composite events result until the result might change (for example when a new notification is
     * created).
     */
    private Cache<List<Object>> longCompositeEventCache;

    /**
     * Cache used to store individual events count result until the result might change (for example when a new
     * notification is created).
     */
    private Cache<Integer> longIndividualEventCountCache;

    /**
     * Cache used to store composite events count result until the result might change (for example when a new
     * notification is created).
     */
    private Cache<Integer> longCompositeEventCountCache;

    /**
     * Number of times the caches have been flushed. It is part of the cache keys so that a result computed from
     * events that have changed in the meantime is stored under a key that is never read again, and thus cannot be
     * returned instead of an up-to-date result.
     */
    private final AtomicLong epoch = new AtomicLong();

    @Override
    public void initialize() throws InitializationException
    {
        if (this.configuration.isRestCacheEnabled()) {
            try {
                this.longEventCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.longCache.events", 100, 86400));
                this.longIndividualEventCountCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.longCache.count", 10000, 86400));

                this.longCompositeEventCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.longCache.events.composite", 100,
                        86400));
                this.longCompositeEventCountCache = this.cacheManager
                    .createNewCache(new LRUCacheConfiguration("notification.rest.longCache.count.composite", 10000,
                        86400));
            } catch (CacheException e) {
                throw new InitializationException("Failed to create one of the notification cache.", e);
            }
        }
    }

    private void addCacheKeyElement(StringBuilder cacheKeyBuilder, String value)
    {
        if (value != null) {
            // append value length as a separator, so that we don't need to escape the actual / separators.
            cacheKeyBuilder.append(value.length());
            cacheKeyBuilder.append(value);
        }
    }

    /**
     * Compute a key based on a {@link NotificationParameters}.
     * Allows to properly link events on a request performed on
     * {@link org.xwiki.notifications.sources.ParametrizedNotificationManager}.
     * @param notificationParameters the parameters used to retrieve events.
     * @return a unique key corresponding to these parameters.
     */
    public String createCacheKey(NotificationParameters notificationParameters)
    {
        StringBuilder cacheKeyBuilder = new StringBuilder();
        addCacheKeyElement(cacheKeyBuilder, notificationParameters.format.name());
        cacheKeyBuilder.append(CACHE_KEY_SEPARATOR);

        addCacheKeyElement(cacheKeyBuilder, this.entityReferenceSerializer.serialize(notificationParameters.user));
        cacheKeyBuilder.append(CACHE_KEY_SEPARATOR);

        if (notificationParameters.endDate != null) {
            cacheKeyBuilder.append(notificationParameters.endDate.getTime())
                .append(CACHE_KEY_SEPARATOR);
            cacheKeyBuilder.append(notificationParameters.endDateIncluded)
                .append(CACHE_KEY_SEPARATOR);
        }

        cacheKeyBuilder
            .append(notificationParameters.expectedCount)
            .append(CACHE_KEY_SEPARATOR)
            .append(notificationParameters.onlyUnread)
            .append(CACHE_KEY_SEPARATOR)
            .append(notificationParameters.hashCode());

        return cacheKeyBuilder.toString();
    }

    /**
     * @param cacheKey the key where the event are stored.
     * @param count {@code true} if the value to return is a count instead of a list of events
     * @param composite {@code true} if the value to return is about composite events or individual events
     * @param epoch the epoch obtained from {@link #getEpoch()} before the events are retrieved
     * @return the value associated with the passed parameters
     */
    public Object getFromCache(String cacheKey, boolean count, boolean composite, long epoch)
    {
        Object result = null;
        if (this.configuration.isRestCacheEnabled()) {
            String epochCacheKey = getEpochCacheKey(cacheKey, epoch);
            if (count && composite) {
                result = this.longCompositeEventCountCache.get(epochCacheKey);
            } else if (count && !composite) {
                result = this.longIndividualEventCountCache.get(epochCacheKey);
            } else if (!count && composite) {
                result = this.longCompositeEventCache.get(epochCacheKey);
            } else {
                result = this.longEventCache.get(epochCacheKey);
            }
        }

        return result;
    }

    /**
     * The returned value needs to be obtained before the events are retrieved, and given back to
     * {@link #getFromCache(String, boolean, boolean, long)} and
     * {@link #setInCache(String, List, boolean, boolean, long)} so that a result is only ever read with the events it
     * has been computed from.
     *
     * @return the current epoch
     */
    public long getEpoch()
    {
        return this.epoch.get();
    }

    /**
     * Record in cache the events and their number.
     * <p>
     * The value is stored under a key that includes the given epoch, so that a result computed from events that have
     * changed in the meantime is never returned by {@link #getFromCache(String, boolean, boolean, long)}, which only
     * ever looks up the current epoch.
     *
     * @param cacheKey the key to store the given events.
     * @param count if {@code true} only store the number of events; else store the objects.
     * @param events the events to store in cache. Their size will be stored too.
     * @param composite {@code true} if the value to store is about composite events or individual events
     * @param epoch the epoch obtained from {@link #getEpoch()} before the events were retrieved
     */
    public void setInCache(String cacheKey, List<Object> events, boolean count, boolean composite, long epoch)
    {
        // The events changed while this result was being computed, so nobody will ever read the entry that would be
        // stored below: skip it to avoid evicting entries that are still useful. This is only an optimization, the
        // epoch in the cache key is what guarantees that no outdated result is ever returned.
        if (this.epoch.get() != epoch) {
            return;
        }

        if (this.configuration.isRestCacheEnabled()) {
            String epochCacheKey = getEpochCacheKey(cacheKey, epoch);
            if (count && composite) {
                this.longCompositeEventCountCache.set(epochCacheKey, events.size());
            } else if (count && !composite) {
                this.longIndividualEventCountCache.set(epochCacheKey, events.size());
            } else if (!count && composite) {
                this.longCompositeEventCache.set(epochCacheKey, events);
            } else {
                this.longEventCache.set(epochCacheKey, events);
            }
        }
    }

    private String getEpochCacheKey(String cacheKey, long epoch)
    {
        return cacheKey + CACHE_KEY_SEPARATOR + epoch;
    }

    /**
     * Empty the long cache.
     */
    public void flushLongCache()
    {
        if (this.configuration.isRestCacheEnabled()) {
            // Change the epoch before emptying the caches. This is what actually invalidates the caches: the entries
            // stored under the previous epoch are never read again. Doing it first ensures that a result computed
            // from the events before this flush cannot be read after it, even if it is stored after the caches have
            // been emptied.
            this.epoch.incrementAndGet();

            // Empty the caches to free the memory used by the entries of the previous epochs. Entries stored after
            // this point by computations that are still running are left behind and only removed by the eviction of
            // the caches.
            this.longEventCache.removeAll();
            this.longIndividualEventCountCache.removeAll();
            this.longCompositeEventCache.removeAll();
            this.longCompositeEventCountCache.removeAll();
        }
    }

    @Override
    public void dispose()
    {
        if (this.configuration.isRestCacheEnabled()) {
            this.longIndividualEventCountCache.dispose();
            this.longEventCache.dispose();
            this.longCompositeEventCache.dispose();
            this.longCompositeEventCountCache.dispose();
        }
    }
}
