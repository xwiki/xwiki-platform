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

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.BeginFoldEvent;
import org.xwiki.observation.event.CancelableEvent;
import org.xwiki.refactoring.internal.event.AbstractEntityCopyOrRenameEvent;
import org.xwiki.refactoring.job.CopyRequest;

/**
 * Event fired when a document is about to be copied.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the job which produced this event or null for a remote event</li>
 * <li>data: the {@link CopyRequest} request of the job which  produced this event</li>
 * </ul>
 * 
 * @version $Id$
 * @since 11.1RC1
 */
public class DocumentCopyingEvent extends AbstractEntityCopyOrRenameEvent<DocumentReference>
    implements BeginFoldEvent, CancelableEvent
{
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor, used by listeners.
     */
    public DocumentCopyingEvent()
    {
    }

    /**
     * Creates a new instance with the given data.
     * 
     * @param sourceReference the reference of the source entity
     * @param targetReference the reference of the target entity
     */
    public DocumentCopyingEvent(DocumentReference sourceReference, DocumentReference targetReference)
    {
        super(sourceReference, targetReference);
    }
}
