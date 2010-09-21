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
package org.xwiki.officepreview;

import org.xwiki.component.annotation.ComponentRole;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.rendering.block.XDOM;

/**
 * Interface for the component responsible for building previews of office attachments.
 * 
 * @since 2.5M2
 * @version $Id$
 */
@ComponentRole
public interface OfficePreviewBuilder
{
    /**
     * Builds a preview {@link XDOM} of the specified office attachment.
     * 
     * @param attachmentReference office attachment reference
     * @param filterStyles whether office document styles should be filtered
     * @return {@link XDOM} containing a preview of the specified attachment
     * @throws Exception if an error occurs while accessing the attachment or building the preview
     */
    XDOM build(AttachmentReference attachmentReference, boolean filterStyles) throws Exception;
}
