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
import java.util.Map;
import java.util.Set;

import javax.inject.Named;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.SecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightMap;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;

import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * An implementation for the {@link org.xwiki.security.authorization.AuthorizationSettler}.
 * Provide similar decision as the old xwiki right service, but consider rules at the same
 * level by prioritizing user rules, over group rules and all group rules in this order.
 *
 * IMPORTANT NOTE: This experimental settler is current unmaintained and untested. Use at you own risk.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Component
@Named("priority")
@Singleton
public class PrioritizingAuthorizationSettler extends AbstractAuthorizationSettler
{
    /** Priority of rights specified for users. */
    private static final int USER_PRIORITY = Integer.MAX_VALUE;

    /** Priority of rights specified for "all group". */
    private static final int ALL_GROUP_PRIORITY = 0;

    @Override
    protected XWikiSecurityAccess settle(UserSecurityReference user, Collection<GroupSecurityReference> groups,
        SecurityRuleEntry entry, Policies policies)
    {
        XWikiSecurityAccess access = new XWikiSecurityAccess();
        Map<Right, Integer> priorities = new RightMap<Integer>();
        SecurityReference reference = entry.getReference();
        Set<Right> enabledRights = Right.getEnabledRights(reference.getSecurityType());

        // Evaluate rules from current level
        for (Right right : enabledRights) {
            for (SecurityRule obj : entry.getRules()) {
                if (obj.match(right)) {
                    resolveLevel(right, user, groups, obj, access, policies, priorities);
                    if (access.get(right) == ALLOW) {
                        implyRights(right, access, reference, policies, priorities);
                    }
                }
            }
        }

        return access;
    }

    /**
     * Add implied rights of the given right into the current access.
     *
     * @param right the right to imply right for.
     * @param access the access to be augmented (modified and returned).
     * @param reference the reference to imply rights for.
     * @param policies the current security policies.
     * @param priorities A map of current priorities of each rights in the current accumulated access result.
     */
    private void implyRights(Right right, XWikiSecurityAccess access, SecurityReference reference,
        Policies policies, Map<Right, Integer> priorities)
    {
        Set<Right> impliedRights = right.getImpliedRights();
        if (impliedRights != null) {
            for (Right enabledRight : Right.getEnabledRights(reference.getSecurityType())) {
                if (impliedRights.contains(enabledRight)) {
                    // set the policies of the implied right to the policies of the original right
                    policies.set(enabledRight, right);
                    resolveConflict(ALLOW, enabledRight, access, policies, priorities.get(right), priorities);
                }
            }
        }
    }

    /**
     * Update the resulting {@code access} to include the rule state defined by the given {@link SecurityRule}
     * for the given user and group, and the requested {@link Right}.
     *
     * @param right The right to settle.
     * @param user The user to check.
     * @param groups The groups where the user is a member.
     * @param rule The currently considered rule.
     * @param access The accumulated access result during interpretation of rules.
     * @param policies the current security policies.
     * @param priorities A map of current priorities of each rights in the current accumulated access result.
     *
     */
    private void resolveLevel(Right right,
        UserSecurityReference user, Collection<GroupSecurityReference> groups, SecurityRule rule,
        XWikiSecurityAccess access, Policies policies, Map<Right, Integer> priorities)
    {
        RuleState state = rule.getState();
        if (state == UNDETERMINED) {
            return;
        }

        if (rule.match(user)) {
            resolveConflict(state, right, access, policies, USER_PRIORITY, priorities);
        } else {
            for (GroupSecurityReference group : groups) {
                if (rule.match(group)) {
                    resolveConflict(state, right, access, policies, getPriority(group), priorities);
                    break;
                }
            }
        }
    }

    /**
     * Resolve conflicting rights within the current level in the document hierarchy.
     *
     * @param state The state to consider setting.
     * @param right The right that is being concerned.
     * @param access The accumulated access result.
     * @param policies the current security policies.
     * @param priority The priority to use for this particular right match.
     * @param priorities A map of current priorities of each rights in the current accumulated access level.
     */
    private void resolveConflict(RuleState state, Right right, XWikiSecurityAccess access, Policies policies,
        int priority, Map<Right, Integer> priorities)
    {
        if (access.get(right) == UNDETERMINED) {
            access.set(right, state);
            priorities.put(right, priority);
            return;
        }
        if (access.get(right) != state) {
            if (priority > priorities.get(right)) {
                access.set(right, state);
                priorities.put(right, priority);
            } else {
                access.set(right, policies.getTieResolutionPolicy(right));
            }
        }
    }

    /**
     * @param group A group identifier.
     * @return the priority for the group.
     */
    private int getPriority(GroupSecurityReference group)
    {
        if (group.getName().equals("XWikiAllGroup")) {
            return ALL_GROUP_PRIORITY;
        } else {
            return ALL_GROUP_PRIORITY + 1;
        }
    }
}
