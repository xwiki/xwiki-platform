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
package org.xwiki.model.reference;

import org.xwiki.model.EntityType;

/**
 * Represents a reference to an Attachment (document reference and file name). Note that an attachment is always
 * attached to a document.
 * 
 * @version $Id$
 * @since 2.2M1
 */
public class AttachmentReference extends EntityReference
{
    public AttachmentReference(EntityReference reference)
    {
        super(reference.getName(), EntityType.ATTACHMENT, reference.getParent());
    }

    public AttachmentReference(String fileName, DocumentReference parent)
    {
        super(fileName, EntityType.ATTACHMENT, parent);
    }

    public DocumentReference getDocumentReference()
    {
        return new DocumentReference(extractReference(EntityType.DOCUMENT));
    }
}
