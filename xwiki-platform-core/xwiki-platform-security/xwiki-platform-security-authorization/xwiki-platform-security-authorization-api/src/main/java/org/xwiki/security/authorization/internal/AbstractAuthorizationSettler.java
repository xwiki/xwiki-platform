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

import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.EntityType;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.AuthorizationSettler;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityAccess;
import org.xwiki.security.authorization.SecurityAccessEntry;
import org.xwiki.security.authorization.SecurityRuleEntry;
import org.xwiki.security.internal.GroupSecurityEntry;
import org.xwiki.text.XWikiToStringBuilder;

/**
 * Abstract super class for right resolvers.
 * @version $Id$
 * @since 4.0M2
 */
abstract class AbstractAuthorizationSettler implements AuthorizationSettler
{
    /** Cache the initial tie resolution. */
    private static RightSet initialAllowTie;

    /** Cache the initial no override inheritance policy. */
    private static RightSet initialNoOverride;

    /** The initial policy size. Check to update initial policies if a new Right is added. */
    private static int initialPolicySize;

    /**
     * Private implementation of the {@link SecurityAccessEntry}.
     */
    private final class InternalSecurityAccessEntry extends AbstractSecurityAccessEntry implements GroupSecurityEntry
    {
        /** User reference. */
        private UserSecurityReference userReference;

        /** Entity reference. */
        private final SecurityReference reference;

        /** Security access. */
        private final SecurityAccess access;

        /**
         * @param user User reference
         * @param reference Entity reference
         * @param access access
         */
        InternalSecurityAccessEntry(UserSecurityReference user, SecurityReference reference,
            SecurityAccess access)
        {
            this.userReference = user;
            this.reference = reference;
            this.access = access;
        }

        @Override
        public UserSecurityReference getUserReference()
        {
            return this.userReference;
        }

        @Override
        public void setGroupReference(GroupSecurityReference reference)
        {
            this.userReference = reference;
        }
        
        @Override
        public SecurityAccess getAccess()
        {
            return this.access;
        }

        @Override
        public SecurityReference getReference()
        {
            return this.reference;
        }

        @Override
        public String toString()
        {
            ToStringBuilder builder = new XWikiToStringBuilder(this);
            builder.append("user", this.userReference);
            builder.append("entity", this.reference);
            builder.append("access", this.access);

            return builder.toString();
        }
    }

    /**
     * Current policies helper.
     */
    protected final class Policies
    {
        /** Current right which has an allow tie. */
        private Set<Right> allowTie;

        /** Current right which has an no override inheritance policy. */
        private Set<Right> noOverride;

        /**
         * Create Policies based on default initial policies.
         */
        Policies()
        {
            try {
                if (initialAllowTie == null || Right.size() != initialPolicySize) {
                    initialPolicySize = Right.size();
                    allowTie = new RightSet();
                    noOverride = new RightSet();
                    for (Right right : Right.values()) {
                        set(right, right);
                    }
                    initialAllowTie = ((RightSet) allowTie).clone();
                    initialNoOverride = ((RightSet) noOverride).clone();
                } else {
                    allowTie = initialAllowTie.clone();
                    noOverride = initialNoOverride.clone();
                }
            } catch (CloneNotSupportedException ignored) {
                // unexpected
            }
        }

        /**
         * Set the current tie and inheritance policy of an implied right
         * to the policies of the original right.
         * Once allowed, the resolution policy could not be denied.
         * Once a no override policy set, it could not be revoked.
         *
         * @param impliedRight the implied right to set
         * @param originalRight the original right to get
         */
        public void set(Right impliedRight, Right originalRight)
        {
            if (originalRight.getTieResolutionPolicy() == RuleState.ALLOW) {
                allowTie.add(impliedRight);
            }
            if (!originalRight.getInheritanceOverridePolicy()) {
                noOverride.add(impliedRight);
            }
        }

        /**
         * @param right the right to check.
         * @return the current tie resolution policy of this right.
         */
        public RuleState getTieResolutionPolicy(Right right)
        {
            return (allowTie.contains(right)) ? RuleState.ALLOW : RuleState.DENY;
        }

        /**
         * @param right the right to check.
         * @return the current tie resolution policy of this right.
         */
        public boolean getInheritanceOverridePolicy(Right right)
        {
            return !noOverride.contains(right);
        }
    }

    @Override
    public SecurityAccessEntry settle(UserSecurityReference user,
        Collection<GroupSecurityReference> groups, Deque<SecurityRuleEntry> ruleEntries)
    {
        XWikiSecurityAccess access = new XWikiSecurityAccess();
        SecurityReference reference = null;

        Policies policies = new Policies();

        for (SecurityRuleEntry entry : ruleEntries) {
            if (!entry.isEmpty()) {
                // Chose the highest possible level to store the resulting access
                if (reference == null) {
                    reference = entry.getReference();
                }
                // Compute access of this level and merge it with previous access result
                merge(settle(user, groups, entry, policies), access, entry.getReference(), policies);
            }
            if (reference == null && entry.getReference().getType() == EntityType.WIKI) {
                reference = entry.getReference();
            }
        }

        // Apply defaults and return the resulting access entry
        return new InternalSecurityAccessEntry(user, reference, applyDefaults(user, reference, access));
    }

    /**
     * Apply default values for undetermined rights.
     *
     * @param user The user, whose rights are to be determined.
     * @param reference The entity, which the user wants to access.
     * @param access The accumulated access result (modified and returned).
     * @return the accumulated access result.
     */
    protected XWikiSecurityAccess applyDefaults(UserSecurityReference user, SecurityReference reference,
        XWikiSecurityAccess access)
    {
        for (Right right : Right.values()) {
            if (access.get(right) == RuleState.UNDETERMINED) {
                if (!user.isGlobal() && !user.getOriginalReference().getWikiReference()
                        .equals(reference.extractReference(EntityType.WIKI))) {
                    /*
                     * Deny all by default for users from another wiki.
                     */
                    access.deny(right);
                } else {
                    access.set(right, right.getDefaultState());
                }
            }
        }

        return access;
    }

    /**
     * Compute the access of a particular document hierarchy level.
     * @param user The user.
     * @param groups The groups where the user is a member.
     * @param entry The security entry to settle.
     * @param policies the current security policies.
     * @return the resulting access for the user/group based on the given rules.
     */
    protected abstract XWikiSecurityAccess settle(UserSecurityReference user, Collection<GroupSecurityReference> groups,
        SecurityRuleEntry entry, Policies policies);

    /**
     * Merge the current access with the result from previous ones.
     * @param currentAccess The access computed at the current entity in the document hierarchy.
     * @param access The resulting access previously computed (modified and returned).
     * @param reference the current entity in the document hierarchy.
     * @param policies the current security policies.
     */
    protected void merge(SecurityAccess currentAccess, XWikiSecurityAccess access,
        SecurityReference reference, Policies policies)
    {
        for (Right right : Right.getEnabledRights(reference.getSecurityType())) {
            // Skip undetermined rights
            if (currentAccess.get(right) == RuleState.UNDETERMINED) {
                continue;
            }
            if (access.get(right) == RuleState.UNDETERMINED) {
                access.set(right, currentAccess.get(right));
                continue;
            }
            if (currentAccess.get(right) == RuleState.ALLOW && !policies.getInheritanceOverridePolicy(right)) {
                access.allow(right);
            }
        }
    }
}
