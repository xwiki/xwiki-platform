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
package org.xwiki.refactoring.job;

import org.xwiki.model.reference.EntityReference;

/**
 * Represents a request used for Copy, Move or Rename (though Move).
 *
 * @version $Id$
 * @since 10.11RC1
 */
public abstract class AbstractCopyOrMoveRequest extends EntityRequest
{
    /**
     * @see #getDestination()
     */
    private static final String PROPERTY_DESTINATION = "destination";

    /**
     * @return the destination entity, where to move the entities specified by {@link #getEntityReferences()}
     */
    public EntityReference getDestination()
    {
        return getProperty(PROPERTY_DESTINATION);
    }

    /**
     * Sets the destination entity, where to move the entities specified by {@link #getEntityReferences()}.
     *
     * @param destination the destination entity
     */
    public void setDestination(EntityReference destination)
    {
        setProperty(PROPERTY_DESTINATION, destination);
    }
}
