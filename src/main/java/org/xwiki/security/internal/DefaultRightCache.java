/*
 * Copyright 2010 Andreas Jonsson
 * 
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
 *
 */
package org.xwiki.security.internal;

import org.xwiki.cache.Cache;
import org.xwiki.cache.CacheFactory;
import org.xwiki.cache.CacheException;
import org.xwiki.cache.event.CacheEntryListener;
import org.xwiki.cache.event.CacheEntryEvent;
import org.xwiki.cache.config.CacheConfiguration;
import org.xwiki.cache.eviction.EntryEvictionConfiguration;
import org.xwiki.cache.eviction.LRUEvictionConfiguration;

import org.xwiki.model.reference.EntityReference;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.EntityReferenceSerializer;

import org.xwiki.configuration.ConfigurationSource;
import org.xwiki.component.annotation.Requirement;
import org.xwiki.component.annotation.Component;
import org.xwiki.component.phase.Initializable;

import org.xwiki.security.RightCache;
import org.xwiki.security.RightCacheEntry;
import org.xwiki.security.RightCacheKey;
import org.xwiki.security.ParentEntryEvictedException;
import org.xwiki.security.ConflictingInsertionException;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.util.Map;
import java.util.TreeMap;
import java.util.Set;
import java.util.TreeSet;
import java.util.List;
import java.util.LinkedList;

/**
 * A cache for fast access right checking.
 * @version $Id: $
 */
@Component
public class DefaultRightCache implements RightCache, Initializable
{
    /**
     * The logging tool.
     */
    private static final Log LOG = LogFactory.getLog(DefaultRightCache.class);

    /** Cache factory. */
    @Requirement("oscache") private CacheFactory cacheFactory;

    /** The keys in the cache are generated from instances of {@link EntityReference}. */
    @Requirement("rightcachekey") private EntityReferenceSerializer<String> keySerializer;

    /** The cache instance. */
    private Cache<RightCacheEntry> cache;

    /** Obtain configuration from the xwiki.properties file. */
    @Requirement("xwikiproperties")
    private ConfigurationSource configuration;

    /**
     * The cache entries are arranged into a hierarchy.  This data
     * structure stores the parent-child relationships between
     * entries.
     */ 
    private final Map<String, Node> parentRelations = new TreeMap();

    @Override
    public void initialize()
    {
        CacheConfiguration cacheConfig = new CacheConfiguration();
        cacheConfig.setConfigurationId("xwiki.security.rightcache");
        LRUEvictionConfiguration lru = new LRUEvictionConfiguration();
        Integer defaultCapacity = 500;
        String capacityString = configuration.getProperty("xwiki.security.rightcache.capacity",
                                                          defaultCapacity.toString());
        int capacity;
        try {
            capacity = Integer.parseInt(capacityString);
        } catch (NumberFormatException e) {
            capacity = defaultCapacity;
        }
        lru.setMaxEntries(capacity);
        cacheConfig.put(EntryEvictionConfiguration.CONFIGURATIONID, lru);
        try {
            this.cache = cacheFactory.newCache(cacheConfig);
            this.cache.addCacheEntryListener(new Listener());
        } catch (CacheException e) {
            LOG.error("Failed to create rights cache.");
            throw new RuntimeException(e);
        }
        LOG.info("Created a cache of type "
                 + cache.getClass().getName()
                 + " with a capacity of "
                 + capacity
                 + " entries.");
    }

    @Override
    public RightCacheKey getRightCacheKey(EntityReference entity)
    {
        final EntityReference clone = entity.clone();
        clone.setChild(null);
        String mainWiki = XWikiUtils.getMainWiki();
        EntityReference root = clone.getRoot();
        assert (root.getType() == EntityType.WIKI);
        if (!root.getName().equals(mainWiki)) {
            EntityReference main = new EntityReference(mainWiki, EntityType.WIKI, null);
            root.setParent(main);
        }

        return new RightCacheKey() {
            private final EntityReference entity = clone;

            @Override
            public EntityReference getEntityReference()
            {
                return entity;
            }
        };
    }

    /**
     * Generate a key for representing an entry containing a
     * particular user's access level to an entity.
     * @param user Entity representing a user name.
     * @param entity The entity that is the object for this key.
     * @return Key for the cache.
     */
    private String generateKey(EntityReference user, EntityReference entity)
    {
        // We prefix this category of entries with a unique prefix symbol.
        return "1" + keySerializer.serialize(user) 
             + ":" + keySerializer.serialize(entity);
    }

    /**
     * Generate a key for representing an entry containging
     * information on whether this entity is associated with any
     * rights object.
     * @param entity The entity that is the object for this key.
     * @return Key for the cache.
     */
    private String generateKey(EntityReference entity)
    {
        // We prefix this category of entries with a unique prefix symbol.
        return "2" + keySerializer.serialize(entity);
    }

    /**
     * Generate a key for representing an entry containing a
     * particular user's access level to an entity.
     * @param user Entity representing a user name.
     * @param entity The entity that is the object for this key.
     * @return Key for the cache.
     */
    private String generateKey(RightCacheKey user, RightCacheKey entity)
    {
        return generateKey(user.getEntityReference(), entity.getEntityReference());
    }

    /**
     * Generate a key for representing an entry containging
     * information on whether this entity is associated with any
     * rights object.
     * @param entity The entity that is the object for this key.
     * @return Key for the cache.
     */
    private String generateKey(RightCacheKey entity)
    {
        return generateKey(entity.getEntityReference());
    }


    /**
     * Add an entry to this cache.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     * @param entry The entry to insert.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link RightsLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    public void addUserAtEntity(RightCacheKey user, RightCacheKey entity, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        List<RightCacheKey> parents = new LinkedList();
        parents.add(user);
        parents.add(entity);
        LOG.debug("Adding user at entity: " + user.getEntityReference() + ", " + entity.getEntityReference());
        addEntry(generateKey(user, entity), parents, entry);
    }

    /**
     * Add an entry to this cache.
     * @param entity The entity which is the object of this cache entry.
     * @param parent The parent entity.
     * @param entry The entry to insert.
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link RightsLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    public void addWithExplicitParent(RightCacheKey entity, RightCacheKey parent, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        String parentKey = parent != null ? generateKey(parent) : null;
        addEntry(generateKey(entity), parentKey, entry);
    }

    /**
     * Add an entry to this cache with several parent relations.
     * @param entity The entity which is the object of this cache entry.
     * @param parents The parent entities.
     * @param entry The entry to insert.
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link RightsLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    public void addWithMultipleParents(RightCacheKey entity, Iterable<RightCacheKey> parents, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        addEntry(generateKey(entity), parents, entry);
    }

    /**
     * Add an entry to this cache.
     * @param entity The entity which is the object of this cache entry.
     * @param entry The entry to insert.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link RightsLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    public void add(RightCacheKey entity, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        EntityReference parent = entity.getEntityReference().getParent();
        if (parent != null) {
            parent = getRightCacheKey(parent).getEntityReference();
        }
        String parentKey = parent != null ? generateKey(parent) : null;
        addEntry(generateKey(entity), parentKey, entry);
    }

    /**
     * Add an entry to this cache.
     * @param key The key under which this entry will be stored.
     * @param parentObject An object representing the parent entry/entries.
     * @param entry The entry to insert.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link RightsLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    private synchronized void addEntry(String key, Object parentObject, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException
    {
        RightCacheEntry old = cache.get(key);
        if (old != null) {
            if (old.equals(entry)) {
                // Another thread have already inserted this entry.
                return;
            } else {
                // Another thread have inserted an entry which is
                // different from this entry!
                throw new ConflictingInsertionException();
            }
        }
        addParentRelation(parentObject, key);
        cache.set(key, entry);
    }

    @Override
    public RightCacheEntry get(RightCacheKey entity)
    {
        return cache.get(generateKey(entity));
    }

    @Override
    public RightCacheEntry get(RightCacheKey user, RightCacheKey entity)
    {
        LOG.debug("Getting " + user.getEntityReference() + " at " + entity.getEntityReference());
        return cache.get(generateKey(user, entity));
    }

    @Override
    public void remove(RightCacheKey user, RightCacheKey entity)
    {
        remove(generateKey(user, entity));
    }

    @Override
    public void remove(RightCacheKey entity)
    {
        remove(generateKey(entity));
    }

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param key The key of the entry.
     */
    private synchronized void remove(String key)
    {
        removeChildren(key);
        removeParentRelation(key);
        removeEntryNoChildren(key);
    }

    /**
     * Remove an entry ignoring the children.  (The children are
     * expected to already have been removed.)
     * @param key Key of the entry to remove.
     */
    private void removeEntryNoChildren(String key)
    {
        removeParentRelation(key);
        cache.remove(key);
    }

    /**
     * Add one or several parents to the given key.
     * @param parentObject Either a parent key, or a list of parent keys.
     * @param key The key.
     * @exception ParentEntryEvictedException if the parent entry is
     * not in the cache.
     */
    private void addParentRelation(Object parentObject, String key)
        throws ParentEntryEvictedException
    {
        if (parentObject == null || parentObject instanceof String) {
            addParentRelation((String) parentObject, key);
        } else if (parentObject instanceof Iterable) {
            addParentRelation((Iterable<RightCacheKey>) parentObject, key);
        }
    }

    /**
     * Setup a parent-child relationship.
     * @param parentKey The key for the parent entry.
     * @param key The key for the child entry.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link RightsLoader} must restart its load attempt.
     */
    private void addParentRelation(String parentKey, String key)
        throws ParentEntryEvictedException
    {
        Parent parent;
        if (parentKey == null) {
            parent = null;
        } else {
            parent = parentRelations.get(parentKey);
            if (parent == null) {
                LOG.debug("Parent entry was evicted.  Throwing exception.");
                throw new ParentEntryEvictedException();
            }
            parent.addChild(key);
        } 
        Node n = new Node(parent);
        parentRelations.put(key, n);
    }

    /**
     * Add multiple parent relations to the given key.
     * @param parentReferences List of parents.
     * @param key Key of the entry.
     * @exception ParentEntryEvictedException if the parent entry was
     * not in the cache.
     */
    private void addParentRelation(Iterable<RightCacheKey> parentReferences, String key)
        throws ParentEntryEvictedException
    {
        List<Node> parents = new LinkedList();
        for (RightCacheKey ref : parentReferences) {
            Node parent = parentRelations.get(generateKey(ref));
            if (parent == null) {
                LOG.debug("One of the parent entries was evicted.  Throwing exception.");
                throw new ParentEntryEvictedException();
            }
            parents.add(parent);
        }
        Parent p = new MultiParent(parents);
        p.addChild(key);
        Node n = new Node(p);
        parentRelations.put(key, n);
    }

    /**
     * Remove a parent-child relationship.
     * @param key The key for the child entry.
     */
    private void removeParentRelation(String key)
    {
        Node n = parentRelations.get(key);
        if (n != null) {
            Parent parent = n.getParent();
            if (parent != null) {
                parent.removeChild(key);
            }
        }
    }

    /**
     * Remove all children of an entry.
     * @param key The key for entry.
     */
    private void removeChildren(String key)
    {
        Node node = parentRelations.get(key);
        if (node != null) {
            Iterable<String> children = node.getChildren();
            node.setChildren(null);
            if (children != null) {
                for (String childKey : children) {
                    removeEntryNoChildren(childKey);
                }
            }
        }
    }

    /**
     * Parenthood interface.
     */
    private interface Parent
    {
        /**
         * Add a child entry to this parent.
         * @param child The key of the child entry.
         */
        void addChild(String child);

        /**
         * Remove a child entry from this parent.
         * @param child The key of the child entry.
         */
        void removeChild(String child);
    }

    /**
     * A class for keeping track of multiple parent relations.
     */
    private static class MultiParent implements Parent
    {
        /** List of parents. */
        private final List<Node> parentNodes;

        /**
         * @param parentNodes List of parents.
         */
        public MultiParent(List<Node> parentNodes)
        {
            this.parentNodes = parentNodes;
        }

        @Override
        public void addChild(String child)
        {
            for (Parent p : parentNodes) {
                p.addChild(child);
            }
        }

        @Override
        public void removeChild(String child)
        {
            for (Parent p : parentNodes) {
                p.removeChild(child);
            }
        }
    }

    /**
     * Represent nodes in the parent-child relationship datastructure.
     */
    private static class Node implements Parent
    {
        /** The set of children keys of this entry. */
        private Set<String> children;
        /** The parent entry. */
        private final Parent parent;

        /**
         * @param parent The parent entry, may be null.
         */
        Node(Parent parent)
        {
            this.children = children;
            this.parent = parent;
        }

        /** @return The set of children.  May be null. */
        public Set<String> getChildren()
        {
            return this.children;
        }

        /** @param children The set of children.  May be null. */
        public void setChildren(Set<String> children)
        {
            this.children = children;
        }

        @Override
        public void addChild(String childKey) 
        {
            if (children == null) {
                children = new TreeSet();
            }
            children.add(childKey);
        }

        @Override
        public void removeChild(String childKey)
        {
            if (children != null) {
                children.remove(childKey);
                if (children.size() == 0) {
                    children = null;
                }
            }
        }

        /**
         * @return The parent relation of this node.
         */
        Parent getParent()
        {
            return parent;
        }
    }

    /**
     * Listener for cache events.
     */
    private class Listener implements CacheEntryListener<RightCacheEntry>
    {
        /** {@inheritDoc} */
        public void cacheEntryAdded(CacheEntryEvent<RightCacheEntry> event)
        {
            String key = event.getEntry().getKey();
            LOG.debug("Cache entry added: " + key);
        }

        /** {@inheritDoc} */
        public void cacheEntryRemoved(CacheEntryEvent<RightCacheEntry> event)
        {
            String key = event.getEntry().getKey();
            removeChildren(key);
            removeParentRelation(key);
            LOG.debug("Cache entry removed: " + key);
        }

        /** {@inheritDoc} */
        public void cacheEntryModified(CacheEntryEvent<RightCacheEntry> event)
        {
            LOG.debug("Got cache entry modified event for key " + event.getEntry().getKey());
        }
    }
}
