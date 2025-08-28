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

import java.util.Optional;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.xwiki.model.reference.EntityReference;
import org.xwiki.stability.Unstable;

/**
 * Represent an entity with an information about either or not the entity is selected to perform some refactoring.
 *
 * @version $Id$
 * @since 9.1RC1
 */
public class EntitySelection implements Comparable<EntitySelection>
{
    /**
     * Define the inner state of the EntitySelection.
     */
    public enum State
    {
        /**
         * If a user selected it.
         */
        SELECTED,

        /**
         * If a user deselected it.
         */
        DESELECTED,

        /**
         * Default state. By default, UNKNOWN is considered as selected: see {@link #isSelected()}.
         */
        UNKNOWN
    };

    /**
     * Reference to the entity to select for the refactoring.
     */
    private final EntityReference sourceEntityReference;
    private final EntityReference targetEntityReference;

    /**
     * Indicate if the entity is selected or not by the user.
     */
    private State isSelected = State.UNKNOWN;

    /**
     * Construct an EntitySelection.
     * @param entityReference the reference of the entity concerned by the refactoring
     */
    public EntitySelection(EntityReference entityReference)
    {
        this(entityReference, null);
    }

    /**
     * Constructor of an EntitySelection when there is a target destination.
     * @param sourceEntityReference the original reference
     * @param targetEntityReference the target reference of the refactoring
     * @since 16.10.0RC1
     */
    @Unstable
    public EntitySelection(EntityReference sourceEntityReference, EntityReference targetEntityReference)
    {
        this.sourceEntityReference = sourceEntityReference;
        this.targetEntityReference = targetEntityReference;
    }

    /**
     * @return the reference of the entity to select
     */
    public EntityReference getEntityReference()
    {
        return sourceEntityReference;
    }

    /**
     * @return the target reference of the refactoring if any.
     * @since 16.10.0RC1
     */
    @Unstable
    public Optional<EntityReference> getTargetEntityReference()
    {
        return (targetEntityReference != null) ? Optional.of(targetEntityReference) : Optional.empty();
    }

    /**
     * @return true if the user has selected the entity or no choice has been made
     */
    public boolean isSelected()
    {
        return isSelected == State.UNKNOWN || isSelected == State.SELECTED;
    }

    /**
     * @return current state of the entity selection.
     */
    public State getState()
    {
        return isSelected;
    }

    /**
     * Change the state of the entity.
     * @param selected either or not the user has selected the entity
     */
    public void setSelected(boolean selected)
    {
        if (selected) {
            isSelected = State.SELECTED;
        } else {
            isSelected = State.DESELECTED;
        }
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 13)
            .append(getEntityReference())
            .append(getTargetEntityReference())
            .append(isSelected)
            .toHashCode();
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
        return new EqualsBuilder()
            .append(getEntityReference(), entitySelection.getEntityReference())
            .append(getTargetEntityReference(), entitySelection.getTargetEntityReference())
            .append(isSelected(), entitySelection.isSelected())
            .isEquals();
    }

    @Override
    public String toString()
    {
        return new ToStringBuilder(this)
            .append("sourceEntityReference", sourceEntityReference)
            .append("targetEntityReference", targetEntityReference)
            .append("isSelected", isSelected)
            .toString();
    }

    @Override
    public int compareTo(EntitySelection entitySelection)
    {
        if (entitySelection == null) {
            throw new NullPointerException("Provided entitySelection should not be null.");
        }

        if (entitySelection == this) {
            return 0;
        }

        if (sourceEntityReference == null) {
            return -1;
        }

        if (entitySelection.sourceEntityReference == null) {
            return 1;
        }

        int result = sourceEntityReference.compareTo(entitySelection.sourceEntityReference);

        if (result == 0) {
            return isSelected.compareTo(entitySelection.isSelected);
        } else {
            return result;
        }
    }
}
