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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.collections4.map.AbstractReferenceMap;
import org.apache.commons.collections4.map.ReferenceMap;
import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;
import org.xwiki.component.phase.InitializationException;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.reference.EntityReferenceSerializer;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityShadowEntry;
import org.xwiki.security.internal.GroupSecurityEntry;

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
    private static final int DEFAULT_CAPACITY = 10000;

    /** Separator used for composing key for the cache. */
    private static final String KEY_CACHE_SEPARATOR = "@@";

    /** Escaped separator used for composing key for the cache. */
    private static final String KEY_CACHE_SEPARATOR_ESCAPED = KEY_CACHE_SEPARATOR + KEY_CACHE_SEPARATOR;

    /** Logger. **/
    @Inject
    private Logger logger;

    /** Fair read-write lock used for fair scheduling of cache access. */
    private final ReadWriteLock readWriteLock = new ReentrantReadWriteLock(true);

    /** Fair read lock. */
    private final Lock readLock = readWriteLock.readLock();

    /** Fair write lock. */
    private final Lock writeLock = readWriteLock.writeLock();

    private final ReadWriteLock invalidationReadWriteLock = new ReentrantReadWriteLock(true);

    private final Lock invalidationReadLock = invalidationReadWriteLock.readLock();

    private final Lock invalidationWriteLock = invalidationReadWriteLock.writeLock();

    /** The keys in the cache are generated from instances of {@link org.xwiki.model.reference.EntityReference}. */
    @Inject
    private EntityReferenceSerializer<String> keySerializer;

    /** Cache manager to create the cache. */
    @Inject
    private CacheManager cacheManager;

    /** The cache instance. */
    private Cache<SecurityCacheEntry> cache;

    /**
     * A map of all entries that are strongly referenced somewhere. This includes all entries in the above cache, but
     * also all entries that are referenced as parents somewhere and that are thus required for the hierarchy to be
     * complete and to support correct hierarchical cache invalidation. When an entry is removed from the cache, the
     * GC will also remove it from the internal entries unless it is still referenced as a parent somewhere else. For
     * this to work, the only strong references to SecurityCacheEntry are stored during entry creation, in the cache
     * and in the list of parents.
     */
    private final Map<String, SecurityCacheEntry> internalEntries =
        new ReferenceMap<>(AbstractReferenceMap.ReferenceStrength.HARD, AbstractReferenceMap.ReferenceStrength.WEAK);

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
        cacheConfig.put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
        try {
            return cacheManager.createNewCache(cacheConfig);
        } catch (Exception e) {
            throw new InitializationException(String
                .format("Unable to create the security cache with a capacity of [%d] entries", lru.getMaxEntries()), e);
        }
    }

    @Override
    public void initialize() throws InitializationException
    {
        cache = newCache();
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
         * 
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
                    throw new ParentEntryEvictedException(String.format(
                        "The parent with reference [%s] for entry [%s] is no longer available in the cache",
                        parentReference, entry));
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
         * Create a new cache entry for a security shadow, linking it to its parent.
         * 
         * @param entry the security rule entry to cache.
         * @throws ParentEntryEvictedException if the parent required is no more available in the cache.
         */
        SecurityCacheEntry(SecurityShadowEntry entry) throws ParentEntryEvictedException
        {
            this.entry = entry;
            SecurityCacheEntry parent1 = DefaultSecurityCache.this.getEntry(entry.getReference());
            if (parent1 == null) {
                throw new ParentEntryEvictedException(String.format(
                    "The parent entry with reference [%s] for entry [%s] is no longer available in the cache", parent1,
                    entry));
            }
            SecurityCacheEntry parent2 = DefaultSecurityCache.this.getEntry(entry.getWikiReference());
            if (parent2 == null) {
                throw new ParentEntryEvictedException(String.format(
                    "The wiki entry with reference [%s] for entry [%s] is no longer available in the cache", parent2,
                    entry));
            }
            this.parents = Arrays.asList(parent1, parent2);
            parent1.addChild(this);
            parent2.addChild(this);
            logNewEntry();
        }

        /**
         * Create a new cache entry for a user access, linking it to the related entity and user, or shadow user.
         * 
         * @param entry the security access entry to cache.
         * @param wiki if not null, the wiki context of the shadow user.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityAccessEntry entry, SecurityReference wiki) throws ParentEntryEvictedException
        {
            this.entry = entry;
            boolean isSelf = entry.getReference().equals(entry.getUserReference());
            SecurityCacheEntry parent1 = DefaultSecurityCache.this.getEntry(entry.getReference());
            if (parent1 == null) {
                throw new ParentEntryEvictedException(String.format(
                    "The first parent with reference [%s] for the entry [%s] with wiki [%s] is no longer "
                        + "available in the cache.",
                    entry.getReference(), entry, wiki));
            }
            SecurityCacheEntry parent2 = (isSelf) ? parent1
                : (wiki != null) ? DefaultSecurityCache.this.getShadowEntry(entry.getUserReference(), wiki)
                    : DefaultSecurityCache.this.getEntry(entry.getUserReference());
            if (parent2 == null) {
                throw new ParentEntryEvictedException(String.format(
                    "The second parent with reference [%s] for the entry [%s] with wiki [%s] is no longer available "
                        + "in the cache.",
                    entry.getUserReference(), entry, wiki));
            }
            // If the user is the guest user, don't throw an exception as the guest user isn't stored as user.
            if (!parent2.isUser() && entry.getUserReference().getOriginalReference() != null) {
                throw new ParentEntryEvictedException(String.format(
                    "The second parent [%s] for the entry [%s] with wiki [%s] is not a user entry.",
                    parent2, entry, wiki));
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
         * 
         * @param entry the security rule entry to cache.
         * @param groups the list of groups to link this entry to.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
            throws ParentEntryEvictedException
        {
            this(entry, groups, entry.getReference().getParentSecurityReference());
        }

        /**
         * Create a new cache entry for a user rule entry, linking it to its parent and to all provided groups.
         * 
         * @param entry the security rule entry to cache.
         * @param groups the list of groups to link this entry to.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityShadowEntry entry, Collection<GroupSecurityReference> groups)
            throws ParentEntryEvictedException
        {
            this(entry, groups, entry.getReference());
        }

        /**
         * Create a new cache entry for a user rule entry, linking it to its parent and to all provided groups.
         * 
         * @param entry the security rule entry to cache.
         * @param groups the list of groups to link this entry to.
         * @param parentReference the reference to the parent to link to.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        private SecurityCacheEntry(SecurityEntry entry, Collection<GroupSecurityReference> groups,
            SecurityReference parentReference) throws ParentEntryEvictedException
        {
            this.entry = entry;
            int parentSize = groups.size() + ((parentReference == null) ? 0 : 1);
            if (parentSize > 0) {
                if (parentReference != null) {
                    this.parents = new ArrayList<>(parentSize);
                    SecurityCacheEntry parent = DefaultSecurityCache.this.getEntry(parentReference);
                    if (parent == null) {
                        throw new ParentEntryEvictedException(String.format(
                            "The parent with reference [%s] required by entry [%s] with groups [%s]"
                                + " is no longer available in the cache.",
                            parentReference, entry, groups));
                    }
                    this.parents.add(parent);
                    this.parents.addAll(getParentGroups(groups, parentReference));
                    // Wait until here to avoid that in case of an exception there is a reference to the new object
                    // in the parent's children.
                    parent.addChild(this);
                } else {
                    this.parents = getParentGroups(groups, null);
                }
            } else {
                this.parents = null;
            }
            logNewEntry();
        }

        /**
         * Get group entries for the provided parents, excluding the main parent reference.
         * <p>
         * If loading all of them succeeds, this entry is added as child of all of them.
         *
         * @param groups the list of groups to add.
         * @param parentReference the main parent reference to exclude.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        private Collection<SecurityCacheEntry> getParentGroups(Collection<GroupSecurityReference> groups,
            SecurityReference parentReference)
            throws ParentEntryEvictedException
        {
            Collection<SecurityCacheEntry> result = new ArrayList<>(groups.size());
            for (GroupSecurityReference group : groups) {
                SecurityCacheEntry parent = (entry instanceof SecurityShadowEntry && group.isGlobal())
                    ? DefaultSecurityCache.this.getShadowEntry(group, ((SecurityShadowEntry) entry).getWikiReference())
                    : DefaultSecurityCache.this.getEntry(group);
                if (parent == null) {
                    throw new ParentEntryEvictedException(String
                        .format("The parent with reference [%s] is no longer available in the cache", parentReference));
                }

                if (!parent.isUser()) {
                    throw new ParentEntryEvictedException(
                        String.format("The parent [%s] is not a group entry.", parent.getEntry()));
                }

                // Make sure the group really is stored as such (can happen if that the right of the group was checked
                // directly)
                if (!(parent.getEntry().getReference() instanceof GroupSecurityReference)
                    && parent.getEntry() instanceof GroupSecurityEntry) {
                    ((GroupSecurityEntry) parent.getEntry()).setGroupReference(group);
                }

                // Do not add the main parent reference but still execute all checks to be sure that it is a group.
                if (!group.equals(parentReference)) {
                    result.add(parent);
                }
            }

            // Wait until here to be sure that no children are added if there is an exception while collecting the
            // parents.
            result.forEach(parent -> parent.addChild(this));

            return result;
        }

        /**
         * Update an existing cached security rule entry with parents groups if it does not have any already.
         *
         * @param groups the groups to be added to this entry, if null or empty nothing will be done.
         * @throws ParentEntryEvictedException if one of the groups has been evicted from the cache.
         */
        boolean updateParentGroups(Collection<GroupSecurityReference> groups) throws ParentEntryEvictedException
        {
            if (isUser() || !(entry instanceof SecurityRuleEntry)) {
                return false;
            }

            if (groups != null && !groups.isEmpty()) {
                if (this.parents == null) {
                    this.parents = getParentGroups(groups, null);
                } else {
                    SecurityCacheEntry parent = this.parents.iterator().next();
                    // Ensure that parents aren't modified if an exception is thrown.
                    Collection<SecurityCacheEntry> newParents = new ArrayList<>(groups.size() + 1);
                    newParents.add(parent);
                    newParents.addAll(getParentGroups(groups, parent.entry.getReference()));
                    this.parents = newParents;
                }
            }

            return true;
        }

        /**
         * Log the new entry creation.
         */
        private void logNewEntry()
        {
            if (logger.isDebugEnabled()) {
                if (CollectionUtils.isEmpty(parents)) {
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
         * Dispose this entry from the cache, removing all children relation in its parents, and removing all its
         * children recursively. This method is not thread safe in regards to the cache, proper locking should be done
         * externally.
         */
        public void dispose()
        {
            if (!disposed) {
                DefaultSecurityCache.this.cache.remove(getKey());
                DefaultSecurityCache.this.internalEntries.remove(getKey());
                disposed = true;

                disconnectFromParents();
                disposeChildren();
            }
        }

        protected void disconnectFromParents()
        {
            if (parents != null) {
                for (SecurityCacheEntry parent : parents) {
                    if (!parent.disposed) {
                        parent.removeChild(this);
                    }
                }
            }
        }

        private void disposeChildren()
        {
            if (children != null) {
                for (SecurityCacheEntry child : children) {
                    if (!child.disposed) {
                        if (logger.isDebugEnabled()) {
                            logger.debug("Cascaded removal of entry [{}] from cache.", child.getKey());
                        }
                        child.dispose();
                    }
                }
                // Avoid the extra work of the garbage collector by clearing the set.
                children.clear();
            }
        }

        /**
         * Add a children to this cache entry.
         * 
         * @param entry the children entry to add.
         */
        private void addChild(SecurityCacheEntry entry)
        {
            if (this.children == null) {
                // Use a weak set to avoid that upper entries in the hierarchy prevent their children from being
                // garbage collected and removed from internalEntries.
                this.children = Collections.newSetFromMap(new WeakHashMap<>());
            }
            this.children.add(entry);
        }

        /**
         * Remove a children from this cache entry.
         * 
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

        /**
         * @return true if the entity is a user.
         */
        public boolean isUser()
        {
            return entry.getReference() instanceof UserSecurityReference && !(entry instanceof SecurityAccessEntry);
        }

        @Override
        public String toString()
        {
            return this.entry.toString();
        }
    }

    private String escapeEntryKey(String value)
    {
        return value.replace(KEY_CACHE_SEPARATOR, KEY_CACHE_SEPARATOR_ESCAPED);
    }

    private String getEntryKey(boolean shadow, EntityReference userReference, EntityReference reference)
    {
        StringBuilder builder = new StringBuilder();

        builder.append(shadow ? 's' : 'n');
        if (userReference != null) {
            builder.append(escapeEntryKey(this.keySerializer.serialize(userReference)));
        }
        builder.append(KEY_CACHE_SEPARATOR);
        builder.append(reference.getType());
        builder.append(':');
        builder.append(escapeEntryKey(this.keySerializer.serialize(reference)));

        return builder.toString();
    }

    /**
     * @param reference the reference to build the key.
     * @return a unique key for this reference.
     */
    private String getEntryKey(SecurityReference reference)
    {
        return getEntryKey(null, reference);
    }

    /**
     * @param userReference the user reference to build the key.
     * @param reference the entity reference to build the key.
     * @return a unique key for the combination of this user and entity.
     */
    private String getEntryKey(UserSecurityReference userReference, SecurityReference reference)
    {
        return getEntryKey(false, userReference, reference);
    }

    /**
     * @param userReference the user reference to build the key.
     * @param root the entity reference of the sub-wiki.
     * @return a unique key for the combination of this user and entity.
     */
    private String getShadowEntryKey(SecurityReference userReference, SecurityReference root)
    {
        return getEntryKey(true, userReference, root);
    }

    /**
     * @param entry the security entry for which a key is requested. It could be either a {@link SecurityRuleEntry} or a
     *            {@link SecurityAccessEntry}.
     * @return a unique key for this security entry.
     */
    private String getEntryKey(SecurityEntry entry)
    {
        if (entry instanceof SecurityAccessEntry) {
            return getEntryKey((SecurityAccessEntry) entry);
        } else if (entry instanceof SecurityRuleEntry) {
            return getEntryKey((SecurityRuleEntry) entry);
        } else {
            return getEntryKey((SecurityShadowEntry) entry);
        }
    }

    /**
     * @param entry the security entry for which a key is requested. It could be either a {@link SecurityRuleEntry} or a
     *            {@link SecurityAccessEntry}.
     * @return a unique key for this security entry.
     */
    private String getEntryKey(SecurityShadowEntry entry)
    {
        return getShadowEntryKey(entry.getReference(), entry.getWikiReference());
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
        return getInternal(getEntryKey(reference));
    }

    /**
     * @param userReference the user reference requested.
     * @param reference the reference requested.
     * @return a security cache entry corresponding to the given user and reference, null if none is available in the
     *         cache.
     */
    private SecurityCacheEntry getEntry(UserSecurityReference userReference, SecurityReference reference)
    {
        return getInternal(getEntryKey(userReference, reference));
    }

    /**
     * @param userReference the user reference requested.
     * @param wiki the wiki context of the shadow reference to retrieve.
     * @return a security cache entry corresponding to the given user and reference, null if none is available in the
     *         cache.
     */
    private SecurityCacheEntry getShadowEntry(SecurityReference userReference, SecurityReference wiki)
    {
        return getInternal(getShadowEntryKey(userReference, wiki));
    }

    /**
     * Get a security cache entry from the cache or the internal map. In the latter case, the entry is re-inserted
     * into the cache. This method can be called without locking, it uses the read lock internally.
     *
     * @param key the key of the entry to retrieve
     * @throws IllegalStateException if the entry has been disposed (this should never happen)
     * @return the entry corresponding to the given key, null if none is available in the cache
     */
    private SecurityCacheEntry getInternal(String key)
    {
        readLock.lock();
        try {
            SecurityCacheEntry result = cache.get(key);
            if (result == null) {
                // Try to get the entry from the internal map which may have, e.g., parents that are no longer in the
                // cache but still referenced by entries in the cache.
                // Synchronize to avoid concurrent modification of the map as get() may trigger the eviction of
                // garbage collected entries. All other operations are done under the write lock.
                synchronized (this.internalEntries) {
                    result = this.internalEntries.get(key);
                }

                if (result != null) {
                    // Try re-inserting the entry into the cache to give it another chance of being stored directly.
                    this.cache.set(key, result);
                }
            }

            if (result != null && result.disposed) {
                throw new IllegalCacheStateException(
                    String.format("Entry [%s] has been disposed without being removed from the cache.", result));
            }

            return result;
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
        try {
            return isAlreadyInserted(key, entry, null);
        } catch (ParentEntryEvictedException e) {
            // Impossible to reach
            return true;
        }
    }

    private boolean isAlreadyInserted(String key, SecurityEntry entry, Collection<GroupSecurityReference> groups)
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        SecurityCacheEntry oldEntry = getInternal(key);
        if (oldEntry != null) {
            if (!oldEntry.getEntry().equals(entry)) {
                // Another thread has inserted an entry which is different from this entry!
                throw new ConflictingInsertionException(
                    String.format(
                        "Another thread has inserted an entry [%s] which is different from entry [%s]"
                            + " with key [%s] and groups [%s]",
                        oldEntry, entry, key, groups));
            }
            // If the user/group has been completed
            if (oldEntry.updateParentGroups(groups)) {
                // Upgrade it to a user/group entry
                oldEntry.entry = entry;
            }

            return true;
        }
        // The slot is available
        return false;
    }

    /**
     * Add a new entry in the cache and prevent cache container deadlock (in cooperation with the entry dispose method)
     * in case adding the entry cause this same entry to be evicted.
     *
     * @param key the key of the entry to be added.
     * @param entry the entry to add.
     */
    private void addEntry(String key, SecurityCacheEntry entry)
    {
        cache.set(key, entry);
        // Don't store access entries in the internal entries as they can never be the parent of another entry, and
        // thus it is not important to keep them outside the cache. While this could be seen as an additional "cache"
        // layer for access entries, this is not the purpose of the internal entries map. Instead, the size of the
        // cache should be increased if this is desired.
        if (!(entry.getEntry() instanceof SecurityAccessEntry)) {
            this.internalEntries.put(key, entry);
        }

        if (entry.disposed) {
            // This should never happen as entries cannot be disposed while being added to the cache as both
            // operations require the write lock. However, if it happens, there is a serious bug in the code so
            // better fail with an exception.
            throw new IllegalCacheStateException(
                String.format("Entry [%s] has been disposed while being added to the cache.", entry));
        }
    }

    @Override
    public void add(SecurityRuleEntry entry) throws ParentEntryEvictedException, ConflictingInsertionException
    {
        add(entry, null);
    }

    @Override
    public void add(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        add((SecurityEntry) entry, groups);
    }

    @Override
    public void add(SecurityShadowEntry entry, Collection<GroupSecurityReference> groups)
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        add((SecurityEntry) entry, groups);
    }

    /**
     * Add either a rule or shadow user/group entry into the cache.
     * 
     * @param entry the rule or shadow entry
     * @param groups Local groups references that this user/group is a member.
     * @exception ParentEntryEvictedException when the parent entry of this entry was evicted before this insertion.
     *                Since all entries, except wiki-entries, must have a parent cached, the
     *                {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have inserted this entry, but with a different content.
     */
    private void add(SecurityEntry entry, Collection<GroupSecurityReference> groups)
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        String key = getEntryKey(entry);

        writeLock.lock();
        try {
            if (isAlreadyInserted(key, entry, groups)) {
                return;
            }
            addEntry(key, newSecurityCacheEntry(entry, groups));

            logger.debug("Added rule/shadow entry [{}] into the cache.", key);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Construct a security cache entry for the given arguments.
     * 
     * @param entry the rule or shadow entry
     * @param groups Local groups references that this user/group is a member.
     * @return the created security cache entry
     * @exception ParentEntryEvictedException when the parent entry of this entry was evicted before this insertion.
     *                Since all entries, except wiki-entries, must have a parent cached, the
     *                {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     */
    private SecurityCacheEntry newSecurityCacheEntry(SecurityEntry entry, Collection<GroupSecurityReference> groups)
        throws ParentEntryEvictedException
    {
        if (entry instanceof SecurityRuleEntry) {
            return (groups == null) ? new SecurityCacheEntry((SecurityRuleEntry) entry)
                : new SecurityCacheEntry((SecurityRuleEntry) entry, groups);
        } else {
            return (groups == null) ? new SecurityCacheEntry((SecurityShadowEntry) entry)
                : new SecurityCacheEntry((SecurityShadowEntry) entry, groups);
        }
    }

    @Override
    public void add(SecurityAccessEntry entry) throws ParentEntryEvictedException, ConflictingInsertionException
    {
        internalAdd(entry, null);
    }

    @Override
    public void add(SecurityAccessEntry entry, SecurityReference wiki)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        internalAdd(entry, wiki);
    }

    /**
     * Add an entry to this cache.
     * 
     * @param entry The access entry to add.
     * @param wiki The sub-wiki context of this entry. Null for a global entry.
     * @throws ParentEntryEvictedException when the parent entry of this entry was evicted before this insertion. Since
     *             all entries, except wiki-entries, must have a parent cached, the
     *             {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have inserted this entry, but with a different content.
     */
    private void internalAdd(SecurityAccessEntry entry, SecurityReference wiki)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        String key = getEntryKey(entry);

        writeLock.lock();
        try {
            if (isAlreadyInserted(key, entry)) {
                return;
            }
            addEntry(key, new SecurityCacheEntry(entry, wiki));

            logger.debug("Added access entry [{}] into the cache.", key);
        } finally {
            writeLock.unlock();
        }
    }

    /**
     * Retrieve an entry from the cache directly the internal cache. Used during unit test only.
     * 
     * @param entryKey the key to be retrieved.
     * @return the entry stored in the internal cache or Null if no entry was found.
     */
    SecurityEntry get(String entryKey)
    {
        SecurityCacheEntry entry = getInternal(entryKey);
        return (entry != null) ? entry.getEntry() : null;
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
        this.invalidationWriteLock.lock();

        try {
            writeLock.lock();
            try {
                SecurityCacheEntry entry = getEntry(user, entity);
                if (entry != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Remove outdated access entry for [{}].", getEntryKey(user, entity));
                    }
                    entry.dispose();
                }
            } finally {
                writeLock.unlock();
            }
        } finally {
            this.invalidationWriteLock.unlock();
        }
    }

    @Override
    public void remove(SecurityReference entity)
    {
        this.invalidationWriteLock.lock();

        try {
            writeLock.lock();
            try {
                SecurityCacheEntry entry = getEntry(entity);
                if (entry != null) {
                    if (logger.isDebugEnabled()) {
                        logger.debug("Remove outdated rule entry for [{}].", getEntryKey(entity));
                    }
                    entry.dispose();
                }
            } finally {
                writeLock.unlock();
            }
        } finally {
            this.invalidationWriteLock.unlock();
        }
    }

    @Override
    public Collection<GroupSecurityReference> getImmediateGroupsFor(UserSecurityReference user)
    {
        Collection<GroupSecurityReference> groups = new HashSet<>();

        SecurityCacheEntry userEntry = getEntry(user);
        // If the user is not in the cache, or if it is, but not as a user, but as a regular document
        if (userEntry == null || !userEntry.isUser()) {
            // In that case, the ancestors are not fully loaded
            return null;
        }

        if (userEntry.parents != null) {
            for (SecurityCacheEntry parent : userEntry.parents) {
                // Add the parent group (if we have not already seen it)
                SecurityReference parentRef = parent.getEntry().getReference();
                if (parentRef instanceof GroupSecurityReference) {
                    groups.add((GroupSecurityReference) parentRef);
                }
            }
        }
        return groups;
    }

    @Override
    public Collection<GroupSecurityReference> getGroupsFor(UserSecurityReference user, SecurityReference entityWiki)
    {
        Collection<GroupSecurityReference> groups = new HashSet<>();

        // Load the user entry. Then traverse its parents. Parents of user/group entries are never modified, even
        // when the parent is removed from the cache. This makes this code safe despite not locking the cache.
        SecurityCacheEntry userEntry = (entityWiki != null) ? getShadowEntry(user, entityWiki) : getEntry(user);

        // If the user is not in the cache, or if it is, but not as a user, but as a regular document
        if (userEntry == null || !userEntry.isUser()) {
            // In that case, the ancestors are not fully loaded
            return null;
        }

        // We are going to get the parents of the security cache entry recursively, that is why we use a stack
        // (instead of using the execution stack which would be more costly).
        Deque<SecurityCacheEntry> entriesToExplore = new ArrayDeque<>();

        // We start with the current user
        entriesToExplore.add(userEntry);

        // Let's go
        while (!entriesToExplore.isEmpty()) {
            SecurityCacheEntry entry = entriesToExplore.pop();

            if (entry.parents != null) {
                // We add the parents of the current entry
                for (SecurityCacheEntry parent : entry.parents) {
                    // When exploring the parents of a global user in a given wiki, only explore parents that are
                    // either from the desired wiki or that are shadow entries. This avoids leaving that wiki and
                    // ensures that for a group, all parents in the wanted wiki are explored (otherwise, e.g., the main
                    // wiki's entry of a group could be explored that lacks the subwiki parents).
                    if (isEntryGroupInWiki(parent, entityWiki)
                        && (groups.add((GroupSecurityReference) parent.getEntry().getReference())))
                    {
                        entriesToExplore.add(parent);
                    }
                }
            }
        }

        return groups;
    }

    private static boolean isEntryGroupInWiki(SecurityCacheEntry entry, SecurityReference entityWiki)
    {
        SecurityReference reference = entry.getEntry().getReference();
        return reference instanceof GroupSecurityReference
            && (entityWiki == null
            || (entry.getEntry() instanceof SecurityShadowEntry
            || entityWiki.equals(reference.getWikiReference())));
    }

    @Override
    public void suspendInvalidation()
    {
        this.invalidationReadLock.lock();
    }

    @Override
    public void resumeInvalidation()
    {
        this.invalidationReadLock.unlock();
    }
}
