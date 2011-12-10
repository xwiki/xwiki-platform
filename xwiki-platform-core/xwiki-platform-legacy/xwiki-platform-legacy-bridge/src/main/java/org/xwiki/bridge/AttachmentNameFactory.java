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
package org.xwiki.bridge;

import org.xwiki.component.annotation.ComponentRole;

/**
 * Generate an Attachment Name from a raw string reference. The exact format of the reference depends on the 
 * implementation used. For example see {@link org.xwiki.bridge.internal.DefaultAttachmentNameFactory}.
 *
 * @version $Id$
 * @since 2.0RC1
 * @deprecated use {@link org.xwiki.model.reference.AttachmentReferenceResolver} instead since 2.2M1
 */
@ComponentRole
@Deprecated
public interface AttachmentNameFactory
{
    /**
     * @param reference the attachment's name as a string using a textual format 
     *        (eg {@code wiki:space.page@attachment}). The supported format is up to implementers of this method.
     * @return the object representing an attachment reference
     */
    AttachmentName createAttachmentName(String reference);    
}
