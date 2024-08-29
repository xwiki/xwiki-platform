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
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationException;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
import org.xwiki.security.authorization.internal.AbstractSecurityRuleEntry;
import org.xwiki.security.internal.GroupSecurityEntry;
import org.xwiki.security.internal.UserBridge;

/**
 * Default implementation of the security cache loader. It depends on a
 * {@link org.xwiki.security.authorization.SecurityEntryReader} for reading rules missing from the cache, and on a
 * {@link org.xwiki.security.authorization.AuthorizationSettler} for resolving access from rules.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultSecurityCacheLoader implements SecurityCacheLoader
{
    /** Logger. **/
    @Inject
    private Logger logger;

    /** The right cache. */
    @Inject
    private org.xwiki.security.authorization.cache.SecurityCache securityCache;

    /** Event listener responsible for invalidating cache entries. */
    @Inject
    private SecurityCacheRulesInvalidator rulesInvalidator;

    /** Factory object for producing SecurityRule instances from the corresponding xwiki rights objects. */
    @Inject
    private SecurityEntryReader securityEntryReader;

    /** User bridge to retrieve the list of group a user is related to. */
    @Inject
    private UserBridge userBridge;

    /** Provide the configured authorization settler. */
    @Inject
    private Provider<AuthorizationSettler> authorizationSettlerProvider;

    /**
     * Implementation of the SecurityRuleEntry.
     */
    private final class EmptySecurityRuleEntry extends AbstractSecurityRuleEntry implements GroupSecurityEntry
    {
        /** Reference of the related entity. */
        private SecurityReference reference;

        /**
         * @param reference reference of the related entity
         */
        private EmptySecurityRuleEntry(SecurityReference reference)
        {
            this.reference = reference;
        }

        /**
         * @return the reference of the related entity
         */
        @Override
        public SecurityReference getReference()
        {
            return reference;
        }

        @Override
        public void setGroupReference(GroupSecurityReference reference)
        {
            this.reference = reference;
        }

        /**
         * @return an empty list of rules
         */
        @Override
        public Collection<SecurityRule> getRules()
        {
            return Collections.emptyList();
        }

        @Override
        public boolean isEmpty()
        {
            return true;
        }
    }

    private SecurityCache getSecurityCache()
    {
        return (SecurityCache) this.securityCache;
    }

    @Override
    public SecurityAccessEntry load(UserSecurityReference user, SecurityReference entity) throws AuthorizationException
    {
        // This lock is important to prevent that any cache invalidation is applied while we are creating new
        // cache entries. Otherwise, this code might create new cache entries based on outdated information that
        // won't be invalidated. Imagine, for example, that thread 1 is starting to load user groups for user A, then
        // thread 2 removes a group of A and invalidates the cache of A and then thread 1 saves a new cache entry for
        // user A with the outdated information. This lock prevents this by delaying the invalidation of the cache of A
        // (that uses a write-lock that corresponds to the read lock we use here) after the store of the cache entry A
        // such that the new cache entry is correctly invalidated.
        this.rulesInvalidator.suspend();

        try {
            return loadRequiredEntries(user, entity);
        } finally {
            this.rulesInvalidator.resume();
        }
    }

    /**
     * Load entity entries, group entries, and user entries required to settle the access, settle it, add this decision
     * into the cache and return the access.
     * 
     * @param user The user to check access for.
     * @param entity The entity to check access to.
     * @return The resulting access for the user on the entity.
     * @throws org.xwiki.security.authorization.AuthorizationException On error.
     */
    private SecurityAccessEntry loadRequiredEntries(UserSecurityReference user, SecurityReference entity)
        throws AuthorizationException
    {
        // No entity, return default rights for user in its wiki
        if (entity == null) {
            return authorizationSettlerProvider.get()
                .settle(user, loadGroupsOfUserOrGroup(user, user.getWikiReference(), null, new ArrayDeque<>()),
                    null);
        }

        // Retrieve rules for the entity from the cache
        Deque<SecurityRuleEntry> ruleEntries = getRules(entity);

        // Evaluate, store and return the access right
        return loadAccessEntries(user, entity, ruleEntries);
    }

    /**
     * Load group entries, and user entries required, to settle the access, settle it, add this decision into the cache
     * and return the access.
     * 
     * @param user The user to check access for.
     * @param entity The lowest entity providing security rules on the path of the entity to check access for.
     * @param ruleEntries The rule entries associated with the above entity.
     * @return The access for the user at the entity (equivalent to the one of the entity to check access for).
     * @throws org.xwiki.security.authorization.AuthorizationException On error.
     */
    private SecurityAccessEntry loadAccessEntries(UserSecurityReference user, SecurityReference entity,
        Deque<SecurityRuleEntry> ruleEntries)
        throws AuthorizationException
    {
        // userWiki is the wiki of the user
        SecurityReference userWiki = user.getWikiReference();
        // entityWiki is the wiki of the entity when the user is global and the entity is local
        SecurityReference entityWiki = user.isGlobal() ? entity.getWikiReference() : null;
        if (entityWiki != null && userWiki.equals(entityWiki)) {
            entityWiki = null;
        }

        // Load user and related groups into the cache (global and shadowed locals) as needed
        Collection<GroupSecurityReference> groups;

        // Public access could not appear in any group, no need to load it carefully, just optimized here
        if (user.getOriginalReference() == null) {
            groups = loadPublicUser(user, entityWiki);
        } else {
            groups = loadGroupsOfUserOrGroup(user, userWiki, entityWiki, new ArrayDeque<>());
        }

        // Settle the access
        SecurityAccessEntry accessEntry = authorizationSettlerProvider.get().settle(user, groups, ruleEntries);

        // Store the result into the cache
        try {
            getSecurityCache().add(accessEntry, entityWiki);
        } catch (ParentEntryEvictedException | ConflictingInsertionException e) {
            logFailedInsertion(e);
        }

        // Return the result
        return accessEntry;
    }

    /**
     * Load user/group entry in the cache as needed, load related group entries and return the list of all groups
     * associated with the given user/group in both the user wiki and the given entity wiki. Groups containing
     * (recursively) groups containing the user/group are also listed.
     *
     * @param user The user/group to load.
     * @param userWiki The user wiki. Should correspond to the wiki of the user/group provided above.
     * @param entityWiki Only for global user, the wiki of the entity currently evaluated if it differ from the user
     *     wiki, null otherwise. Local group information of the entity wiki will be evaluated for the user/group to load
     *     and a shadow user will be made available in that wiki to support access entries.
     * @param branchGroups groups that were already seen by callers, to avoid infinite recursion
     * @return A collection of groups associated to the requested user/group (both user wiki and entity wiki)
     *     cache.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private Collection<GroupSecurityReference> loadGroupsOfUserOrGroup(UserSecurityReference user,
        SecurityReference userWiki, SecurityReference entityWiki, Deque<GroupSecurityReference> branchGroups)
        throws AuthorizationException
    {
        // First, we try to get the groups of the user from the cache
        Collection<GroupSecurityReference> groups = getSecurityCache().getGroupsFor(user, entityWiki);
        if (groups != null) {
            // Since we have then in the cache, it means that the entry is already loaded
            return groups;
        }

        // Otherwise we have to load the entry
        Set<GroupSecurityReference> allImmediateGroups = new HashSet<>();

        // Load all groups of the user. First, look for groups in the entity wiki in the case of a global user and an
        // entity in the subwiki. Then, load the groups in the wiki of the user. Finally, make sure that all of them
        // are available in the cache by recursively loading them (and collecting the list of all groups recursively).
        // If the user/group is global and we are looking for rules inside a subwiki, we need to ensure that the
        // global user/group is loaded first, and we should also looks at global groups that she is a member of,
        // before looking at the group she is a member of in the local wiki.
        if (entityWiki != null) {
            // Load local groups of the user in the entity wiki. They aren't in the cache, otherwise we would have
            // gotten the whole hierarchy from it.
            allImmediateGroups.addAll(this.userBridge.getAllGroupsFor(user, entityWiki.getOriginalWikiReference()));
        }

        // Load the global groups (or local groups for a local user) containing that user/group
        Collection<GroupSecurityReference> globalGroups = null;

        if (entityWiki != null) {
            // Check availability of the information from the user/group entry in the cache. If entityWiki == null,
            // this call would fail for sure as otherwise the first call to get all groups from the cache should have
            // succeeded already.
            globalGroups = getSecurityCache().getImmediateGroupsFor(user);
        }

        if (globalGroups == null) {
            // No luck, load them from the database.
            globalGroups = this.userBridge.getAllGroupsFor(user, userWiki.getOriginalWikiReference());
        }

        allImmediateGroups.addAll(globalGroups);

        Set<GroupSecurityReference> groupsToIgnore = new HashSet<>();

        groups = loadImmediateGroupsRecursively(allImmediateGroups, entityWiki, branchGroups, groupsToIgnore);

        globalGroups.removeAll(groupsToIgnore);
        allImmediateGroups.removeAll(groupsToIgnore);

        // Load the user entry for its own wiki.
        loadUserEntry(user, globalGroups);

        if (entityWiki != null) {
            // Store a shadow entry for a global user/group involved in a local wiki with both local and global
            // groups as parents to ensure that the entry is properly invalidated also when just the shadow parent is
            // invalidated due to a local parent being invalidated.
            try {
                getSecurityCache().add(new DefaultSecurityShadowEntry(user, entityWiki), allImmediateGroups);
            } catch (ParentEntryEvictedException | ConflictingInsertionException e) {
                logFailedInsertion(e);
            }
        }

        // Returns all collected groups for access evaluation
        return groups;
    }

    /**
     * Recurse on the given set of groups that are the immediate parents of a user or group while checking for
     * possible infinite recursion due to cycles in the group-membership graph.
     * List all groups with the given group in both the user wiki and the given entity wiki. Groups containing
     * (recursively) groups containing the group are also listed.
     *
     * @param allImmediateGroups the list of immediate groups to load
     * @param entityWiki Only for global user, the wiki of the entity currently evaluated if it differ from the user
     *     wiki, null otherwise. Local group information of the entity wiki will be evaluated for the user/group to load
     *     and a shadow user will be made available in that wiki to support access entries.
     * @param branchGroups groups that were already seen by callers, to avoid infinite recursion
     * @param groupsToIgnore the groups in the immediate groups that need to be ignored to prevent cycles
     * @return A collection of groups associated to the requested groups (both user wiki and entity wiki)
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private Collection<GroupSecurityReference> loadImmediateGroupsRecursively(
        Set<GroupSecurityReference> allImmediateGroups, SecurityReference entityWiki,
        Deque<GroupSecurityReference> branchGroups, Set<GroupSecurityReference> groupsToIgnore)
        throws AuthorizationException
    {
        Collection<GroupSecurityReference> groups = new HashSet<>();

        for (GroupSecurityReference group : allImmediateGroups) {
            if (branchGroups.contains(group)) {
                // Prevent infinite recursion. The group graph is supposed to be acyclic, this is not supported so no
                // need for a defined behavior.
                groupsToIgnore.add(group);
                continue;
            }

            branchGroups.push(group);
            // When the group isn't global, it is in the wiki of the entity and thus we no longer need to deal
            // with the difference between entity and user wiki, so pass null.
            SecurityReference groupEntityWiki = group.isGlobal() ? entityWiki : null;
            Collection<GroupSecurityReference> nestedGroups =
                loadGroupsOfUserOrGroup(group, group.getWikiReference(), groupEntityWiki, branchGroups);
            // Check if any of the groups that were already considered by a parent call re-appeared in the hierarchy.
            // Again, this is just to prevent infinite recursion, not to get some defined behavior.
            if (branchGroups.stream().anyMatch(nestedGroups::contains)) {
                groupsToIgnore.add(group);
            } else {
                groups.add(group);
                groups.addAll(nestedGroups);
            }
            branchGroups.pop();
        }

        return groups;
    }

    private Collection<GroupSecurityReference> loadPublicUser(UserSecurityReference user, SecurityReference entityWiki)
    {
        // Don't load the actual user as the security entry reader doesn't load rules for the guest user, and we would
        // thus risk inserting a main wiki entry without rules which would be a serious issue. However, the main wiki
        // entry should already have been loaded while loading the hierarchy of the entity.

        if (entityWiki != null) {
            // Ensure there is a Public shadow in the subwiki of the checked entity
            try {
                getSecurityCache().add(new DefaultSecurityShadowEntry(user, entityWiki), null);
            } catch (ParentEntryEvictedException | ConflictingInsertionException e) {
                logFailedInsertion(e);
            }
        }
        return Collections.emptySet();
    }

    /**
     * Load rules for a user/group into the cache with relations to immediate groups. Groups should be already loaded,
     * else a ParentEntryEvictedException will be thrown. The parent chain of the loaded user will be loaded as needed.
     *
     * @param user The user/group to load.
     * @param groups The collection of groups associated with the user/group
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private void loadUserEntry(UserSecurityReference user, Collection<GroupSecurityReference> groups)
        throws AuthorizationException
    {
        // Make sure the parent of the user document is loaded.
        Deque<SecurityReference> chain = user.getReversedSecurityReferenceChain();
        chain.removeLast();
        for (SecurityReference ref : chain) {
            SecurityRuleEntry entry = getSecurityCache().get(ref);
            if (entry == null) {
                entry = securityEntryReader.read(ref);
                addToCache(entry);
            }
        }

        SecurityRuleEntry entry = securityEntryReader.read(user);
        try {
            getSecurityCache().add(entry, groups);
        } catch (ParentEntryEvictedException | ConflictingInsertionException e) {
            logFailedInsertion(e);
        }
    }

    /**
     * Retrieve rules for all hierarchy levels of the provided reference. Rules may be read from the cache, or from the
     * entities and fill the cache.
     *
     * @param entity The entity for which rules should be loaded and retrieve.
     * @return A collection of security rule entry, once for each level of the hierarchy.
     * @exception org.xwiki.security.authorization.AuthorizationException if an error occurs
     */
    private Deque<SecurityRuleEntry> getRules(SecurityReference entity)
        throws AuthorizationException
    {
        Deque<SecurityRuleEntry> rules = new LinkedList<SecurityRuleEntry>();
        List<SecurityRuleEntry> emptyRuleEntryTail = new ArrayList<SecurityRuleEntry>();
        for (SecurityReference ref : entity.getReversedSecurityReferenceChain()) {
            SecurityRuleEntry entry = getSecurityCache().get(ref);
            if (entry == null) {
                if (Right.getEnabledRights(ref.getType()).isEmpty()) {
                    // Do not call the reader on entity that will give useless rules
                    entry = new EmptySecurityRuleEntry(ref);
                    emptyRuleEntryTail.add(entry);
                } else {
                    entry = securityEntryReader.read(ref);
                    // Add intermediate empty rules sets to the cache to hold this significant one
                    for (SecurityRuleEntry emptyRuleEntry : emptyRuleEntryTail) {
                        addToCache(emptyRuleEntry);
                    }
                    emptyRuleEntryTail.clear();
                    addToCache(entry);
                }
            }
            rules.push(entry);
        }
        return rules;
    }

    private void addToCache(SecurityRuleEntry entry)
    {
        try {
            getSecurityCache().add(entry);
        } catch (ParentEntryEvictedException | ConflictingInsertionException e) {
            logFailedInsertion(e);
        }
    }

    /**
     * Log a failed insertion into the security cache to allow debugging problems with the security cache.
     *
     * @param e the exception from the security cache
     */
    private void logFailedInsertion(Exception e)
    {
        this.logger.debug("Insertion into the security cache failed. This may indicate that the security "
            + "cache is too small or a bug.", e);
    }
}
