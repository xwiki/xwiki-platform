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
package org.xwiki.bridge.event;

import java.util.Objects;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.filter.EventFilter;

/**
 * Base class for event related to the restoration of a deleted document.
 * 
 * @version $Id$
 * @since 13.7
 */
public abstract class AbstractDocumentRestoreEvent extends AbstractDocumentEvent
{
    private static final long serialVersionUID = 1L;

    protected final Long deleteId;

    protected AbstractDocumentRestoreEvent()
    {
        this.deleteId = null;
    }

    protected AbstractDocumentRestoreEvent(DocumentReference documentReference)
    {
        super(documentReference);

        this.deleteId = null;
    }

    protected AbstractDocumentRestoreEvent(DocumentReference documentReference, long deleteId)
    {
        super(documentReference);

        this.deleteId = deleteId;
    }

    /**
     * Constructor using a custom {@link EventFilter}.
     * 
     * @param eventFilter the filter to use for matching events
     */
    protected AbstractDocumentRestoreEvent(EventFilter eventFilter)
    {
        super(eventFilter);

        this.deleteId = null;
    }

    /**
     * @return the unique identifier of the deleted document
     */
    public Long getDeleteId()
    {
        return this.deleteId;
    }

    @Override
    public boolean matches(Object otherEvent)
    {
        if (otherEvent == this) {
            return true;
        }

        if (super.matches(otherEvent)) {
            return getDeleteId() == null
                || Objects.equals(getDeleteId(), ((AbstractDocumentRestoreEvent) otherEvent).getDeleteId());
        }

        return false;
    }
}
