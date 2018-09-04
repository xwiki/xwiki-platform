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
package org.xwiki.user.internal.group;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.user.internal.group.AbstractGroupCache.GroupCacheEntry;

/**
 * Manipulate the cache of groups.
 * 
 * @version $Id$
 * @since 10.8RC1
 */
public abstract class AbstractGroupCache implements Initializable, CacheEntryListener<GroupCacheEntry>
{
    private static final int DEFAULT_CAPACITY = 500;

    /**
     * An entry of the group cache.
     * 
     * @version $Id$
     */
    public class GroupCacheEntry
    {
        private final String key;

        private Collection<DocumentReference> direct;

        private Collection<DocumentReference> all;

        GroupCacheEntry(String key)
        {
            this.key = key;
        }

        /**
         * @return direct the direct entities.
         */
        public Collection<DocumentReference> getDirect()
        {
            return this.direct;
        }

        /**
         * @param direct the direct entities.
         */
        public void setDirect(Collection<DocumentReference> direct)
        {
            this.direct = Collections.unmodifiableCollection(direct);

            addToIndex(this.key, direct);
        }

        /**
         * @return the recursive entities.
         */
        public Collection<DocumentReference> getAll()
        {
            return this.all;
        }

        /**
         * @param all the recursive entities.
         */
        public void setAll(Collection<DocumentReference> all)
        {
            this.all = Collections.unmodifiableCollection(all);

            addToIndex(this.key, all);
        }
    }

    @Inject
    protected EntityReferenceSerializer<String> serializer;

    protected Cache<GroupCacheEntry> cache;

    @Inject
    private CacheManager cacheManager;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    private final String id;

    /**
     * Keep an index of what's in the cache to clean just what's needed.
     */
    private Map<DocumentReference, Set<String>> cacheDocumentIndex = new HashMap<>();

    private Map<String, Set<DocumentReference>> cacheWikiIndex = new HashMap<>();

    /**
     * @param id the id of the cache
     */
    public AbstractGroupCache(String id)
    {
        this.id = id;
    }

    @Override
    public void initialize() throws InitializationException
    {
        CacheConfiguration cacheConfig = new CacheConfiguration();
        cacheConfig.setConfigurationId(this.id);
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(DEFAULT_CAPACITY);
        cacheConfig.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        try {
            this.cache = this.cacheManager.createNewCache(cacheConfig);
            this.cache.addCacheEntryListener(this);
        } catch (Exception e) {
            throw new InitializationException("Failed to create the group cache", e);
        }
    }

    protected GroupCacheEntry getCacheEntry(String key, DocumentReference reference, boolean create)
    {
        lockRead();

        GroupCacheEntry entry;
        try {
            entry = this.cache.get(key);
        } finally {
            unlockRead();
        }

        if (entry == null && create) {
            lockWrite();

            try {
                entry = new GroupCacheEntry(key);
                this.cache.set(key, entry);
                addToIndex(key, reference);
            } finally {
                unlockWrite();
            }
        }

        return entry;
    }

    private void addToIndex(String key, DocumentReference reference)
    {
        this.cacheDocumentIndex.computeIfAbsent(reference, k -> new HashSet<>()).add(key);
        this.cacheWikiIndex.computeIfAbsent(reference.getWikiReference().getName(), k -> new HashSet<>())
            .add(reference);
    }

    private void addToIndex(String key, Collection<DocumentReference> references)
    {
        for (DocumentReference reference : references) {
            addToIndex(key, reference);
        }
    }

    private void cleanIndex(String key, Collection<DocumentReference> references)
    {
        if (references != null) {
            for (DocumentReference reference : references) {
                Set<String> keys = this.cacheDocumentIndex.get(reference);

                if (keys != null) {
                    keys.remove(key);
                }
            }
        }
    }

    /**
     * Lock write lock.
     */
    public void lockWrite()
    {
        this.lock.writeLock().lock();
    }

    /**
     * Unlock write lock.
     */
    public void unlockWrite()
    {
        this.lock.writeLock().unlock();
    }

    /**
     * Lock read lock.
     */
    public void lockRead()
    {
        this.lock.readLock().lock();
    }

    /**
     * Unlock read lock.
     */
    public void unlockRead()
    {
        this.lock.readLock().unlock();
    }

    /**
     * Remove anything related to the passed reference from the cache.
     * 
     * @param reference the reference of the entity to remove from the cache
     */
    public void cleanCache(DocumentReference reference)
    {
        lockWrite();

        try {
            cleanDocumentCache(reference);

            Set<DocumentReference> references = this.cacheWikiIndex.get(reference.getWikiReference().getName());
            if (references != null) {
                references.remove(reference);
            }
        } finally {
            unlockWrite();
        }
    }

    private void cleanDocumentCache(DocumentReference reference)
    {
        Set<String> keys = this.cacheDocumentIndex.remove(reference);

        if (keys != null) {
            for (String key : keys) {
                this.cache.remove(key);
            }
        }
    }

    /**
     * Remove anything related to the passed wiki from the cache.
     * 
     * @param wiki the identifier of the wiki to remove from the cache
     */
    public void cleanCache(String wiki)
    {
        lockWrite();

        try {
            Set<DocumentReference> references = this.cacheWikiIndex.remove(wiki);

            if (references != null) {
                for (DocumentReference reference : references) {
                    cleanDocumentCache(reference);
                }
            }
        } finally {
            unlockWrite();
        }
    }

    /**
     * Empty the cache.
     */
    public void removeAll()
    {
        lockWrite();

        try {
            this.cache.removeAll();
            this.cacheDocumentIndex.clear();
            this.cacheWikiIndex.clear();
        } finally {
            unlockWrite();
        }
    }

    @Override
    public void cacheEntryAdded(CacheEntryEvent<GroupCacheEntry> event)
    {
        // Don't care
    }

    @Override
    public void cacheEntryModified(CacheEntryEvent<GroupCacheEntry> event)
    {
        // Don't care
    }

    @Override
    public void cacheEntryRemoved(CacheEntryEvent<GroupCacheEntry> event)
    {
        String key = event.getEntry().getKey();
        GroupCacheEntry entry = event.getEntry().getValue();

        // Workaround https://jira.xwiki.org/browse/XCOMMONS-1484
        if (entry != null) {
            cleanIndex(key, entry.getDirect());
            cleanIndex(key, entry.getAll());
        }
    }
}
