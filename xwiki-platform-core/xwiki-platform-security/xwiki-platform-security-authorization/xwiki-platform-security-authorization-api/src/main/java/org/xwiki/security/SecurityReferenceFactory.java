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
package org.xwiki.security;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Factory to create new {@link SecurityReference}, {@link UserSecurityReference} and {@link GroupSecurityReference}.
 *
 * @version $Id$
 * @since 4.0M2
 */
@Role
public interface SecurityReferenceFactory
{
    /**
     * Returns a {@link SecurityReference} for the provided entity.
     * @param reference the entity reference to clone
     * @return a {@link SecurityReference} for the provided entity.
     */
    SecurityReference newEntityReference(EntityReference reference);

    /**
     * Returns a {@link UserSecurityReference} for the provided user document reference.
     * @param reference the entity reference to clone
     * @return a {@link UserSecurityReference} for the provided user document reference.
     */
    UserSecurityReference newUserReference(DocumentReference reference);

    /**
     * Returns a {@link GroupSecurityReference} for the provided group document reference.
     * @param reference the entity reference to clone
     * @return a {@link GroupSecurityReference} for the provided group document reference.
     */
    GroupSecurityReference newGroupReference(DocumentReference reference);
}
