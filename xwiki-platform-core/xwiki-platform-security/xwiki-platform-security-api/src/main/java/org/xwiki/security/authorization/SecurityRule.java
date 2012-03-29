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
package org.xwiki.security.authorization;

import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;

/**
 * A security rule, representing a declarative access right for some users, groups, and rights.
 *
 * @version $Id$
 * @since 4.0M2
 */
public interface SecurityRule
{
    /**
     * Check if this rule match the given right.
     * @param right The right to match.
     * @return {@code true} if the state should be applied for the right.
     */
    boolean match(Right right);

    /**
     * Check if this rule match the given group.
     *
     * @param group The group to match.
     * @return {@code true} if the state should be applied for group.
     */
    boolean match(GroupSecurityReference group);

    /**
     * Check if this rule match the given user.
     *
     * @param user The user to match.
     * @return {@code true} if the state should be applied for user.
     */
    boolean match(UserSecurityReference user);

    /**
     * @return The {@code RuleState} of this rule.
     */
    RuleState getState();
}
