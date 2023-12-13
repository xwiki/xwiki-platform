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
package org.xwiki.attachment.validation;

import org.xwiki.attachment.AttachmentAccessWrapper;
import org.xwiki.component.annotation.Role;

/**
 * Provide the operations to validate an attachment. For instance, by checking the size or the mimetype of the
 * attachment.
 *
 * @version $Id$
 * @since 14.10
 */
@Role
public interface AttachmentValidator
{
    /**
     * Check if the part is a valid attachment in the current space.
     *
     * @param wrapper the attachment wrapper, containing the actual uploaded file input stream, and its required
     *     metadatas
     * @throws AttachmentValidationException in case of error when validating the part
     */
    void validateAttachment(AttachmentAccessWrapper wrapper) throws AttachmentValidationException;
}
