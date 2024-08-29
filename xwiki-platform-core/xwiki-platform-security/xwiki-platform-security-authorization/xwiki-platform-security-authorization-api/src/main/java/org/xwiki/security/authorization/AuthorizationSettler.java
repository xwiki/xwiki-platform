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

import java.util.Collection;
import java.util.Deque;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.GroupSecurityReference;
import org.xwiki.security.UserSecurityReference;

/**
 * An AuthorizationSettler compute the resulting access for a given user,
 * a list of groups, and a hierarchy of rights objects defining rights on a given entity.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface AuthorizationSettler
{
    /**
     * Compute the current access for the user that is a member
     * of the given groups and on an entity which is protected by the
     * given hierarchy of rights objects.
     *
     * @param user a user identifier.
     * @param groups a collection of groups.
     * @param securityRuleEntries a hierarchy of security rules.  The list
     * is arranged such that the rules belonging to the main
     * wiki is put in the last collection, preceded by subwiki if
     * any, preceded by space and subspaces if any, preceded by the
     * document rules.  The levels of this hierarchy
     * match the structure of the entity reference reported by the first SecurityRuleEntry.
     * @return the computed access for the given user.
     */
    SecurityAccessEntry settle(UserSecurityReference user,
        Collection<GroupSecurityReference> groups,
        Deque<SecurityRuleEntry> securityRuleEntries);
}
