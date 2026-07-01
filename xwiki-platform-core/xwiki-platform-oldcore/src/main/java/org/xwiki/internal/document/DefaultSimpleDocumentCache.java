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
package org.xwiki.internal.document;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;

import javax.inject.Inject;

import org.apache.commons.lang3.function.FailableFunction;
import org.xwiki.bridge.event.DocumentCreatedEvent;
import org.xwiki.bridge.event.DocumentDeletedEvent;
import org.xwiki.bridge.event.DocumentUpdatedEvent;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.annotation.InstantiationStrategy;
import org.xwiki.component.descriptor.ComponentInstantiationStrategy;
import org.xwiki.component.phase.Disposable;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.observation.AbstractEventListener;
import org.xwiki.observation.EventListener;
import org.xwiki.observation.ObservationManager;
import org.xwiki.observation.event.Event;

import com.google.common.util.concurrent.Striped;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * A cache that allows caching a single value per document reference.
 *
 * @param <C> the type of the data stored in the cache
 * @param <E> the type of the exception that can be thrown by the provider
 * @version $Id$
 */
@Component
@InstantiationStrategy(ComponentInstantiationStrategy.PER_LOOKUP)
public class DefaultSimpleDocumentCache<C, E extends Throwable> implements Disposable, SimpleDocumentCache<C, E>
{
    @Inject
    private ObservationManager observationManager;

    @Inject
    private CacheManager cacheManager;

    @Inject
    private EntityReferenceSerializer<String> serializer;

    private Cache<C> cache;

    private Listener listener;

    // Use a read-write lock to ensure that during cache invalidation, no values are computed based on outdated data.
    // Use a striped lock to ensure that the removal of one value doesn't block the setting of other values.
    private final Striped<ReadWriteLock> locks = Striped.readWriteLock(16);

    private class Listener extends AbstractEventListener
    {
        Listener(String name)
        {
            super(name, new DocumentCreatedEvent(), new DocumentUpdatedEvent(), new DocumentDeletedEvent());
        }

        @Override
        public void onEvent(Event event, Object source, Object data)
        {
            XWikiDocument doc = (XWikiDocument) source;
            remove(doc.getDocumentReference());
        }
    }

    @Override
    public synchronized void initializeCache(CacheConfiguration cacheConfiguration) throws CacheException
    {
        // If the cache has already been created, dispose the existing one and create a new one.
        if (this.cache != null) {
            dispose();
        }

        this.cache = this.cacheManager.createNewCache(cacheConfiguration);
        this.listener = new Listener(cacheConfiguration.getConfigurationId());
        this.observationManager.addListener(this.listener, EventListener.CACHE_INVALIDATION_DEFAULT_PRIORITY);
    }

    @Override
    public C get(DocumentReference documentReference, FailableFunction<DocumentReference, C, E> provider) throws E
    {
        String key = getKey(documentReference);
        C result = this.cache.get(key);

        if (result == null) {
            // Use a read lock to ensure that we don't compute a new value based on an old document while an
            // invalidation is running. We don't care about computing several times in parallel.
            Lock lock = this.locks.get(key).readLock();
            lock.lock();
            try {
                result = provider.apply(documentReference);
                this.cache.set(key, result);
            } finally {
                lock.unlock();
            }
        }

        return result;
    }

    /**
     * Remove the value associated with the provided document reference.
     *
     * @param documentReference the reference of the document
     */
    public void remove(DocumentReference documentReference)
    {
        String key = getKey(documentReference);
        // Use a write lock to ensure that we don't compute a new value based on an old document.
        Lock lock = this.locks.get(key).writeLock();
        lock.lock();
        try {
            this.cache.remove(key);
        } finally {
            lock.unlock();
        }
    }

    private String getKey(DocumentReference documentReference)
    {
        return this.serializer.serialize(documentReference);
    }

    @Override
    public void dispose()
    {
        if (this.cache != null) {
            this.cache.dispose();
        }

        this.observationManager.removeListener(this.listener.getName());
    }
}
