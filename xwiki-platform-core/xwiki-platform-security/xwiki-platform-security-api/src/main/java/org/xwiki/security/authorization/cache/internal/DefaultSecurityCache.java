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
package org.xwiki.security.authorization.cache.internal;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityCache;

/**
 * Default implementation of the security cache.
 *
 * @version $Id$
 * @since 4.0M2 
 */
@Component
@Singleton
public class DefaultSecurityCache implements SecurityCache, Initializable
{
    /** Default capacity for security cache. */
    private static final int DEFAULT_CAPACITY = 500;

    /** Logger. **/
    @Inject
    private Logger logger;

    /** Fair read-write lock used for fair scheduling of cache access. */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    /** Fair read lock. */
    private final Lock readLock = readWriteLock.readLock();

    /** Fair write lock. */
    private final Lock writeLock = readWriteLock.writeLock();

    /** The keys in the cache are generated from instances of {@link org.xwiki.model.reference.EntityReference}. */
    @Inject
    private EntityReferenceSerializer<String> keySerializer;

    /** Cache manager to create the cache. */
    @Inject
    private CacheManager cacheManager;

    /** The cache instance. */
    private Cache<SecurityCacheEntry> cache;

    /**
     * @return a new configured security cache
     * @throws InitializationException if a CacheException arise during creation
     */
    private Cache<SecurityCacheEntry> newCache() throws InitializationException
    {
        CacheConfiguration cacheConfig = new CacheConfiguration();
        cacheConfig.setConfigurationId("platform.security.authorization.cache");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        lru.setMaxEntries(DEFAULT_CAPACITY);
        cacheConfig.put(LRUEvictionConfiguration.CONFIGURATIONID, lru);
        try {
            return cacheManager.createNewCache(cacheConfig);
        } catch (Exception e) {
            throw new InitializationException("Unable to create the security cache with a capacity of "
                + lru.getMaxEntries(), e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        cache = newCache();
        cache.addCacheEntryListener(new Listener());
    }

    /**
     * Cache entry.
     */
    private class SecurityCacheEntry
    {
        /**
         * The cached security entry.
         */
        private SecurityEntry entry;

        /**
         * Parents of this cached entry.
         */
        private Collection<SecurityCacheEntry> parents;

        /**
         * Children of this cached entry.
         */
        private Collection<SecurityCacheEntry> children;

        /**
         * True if this entry has been removed.
         */
        private boolean disposed;

        /**
         * Create a new cache entry for a security rule, linking it to its parent.
         * @param entry the security rule entry to cache.
         * @throws ParentEntryEvictedException if the parent required is no more available in the cache.
         */
        SecurityCacheEntry(SecurityRuleEntry entry) throws ParentEntryEvictedException
        {
            this.entry = entry;
            SecurityReference parentReference = entry.getReference().getParentSecurityReference();
            if (parentReference != null) {
                SecurityCacheEntry parent = DefaultSecurityCache.this.getEntry(parentReference);
                if (parent == null) {
                    throw new ParentEntryEvictedException();
                }
                this.parents = Arrays.asList(parent);
                parent.addChild(this);
                logNewEntry();
            } else {
                this.parents = null;
                logNewEntry();
            }
        }

        /**
         * Create a new cache entry for a user access, linking it to the related entity and user.
         * @param entry the security access entry to cache.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityAccessEntry entry) throws ParentEntryEvictedException
        {
            this.entry = entry;
            boolean isSelf = entry.getReference().equals(entry.getUserReference());
            SecurityCacheEntry parent1 = DefaultSecurityCache.this.getEntry(entry.getReference());
            SecurityCacheEntry parent2 = (isSelf) ? parent1
                : DefaultSecurityCache.this.getEntry(entry.getUserReference());
            if (parent1 == null || parent2 == null) {
                throw new ParentEntryEvictedException();
            }
            this.parents = (isSelf) ? Arrays.asList(parent1) : Arrays.asList(parent1, parent2);
            parent1.addChild(this);
            if (!isSelf) {
                parent2.addChild(this);
            }
            logNewEntry();
        }

        /**
         * Create a new cache entry for a user rule entry, linking it to its parent and to all provided groups.
         * @param entry the security rule entry to cache.
         * @param groups the list of groups to link this entry to.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
            throws ParentEntryEvictedException
        {
            this.entry = entry;
            SecurityReference entity = entry.getReference();
            SecurityReference parentReference = entry.getReference().getParentSecurityReference();
            int parentSize = groups.size() + ((parentReference == null) ? 0 : 1);
            if (parentSize > 0) {
                this.parents = new ArrayList<SecurityCacheEntry>(parentSize);
                if (parentReference != null) {
                    SecurityCacheEntry parent = DefaultSecurityCache.this.getEntry(entity.getParentSecurityReference());
                    if (parent == null) {
                        throw new ParentEntryEvictedException();
                    }
                    this.parents.add(parent);
                    parent.addChild(this);
                }
                for (GroupSecurityReference group : groups) {
                    if (group.equals(parentReference)) {
                        continue;
                    }
                    SecurityCacheEntry parent = DefaultSecurityCache.this.getEntry(group);
                    if (parent == null) {
                        throw new ParentEntryEvictedException();
                    }
                    this.parents.add(parent);
                    parent.addChild(this);
                }
                logNewEntry();
            } else {
                this.parents = null;
                logNewEntry();
            }
        }

        /**
         * Log the new entry creation.
         */
        private void logNewEntry() 
        {
            if (logger.isDebugEnabled()) {
                if (parents == null || parents.size() == 0) {
                    logger.debug("New orphan entry [{}].", getKey());
                    return;
                }
                StringBuilder sb = new StringBuilder("New entry [");
                sb.append(getKey()).append("] as child of ");
                boolean first = true;
                for (SecurityCacheEntry parent : parents) {
                    if (!first) {
                        sb.append(", ");
                    } else {
                        first = false;
                    }
                    sb.append('[').append(parent.getKey()).append(']');
                }
                sb.append(".");
                logger.debug(sb.toString());
            }
        }

        /**
         * @return the original security entry cached in this cache entry.
         */
        SecurityEntry getEntry() 
        {
            return this.entry;
        }

        /**
         * @return the serialized key of this entry.
         */
        String getKey() 
        {
            return DefaultSecurityCache.this.getEntryKey(entry);
        }

        /**
         * Dispose this entry from the cache, removing all children relation in its parents, and removing
         * all its children recursively. This method is not thread safe in regards to the cache, proper
         * locking should be done externally.
         * @return false if the entry was already disposed, true in all other cases.
         */
        boolean dispose() 
        {
            if (disposed) {
                return false;
            }
            if (parents != null) {
                for (SecurityCacheEntry parent : parents) {
                    parent.removeChild(this);
                }
                parents = null;
            }
            if (children != null) {
                Collection<SecurityCacheEntry> childrenToClean = children;
                children = null;
                for (SecurityCacheEntry child : childrenToClean) {
                    if (child.dispose()) {
                        DefaultSecurityCache.this.cache.remove(child.getKey());
                    }
                }
            }
            String key = getKey();
            disposed = true;
            return true;
        }

        /**
         * Add a children to this cache entry.
         * @param entry the children entry to add.
         */
        private void addChild(SecurityCacheEntry entry) 
        {
            if (this.children == null) {
                this.children = new ArrayList<SecurityCacheEntry>();
            }
            this.children.add(entry);
        }

        /**
         * Remove a children from this cache entry.
         * @param entry the children entry to remove.
         */
        private void removeChild(SecurityCacheEntry entry) 
        {
            if (this.children != null) {
                this.children.remove(entry);
                if (logger.isDebugEnabled()) {
                    logger.debug("Remove child [{}] from [{}].", entry.getKey(), getKey());
                }
            }
        }
    }

    /**
     * @param reference the reference to build the key.
     * @return a unique key for this reference.
     */
    private String getEntryKey(SecurityReference reference) {
        return keySerializer.serialize(reference);
    }

    /**
     * @param userReference the user reference to build the key.
     * @param reference the entity reference to build the key.
     * @return a unique key for the combination of this user and entity.
     */
    private String getEntryKey(UserSecurityReference userReference, SecurityReference reference) {
        return keySerializer.serialize(userReference)
            + "@@" + keySerializer.serialize(reference);
    }

    /**
     * @param entry the security entry for which a key is requested. It could be either a {@link SecurityRuleEntry}
     *              or a {@link SecurityAccessEntry}.
     * @return a unique key for this security entry.
     */
    private String getEntryKey(SecurityEntry entry)
    {
        if (entry instanceof SecurityAccessEntry) {
            return getEntryKey((SecurityAccessEntry) entry);
        } else {
            return getEntryKey((SecurityRuleEntry) entry);
        }
    }

    /**
     * @param entry the security rule entry for which the key is requested.
     * @return a unique key for this security rule entry.
     */
    private String getEntryKey(SecurityRuleEntry entry)
    {
        return getEntryKey(entry.getReference());
    }

    /**
     * @param entry the security access entry for which the key is requested.
     * @return a unique key for this security access entry.
     */
    private String getEntryKey(SecurityAccessEntry entry)
    {
        return getEntryKey(entry.getUserReference(), entry.getReference());
    }

    /**
     * @param reference the reference requested.
     * @return a security cache entry corresponding to given reference, null if none is available in the cache.
     */
    private SecurityCacheEntry getEntry(SecurityReference reference)
    {
        readLock.lock();
        try {
            return cache.get(getEntryKey(reference));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @param userReference the user reference requested.
     * @param reference the reference requested.
     * @return a security cache entry corresponding to the given user and reference, null if none is available 
     *         in the cache.
     */
    private SecurityCacheEntry getEntry(UserSecurityReference userReference, SecurityReference reference)
    {
        readLock.lock();
        try {
            return cache.get(getEntryKey(userReference, reference));
        } finally {
            readLock.unlock();
        }
    }

    /**
     * @param key the key of the cache slot to check.
     * @param entry the entry to compare to.
     * @return true, if the given entry has been inserted by another thread, false if the slot is available.
     * @throws ConflictingInsertionException if another thread use this slot with a different entry.
     */
    private boolean isAlreadyInserted(String key, SecurityEntry entry) throws ConflictingInsertionException
    {
        SecurityCacheEntry oldEntry = cache.get(key);
        if (oldEntry != null) {
            if (!oldEntry.getEntry().equals(entry)) {
                // Another thread have inserted an entry which is different from this entry!
                throw new ConflictingInsertionException();               
            }
            // Another thread have already inserted this entry.
            return true;
        }
        // The slot is available
        return false;
    }

    @Override
    public void add(SecurityRuleEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        add(entry, null);
    }

    @Override
    public void add(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        String key = getEntryKey(entry);
        
        writeLock.lock();
        try {
            if (isAlreadyInserted(key, entry)) {
                return;
            }
            SecurityCacheEntry newEntry = (groups == null || groups.isEmpty())
                ? new SecurityCacheEntry(entry)
                : new SecurityCacheEntry(entry, groups);
            cache.set(key, newEntry);
            if (logger.isDebugEnabled()) {
                logger.debug("Added rule entry [{}] into the cache.", key);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void add(SecurityAccessEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        String key = getEntryKey(entry);

        writeLock.lock();
        try {
            if (isAlreadyInserted(key, entry)) {
                return;
            }
            SecurityCacheEntry newEntry = new SecurityCacheEntry(entry);
            cache.set(key, newEntry);
            if (logger.isDebugEnabled()) {
                logger.debug("Added access entry [{}] into the cache.", key);
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public SecurityAccessEntry get(UserSecurityReference user, SecurityReference entity)
    {
        SecurityCacheEntry entry = getEntry(user, entity);
        if (entry == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Miss read access entry for [{}].", getEntryKey(user, entity));
            }
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Success read access entry for [{}].", getEntryKey(user, entity));
        }
        return (SecurityAccessEntry) entry.getEntry();
    }

    @Override
    public SecurityRuleEntry get(SecurityReference entity)
    {
        SecurityCacheEntry entry = getEntry(entity);
        if (entry == null) {
            if (logger.isDebugEnabled()) {
                logger.debug("Miss read rule entry for [{}].", getEntryKey(entity));
            }
            return null;
        }
        if (logger.isDebugEnabled()) {
            logger.debug("Success read rule entry for [{}].", getEntryKey(entity));
        }
        return (SecurityRuleEntry) entry.getEntry();
    }

    @Override
    public void remove(UserSecurityReference user, SecurityReference entity)
    {
        writeLock.lock();
        try {
            SecurityCacheEntry entry = getEntry(user, entity);
            if (entry != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Remove outdated access entry for [{}].", getEntryKey(user, entity));
                }
                if (entry.dispose()) {
                    this.cache.remove(entry.getKey());
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    @Override
    public void remove(SecurityReference entity)
    {
        writeLock.lock();
        try {
            SecurityCacheEntry entry = getEntry(entity);
            if (entry != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Remove outdated rule entry for [{}].", getEntryKey(entity));
                }
                if (entry.dispose()) {
                    this.cache.remove(entry.getKey());
                }
            }
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Listener for cache events, to properly dispose entries removed.
     */
    private class Listener implements CacheEntryListener<SecurityCacheEntry>
    {
        @Override
        public void cacheEntryAdded(
            CacheEntryEvent<SecurityCacheEntry> securityCacheEntryCacheEntryEvent)
        {
        }

        @Override
        public void cacheEntryRemoved(CacheEntryEvent<SecurityCacheEntry> event)
        {
            if (event.getEntry().getValue().dispose()) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Evicting entry [{}].", event.getEntry().getKey());
                }
            } else {
                if (logger.isDebugEnabled()) {
                    logger.debug("Removed entry [{}].", event.getEntry().getKey());
                }
            }
        }

        @Override
        public void cacheEntryModified(
            CacheEntryEvent<SecurityCacheEntry> securityCacheEntryCacheEntryEvent)
        {
        }
    }
}
