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
package org.xwiki.refactoring.job.question;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Represent an entity with an information about either or not the entity is selected to perform some refactoring.
 *
 * @version $Id$
 * @since 9.1RC1
 */
@Unstable
public class EntitySelection
{
    /**
     * Reference to the entity to select for the refactoring.
     */
    private EntityReference entityReference;

    /**
     * Indicate if the entity is selected or not by the user.
     */
    private boolean isSelected = true;

    /**
     * Construct an EntitySelection.
     * @param entityReference the reference of the entity concerned by the refactoring
     */
    public EntitySelection(EntityReference entityReference)
    {
        this.entityReference = entityReference;
    }

    /**
     * @return the reference of the entity to select
     */
    public EntityReference getEntityReference()
    {
        return entityReference;
    }

    /**
     * @return if the user has selected the entity
     */
    public boolean isSelected()
    {
        return isSelected;
    }

    /**
     * Change the state of the entity.
     * @param selected either or not the user has selected the entity
     */
    public void setSelected(boolean selected)
    {
        isSelected = selected;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 13).append(getEntityReference()).append(isSelected).toHashCode();
    }

    @Override
    public boolean equals(Object object)
    {
        if (object == null) {
            return false;
        }
        if (object == this) {
            return true;
        }
        if (object.getClass() != getClass()) {
            return false;
        }
        EntitySelection entitySelection = (EntitySelection) object;
        return new EqualsBuilder().append(getEntityReference(), entitySelection.getEntityReference())
            .append(isSelected(), entitySelection.isSelected()).isEquals();
    }
}
