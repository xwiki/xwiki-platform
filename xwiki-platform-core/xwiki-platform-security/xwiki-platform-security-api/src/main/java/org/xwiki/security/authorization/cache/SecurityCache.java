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
package org.xwiki.security.authorization.cache;

import java.util.Collection;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;

/**
 * A cache for fast access right rules checking.
 *
 * @version $Id$
 * @since 4.0M2 
 */
@Role
public interface SecurityCache
{
    /**
     * Add an entry to this cache.
     * @param entry The rule entry to add.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    void add(SecurityRuleEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Add an entry to this cache.
     * @param entry The access entry to add.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    void add(SecurityAccessEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Add a user entry to this cache.
     *
     * @param entry The user entry to insert.
     * @param groups Groups references that this user is a member.
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    void add(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
        throws ParentEntryEvictedException, ConflictingInsertionException;


    /**
     * Get a cached entry.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     * @return The cache entry, or {@code null}.
     */
    SecurityAccessEntry get(UserSecurityReference user, SecurityReference entity);

    /**
     * Get a cached entry.
     * @param entity The entity which is the object of this cache entry.
     * @return The cache entry, or {@code null}.
     */
    SecurityRuleEntry get(SecurityReference entity);

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param user Entity representing the user.
     * @param entity The entity which is the object of this cache entry.
     */
    void remove(UserSecurityReference user, SecurityReference entity);

    /**
     * Remove an entry from this cache.  All child entries of this
     * entry will also be removed.
     * @param entity The entity which is the object of this cache entry.
     */
    void remove(SecurityReference entity);
}
