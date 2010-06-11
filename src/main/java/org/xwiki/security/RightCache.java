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
 *
 */
package org.xwiki.security;

import org.xwiki.component.annotation.ComponentRole;

import org.xwiki.model.reference.EntityReference;

/**
 * A cache for fast access right checking.
 * @version $Id: $
 */
@ComponentRole
public interface RightCache
{
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
    void add(RightCacheKey entity, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;

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
    void addUserAtEntity(RightCacheKey user, RightCacheKey entity, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Add an entry to this cache.  To explicitly name the parent is
     * needed for nested spaces and for virtual wiki's.
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
    void addWithExplicitParent(RightCacheKey entity, RightCacheKey parent, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;

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
    void addWithMultipleParents(RightCacheKey entity, Iterable<RightCacheKey> parents, RightCacheEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;


    /**
     * Get a cached entry.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     * @return The cache entry, or {@code null}.
     */
    RightCacheEntry get(RightCacheKey user, RightCacheKey entity);

    /**
     * Get a cached entry.
     * @param entity The entity which is the object of this cache entry.
     * @return The cache entry, or {@code null}.
     */
    RightCacheEntry get(RightCacheKey entity);

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     */
    void remove(RightCacheKey user, RightCacheKey entity);

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param entity The entity which is the object of this cache entry.
     */
    void remove(RightCacheKey entity);

    /**
     * @param entity An entity that is about to be inserted into the cache.
     * @return a {@link RightCacheKey} for the entity.
     */
    RightCacheKey getRightCacheKey(EntityReference entity);
}
