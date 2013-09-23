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
import java.util.Set;

import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;
import org.xwiki.security.authorization.Right;
import org.xwiki.security.authorization.RightSet;
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;

import static org.xwiki.security.authorization.RuleState.ALLOW;
import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * The default implementation for the {@link org.xwiki.security.authorization.AuthorizationSettler}.
 * Provide similar decision as the old xwiki right service.
 *
 * @version $Id$
 * @since 4.0M2 
 */
@Component
@Singleton
public class DefaultAuthorizationSettler extends AbstractAuthorizationSettler
{
    @Override
    protected XWikiSecurityAccess settle(UserSecurityReference user, Collection<GroupSecurityReference> groups,
        SecurityRuleEntry entry, Policies policies)
    {
        Set<Right> enabledRights = Right.getEnabledRights(entry.getReference().getSecurityType());
        Set<Right> fromUser = new RightSet();
        Set<Right> allowed = new RightSet();

        XWikiSecurityAccess access = new XWikiSecurityAccess();

        // Evaluate rules from current entity
        for (Right right : enabledRights) {
            for (SecurityRule rule : entry.getRules()) {
                if (rule.match(right)) {
                    if (rule.getState() == ALLOW) {
                        allowed.add(right);
                    }
                    resolveLevel(right, user, groups, rule, access, policies, fromUser);
                    if (access.get(right) == ALLOW) {
                        implyRights(right, access, enabledRights, policies, fromUser);
                    }
                }
            }
        }

        // The same behavior as the old implementation. I.e., an allow means implicit deny for everyone else.
        for (Right right : allowed) {
            if (access.get(right) == UNDETERMINED) {
                access.deny(right);
            }
        }

        return access;
    }

    /**
     * Add implied rights of the given right into the current access.
     *
     * @param right the right to imply right for.
     * @param access the access to be augmented (modified and returned).
     * @param enabledRights the set of right that could be allowed for the current reference
     * @param policies the current security policies.
     * @param fromUser the set of right that have been set by a user rule.
     */
    private void implyRights(Right right, XWikiSecurityAccess access, Set<Right> enabledRights, Policies policies,
        Set<Right> fromUser)
    {
        Set<Right> impliedRights = right.getImpliedRights();
        if (impliedRights != null) {
            for (Right enabledRight : enabledRights) {
                if (impliedRights.contains(enabledRight)) {
                    // set the policies of the implied right to the policies of the original right
                    policies.set(enabledRight, right);
                    if (fromUser.contains(enabledRight) == fromUser.contains(right)) {
                        // Conflict Implied user/group right, user/group right
                        resolveConflict(ALLOW, enabledRight, access, policies);
                    } else if (fromUser.contains(right)) {
                        // Implied user right win over group right
                        access.set(enabledRight, ALLOW);
                        fromUser.add(enabledRight);
                    }
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
     * @param access The accumulated access result.
     * @param policies the current security policies.
     * @param fromUser the set of right that have been set by a user rule.
     */
    private void resolveLevel(Right right, UserSecurityReference user, Collection<GroupSecurityReference> groups,
        SecurityRule rule, XWikiSecurityAccess access, Policies policies, Set<Right> fromUser)
    {
        RuleState state = rule.getState();

        if (state == UNDETERMINED) {
            return;
        }

        if (rule.match(user)) {
            if (!fromUser.contains(right)) {
                // User right win over group right
                access.set(right, state);
                fromUser.add(right);
            } else {
                // Conflict between user rights
                resolveConflict(state, right, access, policies);
            }
        } else if (!fromUser.contains(right)) {
            for (GroupSecurityReference group : groups) {
                if (rule.match(group)) {
                    // Conflict between group rights
                    resolveConflict(state, right, access, policies);
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
     * @param access The accumulated result.
     * @param policies the current security policies.
     */
    private void resolveConflict(RuleState state, Right right, XWikiSecurityAccess access, Policies policies)
    {        
        if (access.get(right) == UNDETERMINED) {
            access.set(right, state);
            return;
        }
        if (access.get(right) != state) {
            access.set(right, policies.getTieResolutionPolicy(right));
        }
    }
}
