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
package org.xwiki.refactoring.event;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.AbstractCancelableEvent;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.stability.Unstable;

/**
 * Event fired when a document is about to be copied.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Unstable
public class DocumentCopyingEvent extends AbstractCancelableEvent implements BeginFoldEvent
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private final DocumentReference sourceReference;

    private final DocumentReference targetReference;

    /**
     * Default constructor, used by listeners.
     */
    public DocumentCopyingEvent()
    {
        this(null, null);
    }

    /**
     * Creates a new instance with the given data.
     * 
     * @param sourceReference the reference of the document that is about to be copied
     * @param targetReference the reference of the copy that will be created
     */
    public DocumentCopyingEvent(DocumentReference sourceReference, DocumentReference targetReference)
    {
        this.sourceReference = sourceReference;
        this.targetReference = targetReference;
    }

    /**
     * @return the reference of the document that is about to be copied
     */
    public DocumentReference getSourceReference()
    {
        return sourceReference;
    }

    /**
     * @return the reference of the copy that will be created
     */
    public DocumentReference getTargetReference()
    {
        return targetReference;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(3, 101).appendSuper(super.hashCode()).append(this.sourceReference)
            .append(this.targetReference).toHashCode();
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
        DocumentCopyingEvent documentCopyingEvent = (DocumentCopyingEvent) object;
        return new EqualsBuilder().appendSuper(super.equals(object))
            .append(this.sourceReference, documentCopyingEvent.sourceReference)
            .append(this.targetReference, documentCopyingEvent.targetReference).isEquals();
    }
}
