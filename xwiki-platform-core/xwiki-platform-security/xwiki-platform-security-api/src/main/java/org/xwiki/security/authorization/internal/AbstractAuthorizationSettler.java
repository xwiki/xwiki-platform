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
package org.xwiki.security.authorization.internal;

import java.util.Collection;
import java.util.Deque;
import java.util.Set;

import javax.inject.Inject;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.WikiReference;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AccessLevel;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.internal.EntityBridge;
import org.xwiki.security.internal.XWikiBridge;

/**
 * Abstract super class for right resolvers.
 * @version $Id$
 */
abstract class AbstractAuthorizationSettler implements AuthorizationSettler
{
    /**
     * Entity bridge used to check entity creator.
     */
    @Inject
    private EntityBridge entityBridge;

    /**
     * XWiki bridge used to check wiki owner.
     */
    @Inject
    private XWikiBridge wikiBridge;

    /**
     * Private implementation of the {@link SecurityAccessEntry}.
     */
    private final class InternalSecurityAccessEntry extends AbstractSecurityAccessEntry
    {
        /** User reference. */
        private final UserSecurityReference userReference;

        /** Entity reference. */
        private final SecurityReference reference;

        /** Access level. */
        private final AccessLevel accessLevel;

        /**
         * @param user User reference
         * @param reference Entity reference
         * @param accessLevel Access level
         */
        InternalSecurityAccessEntry(UserSecurityReference user, SecurityReference reference, AccessLevel accessLevel)
        {
            this.userReference = user;
            this.reference = reference;
            this.accessLevel = accessLevel;
        }

        @Override
        public UserSecurityReference getUserReference()
        {
            return this.userReference;
        }

        @Override
        public AccessLevel getAccessLevel()
        {
            return this.accessLevel;
        }

        @Override
        public SecurityReference getReference()
        {
            return this.reference;
        }
    }

    @Override
    public SecurityAccessEntry settle(UserSecurityReference user,
        Collection<GroupSecurityReference> groups, Deque<SecurityRuleEntry> ruleEntries)
    {
        XWikiAccessLevel accessLevel = new XWikiAccessLevel();
        SecurityReference reference = ruleEntries.getFirst().getReference();

        for (SecurityRuleEntry entry : ruleEntries) {
            settle(user, groups, entry, accessLevel);
        }

        postProcess(user, reference, accessLevel);

        return new InternalSecurityAccessEntry(user, reference, accessLevel.getExistingInstance());
    }

    /**
     * Fill out default values and add implied rights.
     *
     * @param user The user, whose rights are to be determined.
     * @param reference The entity, which the user wants to access.
     * @param accessLevel The accumulated result.
     */
    protected void postProcess(UserSecurityReference user, SecurityReference reference, XWikiAccessLevel accessLevel)
    {
        // Wiki owner is granted admin rights.
        if (wikiBridge.isWikiOwner(user, new WikiReference(reference.extractReference(EntityType.WIKI)))) {
            accessLevel.allow(Right.ADMIN);
            // Implies rights for current level
            Set<Right> impliedRights = Right.ADMIN.getImpliedRightsSet();
            for (Right enabledRight : Right.getEnabledRights(EntityType.WIKI)) {
                if (impliedRights.contains(enabledRight)) {
                    accessLevel.allow(enabledRight);
                }
            }
        } else {
            // Creator is granted delete-rights.
            if (accessLevel.get(Right.DELETE) == RuleState.UNDETERMINED
                && Right.getEnabledRights(reference.getSecurityType()).contains(Right.DELETE)
                && entityBridge.isDocumentCreator(user, reference)) {
                accessLevel.allow(Right.DELETE);
            }
        }

        for (Right right : Right.values()) {
            if (accessLevel.get(right) == RuleState.UNDETERMINED) {
                if (!user.getOriginalReference().getWikiReference()
                        .equals(reference.extractReference(EntityType.WIKI))) {
                    /*
                     * Deny all by default for users from another wiki.
                     */
                    accessLevel.deny(right);
                } else {
                    accessLevel.set(right, right.getDefaultState());
                }
            }
        }
    }

    /**
     * Compute the access level of a particular document hierarchy level.
     * @param user The user.
     * @param groups The groups where the user is a member.
     * @param entry The security entry to settle.
     * @param accessLevel The accumulated result.
     */
    protected abstract void settle(UserSecurityReference user, Collection<GroupSecurityReference> groups,
        SecurityRuleEntry entry, XWikiAccessLevel accessLevel);

    /**
     * Merge the current levels with the result from previous ones.
     * @param currentLevel The access level computed at the current level in the document hierarchy.
     * @param accessLevel The resulting access levels previously computed.
     * @param reference {@link org.xwiki.security.SecurityReference} that specifies the current level in the document hierarchy.
     */
    protected void mergeLevels(AccessLevel currentLevel, XWikiAccessLevel accessLevel, SecurityReference reference)
    {
        for (Right right : Right.getEnabledRights(reference.getSecurityType())) {
            // Skip undetermined rights
            if (currentLevel.get(right) == RuleState.UNDETERMINED) {
                continue;
            }
            if (accessLevel.get(right) == RuleState.UNDETERMINED) {
                accessLevel.set(right, currentLevel.get(right));
                continue;
            }
            if (currentLevel.get(right) == RuleState.ALLOW && !right.getInheritanceOverridePolicy()) {
                accessLevel.allow(right);
            }
        }
    }
}
