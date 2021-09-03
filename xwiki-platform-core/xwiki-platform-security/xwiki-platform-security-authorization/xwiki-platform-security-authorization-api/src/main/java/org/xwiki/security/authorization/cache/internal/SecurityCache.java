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

import java.util.Collection;

import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityShadowEntry;

/**
 * A cache for fast access right rules checking.
 *
 * @version $Id$
 * @since 4.0M2 
 */
public interface SecurityCache extends org.xwiki.security.authorization.cache.SecurityCache
{
    /**
     * Add an entry to this cache.
     * @param entry The rule entry to add.
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
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
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    void add(SecurityAccessEntry entry)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Add an entry for access to a local wiki entity by a global user.
     * @param entry The access entry to add.
     * @param wiki The sub-wiki context of this entry
     * @throws ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     *
     * @since 5.0M2
     */
    void add(SecurityAccessEntry entry, SecurityReference wiki)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Add a user/group entry to this cache.
     *
     * @param entry The user/group entry to insert.
     * @param groups Local groups references that this user/group is a member.
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     */
    void add(SecurityRuleEntry entry, Collection<GroupSecurityReference> groups)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Add a shadow user/group entry to this cache.
     *
     * @param entry The user entry to insert.
     * @param groups Local group references that this user/group is a member.
     * @exception ParentEntryEvictedException when the parent entry of
     * this entry was evicted before this insertion.  Since all
     * entries, except wiki-entries, must have a parent cached, the
     * {@link org.xwiki.security.authorization.cache.SecurityCacheLoader} must restart its load attempt.
     * @throws ConflictingInsertionException when another thread have
     * inserted this entry, but with a different content.
     *
     * @since 5.0M2
     */
    void add(SecurityShadowEntry entry, Collection<GroupSecurityReference> groups)
        throws ParentEntryEvictedException, ConflictingInsertionException;

    /**
     * Get immediate groups where the user/group is a member (directly in its wiki).
     *
     * @param user reference to a user/group
     * @return the list of immediate groups where the user is a member or null if the information is not in the cache
     *
     * @since 7.1.5
     * @since 7.4.5
     * @since 8.2.2
     * @since 8.3RC1
     */
    Collection<GroupSecurityReference> getImmediateGroupsFor(UserSecurityReference user);

    /**
     * Get all groups where the user/group is a member (directly, or indirectly including relation due to global groups
     * members of local ones).
     * 
     * If the user is global and entityWiki is not null:
     *   - returns the local groups (from entityWiki) and the global groups where the user is, directly or indirectly
     * If entityWiki is null:
     *   - returns the groups from the wiki of the user where the user is, directly or indirectly
     * If the cache does not contain the needed information::
     *   - returns null
     *  
     * @param user reference to a user/group
     * @param entityWiki the wiki where to look for the groups (null if the wiki is the same than the user's wiki) 
     * @return the list of all groups where the user is a member or null if the information is not in the cache yet
     *
     * @since 6.4.3
     * @since 7.0RC1
     */
    Collection<GroupSecurityReference> getGroupsFor(UserSecurityReference user, SecurityReference entityWiki);

    /**
     * Suspend delivery of invalidation events.
     * 
     * @since 10.4RC1
     */
    void suspendInvalidation();

    /**
     * Resume delivery of invalidation events.
     * 
     * @since 10.4RC1
     */
    void resumeInvalidation();
}
