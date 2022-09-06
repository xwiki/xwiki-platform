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
package org.xwiki.link;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Allow accessing the links extracted from various entities.
 * 
 * @version $Id$
 * @since 14.8RC1
 */
@Role
@Unstable
public interface LinkStore
{
    // TODO: also introduce an API (#getLinks) to get all types of links and not only entities

    /**
     * @param reference the reference of the entity containing links
     * @return the other (DOCUMENT based) entities linked by the passed entity
     * @throws LinkException when failing to load the links
     */
    Set<EntityReference> resolveLinkedEntities(EntityReference reference) throws LinkException;

    /**
     * @param reference the reference of the entities targeted by the links
     * @return the (DOCUMENT based) entities containing links to the passed entity
     * @throws LinkException when failing to load the backlinks
     */
    Set<EntityReference> resolveBackLinkedEntities(EntityReference reference) throws LinkException;
}
