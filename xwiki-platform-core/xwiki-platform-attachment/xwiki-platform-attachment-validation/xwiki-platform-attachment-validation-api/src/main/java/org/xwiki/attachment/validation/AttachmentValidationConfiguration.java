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

import java.util.List;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.EntityReference;

/**
 * Configuration values for the attachment validation.
 *
 * @version $Id$
 * @since 14.10
 */
@Role
public interface AttachmentValidationConfiguration
{
    /**
     * @return the list of allowed attachment mimetypes of the current document. A joker (@code '*') can be used to
     *     match any media (e.g., "image/png", "text/*")
     */
    List<String> getAllowedMimetypes();

    /**
     * @param documentReference the reference of a document
     * @return the list of allowed attachment mimetypes of the provided document. A joker (@code '*') can be used to
     *     match any media (e.g., * "image/png", "text/*")
     * @since 14.10.2
     * @since 15.0RC1
     */
    List<String> getAllowedMimetypes(DocumentReference documentReference);

    /**
     * @return the list of blocker attachment mimetype of the current document. A joker (@code '*') can be used to match
     *     any media (e.g., "image/png", "text/*")
     */
    List<String> getBlockerMimetypes();

    /**
     * @param documentReference the reference of a document
     * @return the list of blocker attachment mimetypes of the provided document. A joker (@code '*') can be used to
     *     match any media (e.g., * "image/png", "text/*")
     * @since 14.10.2
     * @since 15.0RC1
     */
    List<String> getBlockerMimetypes(DocumentReference documentReference);

    /**
     * @param entityReference the entity reference to use as the context when looking for the configuration
     * @return the max allowed file size in bytes
     * @since 15.5RC1
     * @since 14.10.13
     */
    long getMaxUploadSize(EntityReference entityReference);
}
