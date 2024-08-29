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
package org.xwiki.notifications.preferences.internal.cache;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.component.manager.ComponentLifecycleException;
import org.xwiki.component.phase.Disposable;
import org.xwiki.management.JMXBeanRegistration;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Produce caches with infinite size.
 * <p>
 * In the context of the pre-filtering of events it's important to have a full caching of anything expensive (like
 * loading a document which is not yet in the document cache) as otherwise the document cache can become quickly
 * unusable if there is too much users for the size of the document cache (which is quite common in a very big
 * instance).
 * 
 * @version $Id$
 * @since 13.8RC1
 * @since 13.4.4
 * @since 12.10.10
 */
@Component(roles = UnboundedEntityCacheManager.class)
@Singleton
public class UnboundedEntityCacheManager implements Disposable
{
    private final Map<String, UnboundedEntityCache<?>> caches = new ConcurrentHashMap<>();

    @Inject
    private JMXBeanRegistration jmx;

    @Override
    public void dispose() throws ComponentLifecycleException
    {
        // Unregister all the JMX beans
        this.caches.values().forEach(c -> this.jmx.unregisterMBean(c.getJmxName()));
    }

    /**
     * @param <T> the type of data stored in the cache
     * @param name the name of the cache
     * @param invalidateOnUpdate true of the cache entries should be invalidated on document update, false for
     *            invalidating them only on delete
     * @return the created cache instance
     */
    public <T> Map<EntityReference, T> createCache(String name, boolean invalidateOnUpdate)
    {
        UnboundedEntityCache<T> cache = new UnboundedEntityCache<>(name, invalidateOnUpdate);

        this.caches.put(cache.getName(), cache);

        // Register a JMX bean
        this.jmx.registerMBean(new JMXUnboundedEntityCache<>(cache), cache.getJmxName());

        return cache.getCache();
    }

    /**
     * @param <T> the type of data stored in the cache
     * @param name the name of the cache
     * @return the cache
     */
    public <T> Map<EntityReference, T> getCache(String name)
    {
        UnboundedEntityCache<T> cache = (UnboundedEntityCache<T>) this.caches.get(name);

        return cache != null ? cache.getCache() : null;
    }

    /**
     * @param entityReference invalidate all caches entries related to update of the passed document
     */
    public void update(DocumentReference entityReference)
    {
        for (UnboundedEntityCache<?> cache : this.caches.values()) {
            if (cache.isInvalidateOnUpdate()) {
                cache.getCache().remove(entityReference);
            }
        }
    }

    /**
     * @param entityReference invalidate all caches entries related to deletion of the passed document
     */
    public void remove(DocumentReference entityReference)
    {
        this.caches.values().forEach(c -> c.getCache().remove(entityReference));
    }

    /**
     * @param wikiId invalidate all caches entries related to the deleting of the passed wiki
     */
    public void remove(String wikiId)
    {
        this.caches.values().forEach(c -> remove(c.getCache(), wikiId));
    }

    private <T> void remove(Map<EntityReference, T> cache, String wikiId)
    {
        // Remove all entries associated with the wiki
        for (Iterator<Map.Entry<EntityReference, T>> it = cache.entrySet().iterator(); it.hasNext();) {
            Map.Entry<EntityReference, T> entry = it.next();

            if (entry.getKey().extractReference(EntityType.WIKI).getName().equals(wikiId)) {
                it.remove();
            }
        }
    }
}
