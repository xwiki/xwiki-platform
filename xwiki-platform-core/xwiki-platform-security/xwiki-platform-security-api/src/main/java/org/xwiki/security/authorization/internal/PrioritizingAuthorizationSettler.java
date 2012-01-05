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
import org.xwiki.security.authorization.RuleState;
import org.xwiki.security.authorization.SecurityRule;
import org.xwiki.security.authorization.SecurityRuleEntry;

import static org.xwiki.security.authorization.RuleState.UNDETERMINED;

/**
 * An implementation for the {@link org.xwiki.security.authorization.AuthorizationSettler}.
 * Provide similar decision as the old xwiki right service, but consider rules at the same
 * level by prioritizing user rules, over group rules and all group rules in this order.
 *
 * @version $Id$
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
    protected void settle(UserSecurityReference user, Collection<GroupSecurityReference> groups,
        SecurityRuleEntry entry, XWikiAccessLevel accessLevel)
    {
        XWikiAccessLevel currentLevel = new XWikiAccessLevel();
        Map<Right, Integer> priorities = new RightMap<Integer>();
        SecurityReference reference = entry.getReference();
        Set<Right> enabledRights = Right.getEnabledRights(reference.getSecurityType());

        // Evaluate rules from current level
        for (Right right : enabledRights) {
            for (SecurityRule obj : entry.getRules()) {
                if (obj.match(right)) {
                    resolveLevel(right, user, groups, obj, reference, priorities, currentLevel);
                }
            }
        }

        // Implies rights for current level
        // FIXME: ensure proper tie resolution on implied rights
        for (Right right : enabledRights) {
            if (currentLevel.get(right) == RuleState.ALLOW) {
                Set<Right> impliedRights = right.getImpliedRightsSet();
                if (impliedRights != null) {
                    for (Right enabledRight : enabledRights) {
                        if (impliedRights.contains(enabledRight)) {
                            currentLevel.allow(enabledRight);
                        }
                    }
                }
            }
        }

        // Inherit rules to lower level
        mergeLevels(currentLevel, accessLevel, reference);
    }


    /**
     * Update the resulting {@code accessLevel} to include the rule state defined by the given {@link SecurityRule}
     * for the given user and group, and the requested {@link Right}. The evaluated entity is provided here to
     * allow the special case of programming right that should not be evaluated in subwikis.
     *
     * @param right The right to settle.
     * @param user The user to check.
     * @param groups The groups where the user is a member.
     * @param rule The currently considered rule.
     * @param reference An entity reference that represents the current level in the document hierarchy.
     * @param priorities A map of current priorities of each rights in the current accumulated access level.
     * @param accessLevel The accumulated result at the current level.
     */
    private void resolveLevel(Right right,
        UserSecurityReference user, Collection<GroupSecurityReference> groups, SecurityRule rule,
        SecurityReference reference, Map<Right, Integer> priorities, XWikiAccessLevel accessLevel)
    {
        if (rule.match(user)) {
            resolveConflict(rule.getState(), right, USER_PRIORITY, priorities, accessLevel);
        } else {
            for (GroupSecurityReference group : groups) {
                if (rule.match(group)) {
                    resolveConflict(rule.getState(), right, getPriority(group), priorities, accessLevel);
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
     * @param priority The priority to use for this particular right match.
     * @param priorities A map of current priorities of each rights in the current accumulated access level.
     * @param accessLevel The accumulated result.
     */
    private void resolveConflict(RuleState state,
                                 Right right,
                                 int priority,
                                 Map<Right, Integer> priorities,
                                 XWikiAccessLevel accessLevel)
    {
        if (state == UNDETERMINED) {
            return;
        }
        if (accessLevel.get(right) == UNDETERMINED) {
            accessLevel.set(right, state);
            priorities.put(right, priority);
            return;
        }
        if (accessLevel.get(right) != state) {
            if (priority > priorities.get(right)) {
                accessLevel.set(right, state);
                priorities.put(right, priority);
            } else {
                accessLevel.set(right, right.getTieResolutionPolicy());
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
