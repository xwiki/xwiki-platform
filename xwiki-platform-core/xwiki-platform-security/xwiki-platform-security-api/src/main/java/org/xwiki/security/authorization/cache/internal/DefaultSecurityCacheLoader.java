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
import java.util.Collection;
import java.util.Deque;
import java.util.HashSet;
import java.util.LinkedList;

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
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityEntryReader;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.authorization.cache.ConflictingInsertionException;
import org.xwiki.security.authorization.cache.ParentEntryEvictedException;
import org.xwiki.security.authorization.cache.SecurityCache;
import org.xwiki.security.authorization.cache.SecurityCacheLoader;
import org.xwiki.security.authorization.cache.SecurityCacheRulesInvalidator;
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
                loadUserEntry(user, getWikiReference(user), null), null);
        }

        // Retrieve rules for the entity from the cache
        SecurityRuleEntry entry = securityCache.get(entity);
        Deque<SecurityRuleEntry> ruleEntries = null;

        if (entry == null) {
            // Cached rules missed, retrieve rules from entities and store them in the cache
            ruleEntries = getRules(entity);
            // pop out empty rules to evaluate and store access at highest possible entity
            do {
                entry = ruleEntries.getFirst();
            } while (entry.isEmpty()
                && entry.getReference().getType() != EntityType.WIKI && (ruleEntries.pop() != null));
        } else {
            // Cached rules found, pop out empty rules to evaluate and store access at the highest possible entity
            while (entry.isEmpty() && entry.getReference().getType() != EntityType.WIKI) {
                entry = securityCache.get(entry.getReference().getParentSecurityReference());
            }
            ruleEntries = getRules(entity);
        }

        // Evaluate, store and return the access right
        return loadAccessEntries(user, entry.getReference(), ruleEntries);
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
        SecurityReference userWiki = getWikiReference(user);
        // entityWiki is the wiki of the entity when the user is global and the entity is local
        SecurityReference entityWiki = (entity != null && user.isGlobal()) ? getWikiReference(entity) : null;
        if (entityWiki != null && userWiki.equals(entityWiki)) {
            entityWiki = null;
        }

        // Load user and related groups into the cache (global and shadowed locals) as needed
        Collection<GroupSecurityReference> groups = loadUserEntry(user, userWiki, entityWiki);

        // Settle the access
        SecurityAccessEntry accessEntry = authorizationSettlerProvider.get().settle(user, groups, ruleEntries);

        // Store the result into the cache
        if (entityWiki != null) {
            securityCache.add(accessEntry, entityWiki);
        } else {
            securityCache.add(accessEntry);
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
        Collection<GroupSecurityReference> groups = new HashSet<GroupSecurityReference>();

        // Load the user and related groups into the cache
        Collection<GroupSecurityReference> globalGroups = new HashSet<GroupSecurityReference>();
        loadUserEntry(user, userWiki, null, globalGroups);
        groups.addAll(globalGroups);
        if (entityWiki != null) {
            // Entity is in a local wiki for a global user
            Collection<GroupSecurityReference> localGroups;
            // Load shadows of user's global group into the cache
            for (GroupSecurityReference group : globalGroups) {
                localGroups = new HashSet<GroupSecurityReference>();
                loadUserEntry(group, userWiki, entityWiki, localGroups);
                groups.addAll(localGroups);
            }
            // Load shadow of the user into the cache
            localGroups = new HashSet<GroupSecurityReference>();
            loadUserEntry(user, userWiki, entityWiki, localGroups);
            groups.addAll(localGroups);
        }

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
     *                  (During recursive calls, it contains the result so far, and allow limiting the recursion.)
     * @throws ParentEntryEvictedException if any of the parent entries of the group were evicted.
     * @throws ConflictingInsertionException When different threads have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private void loadUserEntry(UserSecurityReference user, SecurityReference userWiki,
        SecurityReference entityWiki, Collection<GroupSecurityReference> allGroups)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // Retrieve the list of immediate group for the user/group in either the entity wiki or the user/group wiki
        Collection<GroupSecurityReference> groups = (entityWiki != null)
            ? userBridge.getAllGroupsFor(user, entityWiki.getOriginalWikiReference())
            : userBridge.getAllGroupsFor(user, userWiki.getOriginalWikiReference());

        Collection<GroupSecurityReference> userGroups = new ArrayList<GroupSecurityReference>();

        // Loads all immediate groups recursively, collecting indirect groups along the way
        for (GroupSecurityReference group : groups) {
            if (allGroups.add(group)) {
                // Call in recursion only if the group has never been seen before, avoid infinite recursion
                loadUserEntry(group, (entityWiki != null) ? entityWiki : userWiki, null, allGroups);
                userGroups.add(group);
            }
        }

        // Store the user/group in the cache
        if (entityWiki != null) {
            // Store a shadow entry for a global user/group involved in a local wiki
            securityCache.add(new DefaultSecurityShadowEntry(user, entityWiki), userGroups);
        } else {
            // If not yet in the cache, retrieve associated rules store the user/group in the cache
            if (securityCache.get(user) == null) {
                loadUserEntry(user, userGroups);
            }
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
    @SuppressWarnings("unchecked")
    private Deque<SecurityRuleEntry> getRules(SecurityReference entity)
        throws AuthorizationException, ParentEntryEvictedException, ConflictingInsertionException
    {
        Deque<SecurityRuleEntry> rules = new LinkedList<SecurityRuleEntry>();
        for (SecurityReference ref : entity.getReversedSecurityReferenceChain()) {
            SecurityRuleEntry entry = securityCache.get(ref);
            if (entry == null) {
                entry = securityEntryReader.read(ref);
                securityCache.add(entry);
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
