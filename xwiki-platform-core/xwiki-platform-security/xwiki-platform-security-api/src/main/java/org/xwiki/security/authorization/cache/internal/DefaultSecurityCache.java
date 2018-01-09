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
import java.util.Deque;
import java.util.HashSet;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheManager;
import org.xwiki.cache.DisposableCacheValue;
import org.xwiki.cache.config.CacheConfiguration;
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
import org.xwiki.security.authorization.cache.SecurityShadowEntry;

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

    /** The new entry being added */
    private SecurityCacheEntry newEntry;

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
            throw new InitializationException(
                String.format("Unable to create the security cache with a capacity of [%d] entries",
                    lru.getMaxEntries()), e);
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
    private class SecurityCacheEntry implements DisposableCacheValue
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
         * Create a new cache entry for a security shadow, linking it to its parent.
         * @param entry the security rule entry to cache.
         * @throws ParentEntryEvictedException if the parent required is no more available in the cache.
         */
        SecurityCacheEntry(SecurityShadowEntry entry) throws ParentEntryEvictedException
        {
            this.entry = entry;
            SecurityCacheEntry parent1 = DefaultSecurityCache.this.getEntry(entry.getReference());
            SecurityCacheEntry parent2 = DefaultSecurityCache.this.getEntry(entry.getWikiReference());
            if (parent1 == null || parent2 == null) {
                throw new ParentEntryEvictedException();
            }
            this.parents = Arrays.asList(parent1, parent2);
            parent1.addChild(this);
            parent2.addChild(this);
            logNewEntry();
        }

        /**
         * Create a new cache entry for a user access, linking it to the related entity and user.
         * @param entry the security access entry to cache.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityAccessEntry entry) throws ParentEntryEvictedException
        {
            this(entry, null);
        }

        /**
         * Create a new cache entry for a user access, linking it to the related entity and user, or shadow user.
         * @param entry the security access entry to cache.
         * @param wiki if not null, the wiki context of the shadow user.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        SecurityCacheEntry(SecurityAccessEntry entry, SecurityReference wiki) throws ParentEntryEvictedException
        {
            this.entry = entry;
            boolean isSelf = entry.getReference().equals(entry.getUserReference());
            SecurityCacheEntry parent1 = DefaultSecurityCache.this.getEntry(entry.getReference());
            SecurityCacheEntry parent2 = (isSelf) ? parent1
                : (wiki != null) ? DefaultSecurityCache.this.getShadowEntry(entry.getUserReference(), wiki)
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
            this(entry, groups, entry.getReference().getParentSecurityReference());
        }

        /**
         * Create a new cache entry for a user rule entry, linking it to its parent and to all provided groups.
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
         * @param entry the security rule entry to cache.
         * @param groups the list of groups to link this entry to.
         * @param parentReference the reference to the parent to link to.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        private SecurityCacheEntry(SecurityEntry entry, Collection<GroupSecurityReference> groups,
            SecurityReference parentReference)
            throws ParentEntryEvictedException
        {
            this.entry = entry;
            int parentSize = groups.size() + ((parentReference == null) ? 0 : 1);
            if (parentSize > 0) {
                this.parents = new ArrayList<SecurityCacheEntry>(parentSize);
                if (parentReference != null) {
                    SecurityCacheEntry parent = DefaultSecurityCache.this.getEntry(parentReference);
                    if (parent == null) {
                        throw new ParentEntryEvictedException();
                    }
                    this.parents.add(parent);
                    parent.addChild(this);
                }
                addParentGroups(groups, parentReference);
                logNewEntry();
            } else {
                this.parents = null;
                logNewEntry();
            }
        }

        /**
         * Add provided groups as parent of this entry, excluding the main parent reference.
         *
         * @param groups the list of groups to add.
         * @param parentReference the main parent reference to exclude.
         * @throws ParentEntryEvictedException if the parents required are no more available in the cache.
         */
        private void addParentGroups(Collection<GroupSecurityReference> groups,
            SecurityReference parentReference) throws ParentEntryEvictedException
        {
            for (GroupSecurityReference group : groups) {
                if (group.equals(parentReference)) {
                    continue;
                }
                SecurityCacheEntry parent = (entry instanceof SecurityShadowEntry && group.isGlobal())
                    ? DefaultSecurityCache.this.getShadowEntry(group,
                        ((SecurityShadowEntry) entry).getWikiReference())
                    : DefaultSecurityCache.this.getEntry(group);
                if (parent == null) {
                    throw new ParentEntryEvictedException();
                }
                this.parents.add(parent);
                parent.addChild(this);
            }
        }

        /**
         * Update an existing cached security rule entry with parents groups if it does not have any already.
         *
         * @param groups the groups to be added to this entry, if null or empty nothing will be done.
         * @throws ParentEntryEvictedException if one of the groups has been evicted from the cache.
         */
        boolean updateParentGroups(Collection<GroupSecurityReference> groups)
            throws ParentEntryEvictedException
        {
            if (isUser() || !(entry instanceof SecurityRuleEntry)) {
                return false;
            }

            if (groups != null && !groups.isEmpty()) {
                if (this.parents == null) {
                    this.parents = new ArrayList<SecurityCacheEntry>(groups.size());
                    addParentGroups(groups, null);
                } else {
                    SecurityCacheEntry parent = this.parents.iterator().next();
                    this.parents = new ArrayList<SecurityCacheEntry>(groups.size() + 1);
                    this.parents.add(parent);
                    addParentGroups(groups, parent.entry.getReference());
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
         */
        @Override
        public void dispose()
        {
            if (!disposed) {
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
                        // XWIKI-13746: Prevent an addition in progress to bite his own entry in a bad way.
                        if (child == newEntry) {
                            child.dispose();
                        } else {
                            try {
                                DefaultSecurityCache.this.cache.remove(child.getKey());
                            } catch (Exception e) {
                                logger.error("Security cache failure during eviction of entry [{}]", child.getKey(), e);
                            }
                        }
                    }
                }
            }
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

        /**
         * @return true if the entity is a user.
         */
        public boolean isUser()
        {
            return entry.getReference() instanceof UserSecurityReference && !(entry instanceof SecurityAccessEntry);
        }
    }

    /**
     * @param reference the reference to build the key.
     * @return a unique key for this reference.
     */
    private String getEntryKey(SecurityReference reference)
    {
        return keySerializer.serialize(reference);
    }

    /**
     * @param userReference the user reference to build the key.
     * @param reference the entity reference to build the key.
     * @return a unique key for the combination of this user and entity.
     */
    private String getEntryKey(UserSecurityReference userReference, SecurityReference reference)
    {
        return keySerializer.serialize(userReference)
            + KEY_CACHE_SEPARATOR + keySerializer.serialize(reference);
    }

    /**
     * @param userReference the user reference to build the key.
     * @param root the entity reference of the sub-wiki.
     * @return a unique key for the combination of this user and entity.
     */
    private String getShadowEntryKey(SecurityReference userReference, SecurityReference root)
    {
        return keySerializer.serialize(root) + KEY_CACHE_SEPARATOR + keySerializer.serialize(userReference);
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
        } else if (entry instanceof SecurityRuleEntry) {
            return getEntryKey((SecurityRuleEntry) entry);
        } else {
            return getEntryKey((SecurityShadowEntry) entry);
        }
    }

    /**
     * @param entry the security entry for which a key is requested. It could be either a {@link SecurityRuleEntry}
     *              or a {@link SecurityAccessEntry}.
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
     * @param userReference the user reference requested.
     * @param wiki the wiki context of the shadow reference to retrieve.
     * @return a security cache entry corresponding to the given user and reference, null if none is available
     *         in the cache.
     */
    private SecurityCacheEntry getShadowEntry(SecurityReference userReference, SecurityReference wiki)
    {
        readLock.lock();
        try {
            return cache.get(getShadowEntryKey(userReference, wiki));
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
        SecurityCacheEntry oldEntry = cache.get(key);
        if (oldEntry != null) {
            if (!oldEntry.getEntry().equals(entry)) {
                // Another thread have inserted an entry which is different from this entry!
                throw new ConflictingInsertionException();
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
     * Add a new entry in the cache and prevent cache container deadlock (in cooperation with the entry
     * dispose method) in case adding the entry cause this same entry to be evicted.
     * @param key the key of the entry to be added.
     * @param entry the entry to add.
     * @throws ConflictingInsertionException when the entry have been disposed while being added, the full load should
     *                                       be retried.
     */
    private void addEntry(String key, SecurityCacheEntry entry) throws ConflictingInsertionException
    {
        try {
            newEntry = entry;
            cache.set(key, newEntry);
            if (entry.disposed) {
                // XWIKI-13746: The added entry have been disposed while being added, meaning that the eviction
                // triggered by adding the entry has hit the entry itself, so remove it and fail.
                cache.remove(key);
                throw new ConflictingInsertionException();
            }
        } finally {
            newEntry = null;
        }
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
     * @param entry the rule or shadow entry
     * @param groups Local groups references that this user/group is a member.
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
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
     * @param entry the rule or shadow entry
     * @param groups Local groups references that this user/group is a member.
     * @return the created security cache entry
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    private SecurityCacheEntry newSecurityCacheEntry(SecurityEntry entry, Collection<GroupSecurityReference> groups)
        throws ConflictingInsertionException, ParentEntryEvictedException
    {
        if (entry instanceof SecurityRuleEntry) {
            return (groups == null)
                ? new SecurityCacheEntry((SecurityRuleEntry) entry)
                : new SecurityCacheEntry((SecurityRuleEntry) entry, groups);
        } else {
            return (groups == null)
                ? new SecurityCacheEntry((SecurityShadowEntry) entry)
                : new SecurityCacheEntry((SecurityShadowEntry) entry, groups);
        }
    }

    @Override
    public void add(SecurityAccessEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        internalAdd(entry, null);
    }

    @Override
    public void add(SecurityAccessEntry entry,  SecurityReference wiki)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        internalAdd(entry, wiki);
    }

    /**
     * Add an entry to this cache.
     * @param entry The access entry to add.
     * @param wiki The sub-wiki context of this entry. Null for a global entry.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
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
            newEntry = null;
            writeLock.unlock();
        }
    }

    /**
     * Retrieve an entry from the cache directly the internal cache. Used during unit test only.
     * @param entryKey the key to be retrieved.
     * @return the entry stored in the internal cache or Null if no entry was found.
     */
    SecurityEntry get(String entryKey) {
        SecurityCacheEntry entry = cache.get(entryKey);
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
        writeLock.lock();
        try {
            SecurityCacheEntry entry = getEntry(user, entity);
            if (entry != null) {
                if (logger.isDebugEnabled()) {
                    logger.debug("Remove outdated access entry for [{}].", getEntryKey(user, entity));
                }
                this.cache.remove(entry.getKey());
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
                this.cache.remove(entry.getKey());
            }
        } finally {
            writeLock.unlock();
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

        for (SecurityCacheEntry parent : userEntry.parents) {
            // Add the parent group (if we have not already seen it)
            SecurityReference parentRef = parent.getEntry().getReference();
            if (parentRef instanceof GroupSecurityReference) {
                groups.add((GroupSecurityReference) parentRef);
            }
        }
        return groups;
    }

    @Override
    public Collection<GroupSecurityReference> getGroupsFor(UserSecurityReference user, SecurityReference entityWiki)
    {
        Collection<GroupSecurityReference> groups = new HashSet<>();

        SecurityCacheEntry userEntry = (entityWiki != null) ? getShadowEntry(user, entityWiki) : getEntry(user);

        // If the user is not in the cache, or if it is, but not as a user, but as a regular document
        if (userEntry == null || !userEntry.isUser()) {
            // In that case, the ancestors are not fully loaded
            return null;
        }

        // We are going to get the parents of the security cache entry recursively, that is why we use a stack
        // (instead of using the execution stack which would be more costly).
        Deque<SecurityCacheEntry> entriesToExplore = new ArrayDeque<>();

        // Special case if the user is a shadow.
        if (entityWiki != null) {
            // We start with the parents of the original entry, and the parent of this shadow (excluding the original)
            addParentsWhenEntryIsShadow(userEntry, user, groups, entriesToExplore);
        } else {
            // We start with the current user
            entriesToExplore.add(userEntry);
        }

        // Let's go
        while (!entriesToExplore.isEmpty()) {
            SecurityCacheEntry entry = entriesToExplore.pop();

            // We add the parents of the current entry
            addParentsToTheListOfEntriesToExplore(entry.parents, groups, entriesToExplore);

            // If the entry has a shadow (in the concerned subwiki), we also add the parents of the shadow
            if (entityWiki != null) {
                GroupSecurityReference entryRef = (GroupSecurityReference) entry.getEntry().getReference();
                if (entryRef.isGlobal()) {
                    SecurityCacheEntry shadow = getShadowEntry(entryRef, entityWiki);
                    if (shadow != null) {
                        addParentsToTheListOfEntriesToExplore(shadow.parents, groups, entriesToExplore, entry);
                    }
                }
            }
        }

        return groups;
    }

    private void addParentsWhenEntryIsShadow(SecurityCacheEntry shadow, UserSecurityReference user,
            Collection<GroupSecurityReference> groups,
            Deque<SecurityCacheEntry> entriesToExplore)
    {
        SecurityCacheEntry originalEntry = getEntry(user);

        // We add the parents of the original (but not the original, otherwise we could have the same group twice)
        addParentsToTheListOfEntriesToExplore(originalEntry.parents, groups, entriesToExplore);
        // And we add the parent groups of the shadow
        addParentsToTheListOfEntriesToExplore(shadow.parents, groups, entriesToExplore, originalEntry);
    }

    /**
     * Add the parents of an entry to the list of entries to explore.
     *
     * @param parents the parents of the entry
     * @param groups the collection where we store the found groups
     * @param entriesToExplore the collection holding the entries we still have to explore
     */
    private void addParentsToTheListOfEntriesToExplore(Collection<SecurityCacheEntry> parents,
            Collection<GroupSecurityReference> groups,
            Deque<SecurityCacheEntry> entriesToExplore)
    {
        addParentsToTheListOfEntriesToExplore(parents, groups, entriesToExplore, null);
    }

    /**
     * Add the parents of an entry to the list of entries to explore.
     *
     * @param parents the parents of the entry
     * @param groups the collection where we store the found groups
     * @param entriesToExplore the collection holding the entries we still have to explore
     * @param originalEntry the original entry of the current entry (if the current entry is a shadow), null otherwise
     */
    private void addParentsToTheListOfEntriesToExplore(Collection<SecurityCacheEntry> parents,
            Collection<GroupSecurityReference> groups,
            Deque<SecurityCacheEntry> entriesToExplore, SecurityCacheEntry originalEntry)
    {
        if (parents == null) {
            return;
        }

        for (SecurityCacheEntry parent : parents) {
            // skip this parent if the entry is a shadow and the parent is the original entry
            // (ie: don't explore the original entry)
            if (originalEntry != null && parent == originalEntry) {
                continue;
            }

            // Add the parent group (if we have not already seen it)
            SecurityReference parentRef = parent.getEntry().getReference();
            if (parentRef instanceof GroupSecurityReference && groups.add((GroupSecurityReference) parentRef)) {
                entriesToExplore.add(parent);
            }
        }
    }


}
