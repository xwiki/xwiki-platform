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
 *
 */
package org.xwiki.security;

import org.xwiki.component.annotation.ComponentRole;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

import java.util.Collection;
import java.util.List;

/**
 * A RightResolver will compute the access levels given a username, a
 * list of groups, and a hierarchy of rights objects.
 * @version $Id$
 */
@ComponentRole
public interface RightResolver
{
    /**
     * Compute the current access level for the user that is a member
     * of the given groups and on an entity which is protected by the
     * given hierarchy of rights objects.
     *
     * @param user a user identifier.
     * @param entity the entity on which the right is to be set.
     * @param entityKey gives the document hierarchy of the entity.
     * @param groups a collection of groups.
     * @param rightsObjects a hierarchy of rights objects.  The list
     * is arranged such that the rights objects belonging to the main
     * wiki is put in the first collection, followed by subwiki if
     * any, followed by space and subspaces if any, followed by the
     * document rights objects.  The levels of this hierarchy must
     * match the structure of the entity parameter.
     * @return the computed access level for the given user.
     */
    AccessLevel resolve(DocumentReference user,
                        EntityReference entity,
                        RightCacheKey entityKey,
                        Collection<DocumentReference> groups,
                        List<Collection<RightsObject>> rightsObjects);
}
