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
package org.xwiki.refactoring.internal.event;

import java.io.Serializable;
import java.util.Objects;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.EntityReference;

/**
 * Base class for events triggered when an entity is copied or renamed.
 * 
 * @version $Id$
 * @since 11.1RC1
 * @param <T> the entity type
 */
public abstract class AbstractEntityCopyOrRenameEvent<T extends EntityReference> extends AbstractEvent
    implements Serializable
{
    private static final long serialVersionUID = 1L;

    /**
     * The source reference.
     */
    private final T sourceReference;

    /**
     * The target reference.
     */
    private final T targetReference;

    /**
     * Default constructor, used by listeners.
     */
    public AbstractEntityCopyOrRenameEvent()
    {
        this(null, null);
    }

    /**
     * Creates a new instance with the given data.
     * 
     * @param sourceReference the reference of the source entity
     * @param targetReference the reference of the target entity
     */
    public AbstractEntityCopyOrRenameEvent(T sourceReference, T targetReference)
    {
        this.sourceReference = sourceReference;
        this.targetReference = targetReference;
    }

    /**
     * @return the reference of the source entity
     */
    public T getSourceReference()
    {
        return sourceReference;
    }

    /**
     * @return the reference of the target entity
     */
    public T getTargetReference()
    {
        return targetReference;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(7, 91).append(this.sourceReference).append(this.targetReference).toHashCode();
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
        AbstractEntityCopyOrRenameEvent<?> event = (AbstractEntityCopyOrRenameEvent<?>) object;
        return new EqualsBuilder().append(this.sourceReference, event.sourceReference)
            .append(this.targetReference, event.targetReference).isEquals();
    }

    @Override
    public boolean matches(Object occuringEvent)
    {
        boolean matches = super.matches(occuringEvent);
        if (matches) {
            AbstractEntityCopyOrRenameEvent<?> copyOrRenameEvent = (AbstractEntityCopyOrRenameEvent<?>) occuringEvent;
            matches = matches(this.sourceReference, copyOrRenameEvent.getSourceReference())
                && matches(this.targetReference, copyOrRenameEvent.getTargetReference());
        }
        return matches;
    }

    private static boolean matches(EntityReference expected, EntityReference actual)
    {
        return expected == null || expected.equals(actual) || hasParentOfSameType(actual, expected);
    }

    private static boolean hasParentOfSameType(EntityReference entityReference, EntityReference parentReference)
    {
        return entityReference != null && Objects.equals(entityReference.getType(), parentReference.getType())
            && entityReference.hasParent(parentReference);
    }
}
