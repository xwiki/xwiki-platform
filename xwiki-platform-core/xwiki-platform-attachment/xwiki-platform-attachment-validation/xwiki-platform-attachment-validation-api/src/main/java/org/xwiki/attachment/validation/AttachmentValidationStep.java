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
 * One attachment validation step. {@link AttachmentValidator} calls them one after the other and fails whenever a step
 * fails.
 *
 * @version $Id$
 * @since 14.10
 */
@Role
public interface AttachmentValidationStep
{
    /**
     * Validate a single aspect of the attachment.
     *
     * @param wrapper the attachment wrapper
     * @throws AttachmentValidationException in case of validation error
     */
    void validate(AttachmentAccessWrapper wrapper) throws AttachmentValidationException;
}
