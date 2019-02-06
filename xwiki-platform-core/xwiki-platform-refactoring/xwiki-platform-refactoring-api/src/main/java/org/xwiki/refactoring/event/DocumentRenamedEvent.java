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
import org.xwiki.observation.event.AbstractFilterableEvent;
import org.xwiki.observation.event.EndFoldEvent;
import org.xwiki.stability.Unstable;

/**
 * Event fired after a document has been renamed.
 * 
 * @version $Id$
 * @since 11.1RC1
 */
@Unstable
public class DocumentRenamedEvent extends AbstractFilterableEvent implements EndFoldEvent
{
    /**
     * Serialization identifier.
     */
    private static final long serialVersionUID = 1L;

    private final DocumentReference oldReference;

    private final DocumentReference newReference;

    /**
     * Default constructor, used by listeners.
     */
    public DocumentRenamedEvent()
    {
        this(null, null);
    }

    /**
     * Creates a new instance with the given data.
     * 
     * @param oldReference the old document reference
     * @param newReference the new document reference
     */
    public DocumentRenamedEvent(DocumentReference oldReference, DocumentReference newReference)
    {
        this.oldReference = oldReference;
        this.newReference = newReference;
    }

    /**
     * @return the old document reference
     */
    public DocumentReference getOldReference()
    {
        return oldReference;
    }

    /**
     * @return the new document reference
     */
    public DocumentReference getNewReference()
    {
        return newReference;
    }

    @Override
    public int hashCode()
    {
        return new HashCodeBuilder(11, 83).appendSuper(super.hashCode()).append(this.oldReference)
            .append(this.newReference).toHashCode();
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
        DocumentRenamedEvent documentRenamedEvent = (DocumentRenamedEvent) object;
        return new EqualsBuilder().appendSuper(super.equals(object))
            .append(this.oldReference, documentRenamedEvent.oldReference)
            .append(this.newReference, documentRenamedEvent.newReference).isEquals();
    }
}
