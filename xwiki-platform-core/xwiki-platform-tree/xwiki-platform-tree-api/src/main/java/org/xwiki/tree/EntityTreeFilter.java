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
package org.xwiki.tree;

import java.util.Set;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * The interface used to filter entity tree nodes.
 * 
 * @version $Id$
 * @since 11.10
 */
@Role
@Unstable
public interface EntityTreeFilter
{
    /**
     * @param parentReference specifies the parent node to filter
     * @return the set of child nodes to exclude from the specified parent
     */
    Set<EntityReference> getChildExclusions(EntityReference parentReference);

    /**
     * @param parentReference specifies the parent node to filter
     * @return the set of descendant nodes to exclude from the sub-tree rooted in the specified parent node
     */
    Set<EntityReference> getDescendantExclusions(EntityReference parentReference);
}
