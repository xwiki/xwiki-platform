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
import java.util.Deque;
import java.util.LinkedList;
import java.util.Formatter;

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
            String message = new Formatter().format("Failed to load the cache in %d attempts.  Giving up.",
                                                    retries).toString();
            this.logger.error(message);
            throw new AuthorizationException(user.getOriginalDocumentReference(),
                                             entity.getOriginalReference(), message);
        }
    }

    /**
     * Load entity entries, group entries, and user entries required, than
     * call the settler and store and return the result.
     * 
     * @param user The user to check access for.
     * @param entity The entity to check access to.
     * @return The resulting access for the user at the entity.
     * @throws ParentEntryEvictedException If one of the parent
     * entries are evicted before the load is completed.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException On error.
     */
    private SecurityAccessEntry loadRequiredEntries(UserSecurityReference user, SecurityReference entity)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        if (entity == null) {
            return authorizationSettlerProvider.get().settle(user, loadGroupEntries(user), null);
        }

        SecurityRuleEntry entry = securityCache.get(entity);
        Deque<SecurityRuleEntry> ruleEntries = null;

        if (entry == null) {
            ruleEntries = getRules(entity);
            do {
                entry = ruleEntries.getFirst();
            } while (entry.isEmpty()
                && entry.getReference().getType() != EntityType.WIKI && (ruleEntries.pop() != null));
        } else {
            while (entry.isEmpty() && entry.getReference().getType() != EntityType.WIKI) {
                entry = securityCache.get(entry.getReference().getParentSecurityReference());
            }
        }
        return loadAccessEntries(user, entry.getReference(), ruleEntries);
    }

    /**
     * Load missing user and group entries, call the settler, than store and return the result.
     * Entity entries should have been loaded, and the entity request should have rules.
     * 
     * @param user The user to check access for.
     * @param entity The entity to check access to.
     * @param ruleEntries The rule entries loaded from the cache, may be null if not yet available.
     * @return The resulting access for the user at the entity.
     * @throws ParentEntryEvictedException If one of the parent
     * entries are evicted before the load is completed.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException On error.
     */
    private SecurityAccessEntry loadAccessEntries(UserSecurityReference user, SecurityReference entity,
        Deque<SecurityRuleEntry> ruleEntries)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        // Make sure the group entries are loaded
        Collection<GroupSecurityReference> groups = loadGroupEntries(user);

        // Make sure the user entry is loaded
        if (securityCache.get(user) == null) {
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

        Deque<SecurityRuleEntry> entries = (ruleEntries == null) ? getRules(entity) : ruleEntries;

        SecurityAccessEntry accessEntry = authorizationSettlerProvider.get().settle(user, groups, entries);

        securityCache.add(accessEntry);
        return accessEntry;
    }

    /**
     * Load group entries and return the list of groups associated with the given user.
     * 
     * @param user The user.
     * @return The collection of groups.
     * @throws ParentEntryEvictedException if any of the parent entries of the group
     * were evicted.
     * @throws ConflictingInsertionException When different threads
     * have inserted conflicting entries into the cache.
     * @throws org.xwiki.security.authorization.AuthorizationException on error.
     */
    private Collection<GroupSecurityReference> loadGroupEntries(UserSecurityReference user)
        throws ParentEntryEvictedException, ConflictingInsertionException, AuthorizationException
    {
        Collection<GroupSecurityReference> groups = userBridge.getAllGroupsFor(user);

        for (GroupSecurityReference group : groups) {
            getRules(group);
        }

        return groups;
    }

    /**
     * Retrieve rules for all hierarchy levels of the provided reference.
     * Rules may be read from the cache, or from the documents and fill the cache.
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
}
