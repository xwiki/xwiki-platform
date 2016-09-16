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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.EntityType;
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
import org.xwiki.security.internal.UserBridge;

/**
 * Default implementation of the security cache loader.
 *
 * It depends on a {@link org.xwiki.security.authorization.SecurityEntryReader}
 * for reading rules missing from the cache, and on a
 * {@link org.xwiki.security.authorization.AuthorizationSettler} for resolving
 * access from rules.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Singleton
public class DefaultSecurityCacheLoader implements SecurityCacheLoader
{
    /** Maximum number of attempts at loading an entry. */
    private static final int MAX_RETRIES = 5;

    /** Logger. **/
    @Inject
    private Logger logger;

    /** The right cache. */
    @Inject
    private SecurityCache securityCache;

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
    private final class EmptySecurityRuleEntry extends AbstractSecurityRuleEntry
    {
        /** Reference of the related entity. */
        private final SecurityReference reference;

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

    @Override
    public SecurityAccessEntry load(UserSecurityReference user, SecurityReference entity)
        throws AuthorizationException
    {
        int retries = 0;

        while (true) {
            rulesInvalidator.suspend();

            try {
                retries++;
                return loadRequiredEntries(user, entity);
            } catch (ParentEntryEvictedException e) {
                if (retries < MAX_RETRIES) {
                    this.logger.debug("The parent entry was evicted. Have tried {} times.  Trying again...", retries);
                    continue;
                }
            } catch (ConflictingInsertionException e) {
                if (retries < MAX_RETRIES) {
                    this.logger.debug("There were conflicting insertions. Have tried {} times.  Retrying...", retries);
                    continue;
                }
            } finally {
                rulesInvalidator.resume();
            }
            String message = String.format("Failed to load the cache in %d attempts.  Giving up.", retries);
            this.logger.error(message);
            throw new AuthorizationException(user.getOriginalDocumentReference(),
                                             entity.getOriginalReference(), message);
        }
    }

    /**
     * Load entity entries, group entries, and user entries required to settle the access, settle it,
     * add this decision into the cache and return the access.
     * 
     * @param user The user to check access for.
     * @param entity The entity to check access to.
     * @return The resulting access for the user on the entity.
     * @throws ParentEntryEvictedException If one of the parent entries are evicted before the load is completed.
     * @throws ConflictingInsertionException When different threads have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException On error.
     */
    private SecurityAccessEntry loadRequiredEntries(UserSecurityReference user, SecurityReference entity)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // No entity, return default rights for user in its wiki
        if (entity == null) {
            return authorizationSettlerProvider.get().settle(user,
                loadUserEntry(user, user.getWikiReference(), null), null);
        }

        // Retrieve rules for the entity from the cache
        Deque<SecurityRuleEntry> ruleEntries = getRules(entity);

        // Evaluate, store and return the access right
        return loadAccessEntries(user, entity, ruleEntries);
    }

    /**
     * Load group entries, and user entries required, to settle the access, settle it,
     * add this decision into the cache and return the access.
     * 
     * @param user The user to check access for.
     * @param entity The lowest entity providing security rules on the path of the entity to check access for.
     * @param ruleEntries The rule entries associated with the above entity.
     * @return The access for the user at the entity (equivalent to the one of the entity to check access for).
     * @throws ParentEntryEvictedException If one of the parent entries are evicted before the load is completed.
     * @throws ConflictingInsertionException When different threads have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException On error.
     */
    private SecurityAccessEntry loadAccessEntries(UserSecurityReference user, SecurityReference entity,
        Deque<SecurityRuleEntry> ruleEntries)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // userWiki is the wiki of the user
        SecurityReference userWiki = user.getWikiReference();
        // entityWiki is the wiki of the entity when the user is global and the entity is local
        SecurityReference entityWiki = user.isGlobal() ? entity.getWikiReference() : null;
        if (entityWiki != null && userWiki.equals(entityWiki)) {
            entityWiki = null;
        }

        // Load user and related groups into the cache (global and shadowed locals) as needed
        Collection<GroupSecurityReference> groups = loadUserEntry(user, userWiki, entityWiki);

        // Settle the access
        SecurityAccessEntry accessEntry = authorizationSettlerProvider.get().settle(user, groups, ruleEntries);

        // Store the result into the cache
        securityCache.add(accessEntry, entityWiki);

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
     * wiki, null otherwise. Local group information of the entity wiki will be evaluated for the user/group to load
     * and a shadow user will be made available in that wiki to support access entries.
     * @return A collection of groups associated to the requested user/group (both user wiki and entity wiki)
     * @throws ParentEntryEvictedException if any of the parent entries of the group were evicted.
     * @throws ConflictingInsertionException When different threads have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private Collection<GroupSecurityReference> loadUserEntry(UserSecurityReference user, SecurityReference userWiki,
        SecurityReference entityWiki)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // First, we try to get the groups of the user from the cache
        Collection<GroupSecurityReference> groups = securityCache.getGroupsFor(user, entityWiki); 
        if (groups != null) {
            // Since we have then in the cache, it means that the entry is already loaded
            return groups;
        }
        
        // Otherwise we have to load the entry
        groups = new HashSet<GroupSecurityReference>();

        // Public access could not appear in any group, no need to load it carefully, just optimized here
        if (user.getOriginalReference() == null) {
            if (securityCache.get(user) == null) {
                // Main wiki entry should be loaded
                getRules(user);
            }
            if (entityWiki != null) {
                // Ensure there is a Public shadow in the subwiki of the checked entity
                securityCache.add(new DefaultSecurityShadowEntry(user, entityWiki), null);
            }
            return groups;
        }

        // If the user/group is global and we are looking for rules inside a subwiki, we need to ensure that the
        // global user/group is loaded first, and we should also looks at global groups that it is a member of
        if (entityWiki != null) {
            // First we add the global groups containing that user/group
            // Check availability of the information from the user/group entry in the cache
            Collection<GroupSecurityReference> globalGroups = securityCache.getGroupsFor(user, null);
            if (globalGroups == null) {
                // No luck, the global user does not seems to be in the cache, so we need to load it
                globalGroups = new HashSet<>();
                loadUserEntry(user, userWiki, null, globalGroups, new ArrayDeque<GroupSecurityReference>());
            }
            groups.addAll(globalGroups);

            // Now we also need to consider the local groups that contains the global groups found, since the user/group
            // should be considered indirectly a member of these groups as well
            for (GroupSecurityReference group : globalGroups) {
                // Check availability of the information from the shadow entry of the global group in the entity wiki
                Collection<GroupSecurityReference> localGroups = securityCache.getGroupsFor(group, entityWiki);
                if (localGroups == null) {
                    // No luck, the shadow of the global group in the entity wiki does not seems to be in the cache,
                    // so we need to load it
                    localGroups = new HashSet<>();
                    loadUserEntry(group, userWiki, entityWiki, localGroups, new ArrayDeque<GroupSecurityReference>());
                }
                groups.addAll(localGroups);
            }
        }

        // We load the rules concerning the groups of the user inside the wiki of the entity, could be either
        // the global group of a global user, local group for a global user, or local group for a local user
        Collection<GroupSecurityReference> localGroups = new HashSet<>();
        loadUserEntry(user, userWiki, entityWiki, localGroups, new ArrayDeque<GroupSecurityReference>());
        groups.addAll(localGroups);

        // Returns all collected groups for access evaluation
        return groups;
    }

    /**
     * Load a user/group entry in the cache as need, load related group entries, in the context of a single wiki.
     *
     * @param user The user/group to be loaded.
     * @param userWiki The user wiki. Should correspond to the wiki of the user provided above.
     * @param entityWiki Only for global user, the wiki to be evaluated, null otherwise to evaluate the user wiki.
     * @param allGroups For the initial call, this collection should normally be empty, and will receive all the
     *                  group associated with the given user (either directly or indirectly).
     * @param branchGroups During recursive calls, it contains the result so far, and allow limiting the recursion.
     * @throws ParentEntryEvictedException if any of the parent entries of the group were evicted.
     * @throws ConflictingInsertionException When different threads have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private void loadUserEntry(UserSecurityReference user, SecurityReference userWiki,
        SecurityReference entityWiki, Collection<GroupSecurityReference> allGroups,
        Deque<GroupSecurityReference> branchGroups)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // Retrieve the list of immediate group for the user/group in either the entity wiki or the user/group wiki
        Collection<GroupSecurityReference> groups = (entityWiki != null)
            ? userBridge.getAllGroupsFor(user, entityWiki.getOriginalWikiReference())
            : userBridge.getAllGroupsFor(user, userWiki.getOriginalWikiReference());

        Collection<GroupSecurityReference> immediateGroup = new ArrayList<GroupSecurityReference>();

        // Loads all immediate groups recursively, collecting indirect groups along the way
        for (GroupSecurityReference group : groups) {
            // Loads the group only if it has never been seen before in the current path to avoid infinite recursion
            if (!branchGroups.contains(group)) {
                // We check the cache for real nodes (not shadows) since group are coming from their own wiki
                Collection<GroupSecurityReference> groupsOfGroup = securityCache.getGroupsFor(group, null);
                // And we load the groups only if they are not in the cache
                if (groupsOfGroup == null) {
                    // Add this group into the list of immediate groups for this entry
                    immediateGroup.add(group);

                    // Load dependencies recursively
                    branchGroups.push(group);
                    loadUserEntry(group, (entityWiki != null) ? entityWiki : userWiki, null, allGroups, branchGroups);
                    branchGroups.pop();
                } else {
                    // Check for possible recursion in the cached groups and add this group only if it is safe
                    boolean recursionFound = false;
                    for (GroupSecurityReference existingGroup : groupsOfGroup) {
                        if (branchGroups.contains(existingGroup)) {
                            recursionFound = true;
                            break;
                        }
                    }
                    if (!recursionFound) {
                        // Add this group into the list of immediate groups for this entry
                        immediateGroup.add(group);
                        // Add all group found in the cache for the final result
                        allGroups.addAll(groupsOfGroup);
                    }
                }
            }
        }

        // Collect groups of this entry for the final result
        allGroups.addAll(immediateGroup);

        // Store the user/group in the cache
        if (entityWiki != null) {
            // Store a shadow entry for a global user/group involved in a local wiki
            securityCache.add(new DefaultSecurityShadowEntry(user, entityWiki), immediateGroup);
        } else {
            // Store or upgrade document entry into a user/group entry in the cache
            // While the document rules could be already loaded in the cache, even if these rules should stay the same,
            // we need to reload them as a user/group to have the security rules reference properly typed and
            // recognized by the cache as a user. (cfr. XWIKI-12016)
            loadUserEntry(user, immediateGroup);
        }
    }

    /**
     * Load rules for a user/group into the cache with relations to immediate groups. Groups should be already loaded,
     * else a ParentEntryEvictedException will be thrown. The parent chain of the loaded user will be loaded as needed.
     *
     * @param user The user/group to load.
     * @param groups The collection of groups associated with the user/group
     * @throws ParentEntryEvictedException if any of the parent entries of the group were evicted.
     * @throws ConflictingInsertionException When different threads have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private void loadUserEntry(UserSecurityReference user, Collection<GroupSecurityReference> groups)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // Make sure the parent of the user document is loaded.
        Deque<SecurityReference> chain = user.getReversedSecurityReferenceChain();
        chain.removeLast();
        for (SecurityReference ref : chain) {
            SecurityRuleEntry entry = securityCache.get(ref);
            if (entry == null) {
                entry = securityEntryReader.read(ref);
                securityCache.add(entry);
            }
        }

        SecurityRuleEntry entry = securityEntryReader.read(user);
        securityCache.add(entry, groups);
    }

    /**
     * Retrieve rules for all hierarchy levels of the provided reference.
     * Rules may be read from the cache, or from the entities and fill the cache.
     *
     * @param entity The entity for which rules should be loaded and retrieve.
     * @return A collection of security rule entry, once for each level of the hierarchy.
     * @exception org.xwiki.security.authorization.AuthorizationException if an error occurs
     * @exception ParentEntryEvictedException if any parent entry is
     * evicted before the operation completes.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     */
    private Deque<SecurityRuleEntry> getRules(SecurityReference entity)
        throws AuthorizationException, ParentEntryEvictedException, ConflictingInsertionException
    {
        Deque<SecurityRuleEntry> rules = new LinkedList<SecurityRuleEntry>();
        List<SecurityRuleEntry> emptyRuleEntryTail = new ArrayList<SecurityRuleEntry>();
        for (SecurityReference ref : entity.getReversedSecurityReferenceChain()) {
            SecurityRuleEntry entry = securityCache.get(ref);
            if (entry == null) {
                if (Right.getEnabledRights(ref.getType()).isEmpty()) {
                    // Do not call the reader on entity that will give useless rules
                    entry = new EmptySecurityRuleEntry(ref);
                    emptyRuleEntryTail.add(entry);
                } else {
                    entry = securityEntryReader.read(ref);
                    if (!emptyRuleEntryTail.isEmpty()) {
                        // Add intermediate empty rules sets to the cache to hold this significant one
                        for (SecurityRuleEntry emptyRuleEntry : emptyRuleEntryTail) {
                            securityCache.add(emptyRuleEntry);
                        }
                        emptyRuleEntryTail.clear();
                    }
                    securityCache.add(entry);
                }
            }
            rules.push(entry);
        }
        return rules;
    }

    /**
     * Extract the SecurityReference of EntityType.WIKI from the given SecurityReference.
     * @param entity The entity to be parsed.
     * @return the a SecurityReference representing a WikiReference.
     */
    private SecurityReference getWikiReference(SecurityReference entity) {
        SecurityReference result = entity;
        while (result != null && result.getType() != EntityType.WIKI) {
            result = result.getParentSecurityReference();
        }
        return result;
    }
}
