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
package org.xwiki.attachment.refactoring.event;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.refactoring.internal.event.AbstractEntityCopyOrRenameEvent;

/**
 * Event fired after an attachment is moved.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>source: the job which produced this event or null for a remote event</li>
 * <li>data: the request of the job which  produced this event</li>
 * </ul>
 *
 * @version $Id$
 * @since 14.2RC1
 */
public class AttachmentMovedEvent extends AbstractEntityCopyOrRenameEvent<AttachmentReference>
{
    /**
     * Default constructor, used by listeners.
     */
    public AttachmentMovedEvent()
    {
    }

    /**
     * Creates a new instance with two attachment references.
     *
     * @param sourceReference the reference of the source attachment
     * @param targetReference the reference of the target attachment
     */
    public AttachmentMovedEvent(AttachmentReference sourceReference,
        AttachmentReference targetReference)
    {
        super(sourceReference, targetReference);
    }
}
