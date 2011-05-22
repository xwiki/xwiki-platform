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
package org.xwiki.cache.util;

import javax.swing.event.EventListenerList;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xwiki.cache.Cache;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;

/**
 * Base class for {@link Cache} implementations. It provides events {@link DisposableCacheValue} management.
 * 
 * @param <T>
 * @version $Id$
 */
public abstract class AbstractCache<T> implements Cache<T>
{
    /**
     * The logger to use to log.
     */
    private static final Logger LOGGER = LoggerFactory.getLogger(AbstractCache.class);

    /**
     * The configuration used to create the cache.
     */
    protected CacheConfiguration configuration;

    /**
     * The list of listener to called when events appends on a cache entry.
     */
    protected final EventListenerList cacheEntryListeners = new EventListenerList();

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.Cache#dispose()
     */
    public void dispose()
    {
        for (CacheEntryListener<T> listener : this.cacheEntryListeners.getListeners(CacheEntryListener.class)) {
            this.cacheEntryListeners.remove(CacheEntryListener.class, listener);
        }
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.Cache#addCacheEntryListener(org.xwiki.cache.event.CacheEntryListener)
     */
    public void addCacheEntryListener(CacheEntryListener<T> listener)
    {
        this.cacheEntryListeners.add(CacheEntryListener.class, listener);
    }

    /**
     * {@inheritDoc}
     * 
     * @see org.xwiki.cache.Cache#removeCacheEntryListener(org.xwiki.cache.event.CacheEntryListener)
     */
    public void removeCacheEntryListener(CacheEntryListener<T> listener)
    {
        cacheEntryListeners.remove(CacheEntryListener.class, listener);
    }

    /**
     * Helper method to send event when a new cache entry is inserted.
     * 
     * @param event the event to send.
     */
    protected void sendEntryAddedEvent(CacheEntryEvent<T> event)
    {
        for (org.xwiki.cache.event.CacheEntryListener<T> listener
                : this.cacheEntryListeners.getListeners(org.xwiki.cache.event.CacheEntryListener.class)) {
            listener.cacheEntryAdded(event);
        }
    }

    /**
     * Helper method to send event when an existing cache entry is removed.
     * 
     * @param event the event to send.
     */
    protected void sendEntryRemovedEvent(CacheEntryEvent<T> event)
    {
        for (org.xwiki.cache.event.CacheEntryListener<T> listener
                : this.cacheEntryListeners.getListeners(org.xwiki.cache.event.CacheEntryListener.class)) {
            listener.cacheEntryRemoved(event);
        }

        disposeCacheValue(event.getEntry().getValue());
    }

    /**
     * Helper method to send event when a cache entry is modified.
     * 
     * @param event the event to send.
     */
    protected void sendEntryModifiedEvent(CacheEntryEvent<T> event)
    {
        for (org.xwiki.cache.event.CacheEntryListener<T> listener
                : this.cacheEntryListeners.getListeners(org.xwiki.cache.event.CacheEntryListener.class)) {
            listener.cacheEntryModified(event);
        }
    }

    /**
     * Dispose the value being removed from the cache.
     * 
     * @param value the value to dispose
     */
    protected void disposeCacheValue(T value)
    {
        if (value instanceof DisposableCacheValue) {
            try {
                ((DisposableCacheValue) value).dispose();
            } catch (Throwable e) {
                if (LOGGER.isWarnEnabled()) {
                    LOGGER.warn("Error when trying to dispose a cache object of cache ["
                        + configuration.getConfigurationId() + "]", e);
                }
            }
        }
    }
}
