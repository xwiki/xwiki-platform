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

import java.util.Optional;
import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.security.SecurityReference;
import org.xwiki.stability.Unstable;

/**
 * A security rules reader reads rules attached to a given entity.
 * <p>
 * This interface decouple the rules storage from the security cache loader.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface SecurityEntryReader
{
    /**
     * Read a collection of rules attached to a given entity.
     *
     * @param entityReference reference to the entity.
     * @return the access rules read from the given reference.
     * @throws AuthorizationException on error.
     */
    SecurityRuleEntry read(SecurityReference entityReference) throws AuthorizationException;

    /**
     * @param entity the entity to get required rights from
     * @return {@link Optional#empty()} if required rights are not applicable for the entity (i.e., it is not a
     *     document, or the required rights are not activated on the document), the set of required rights otherwise
     * @throws AuthorizationException in case of right issue when access the required rights
     * @since 15.5RC1
     */
    @Unstable
    default Optional<Set<Right>> requiredRights(SecurityReference entity) throws AuthorizationException
    {
        return Optional.empty();
    }
}
