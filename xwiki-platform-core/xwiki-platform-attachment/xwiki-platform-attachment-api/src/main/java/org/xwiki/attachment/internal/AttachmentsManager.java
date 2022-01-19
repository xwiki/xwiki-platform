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
package org.xwiki.attachment.internal;

import java.util.Optional;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Provide operations to inspect and manipulate attachments.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Role
public interface AttachmentsManager
{
    /**
     * Check if an attachment exists.
     *
     * @param attachmentLocation the reference of the attachment to check
     * @return {@code true} if the attachment is found at the requested location, {@code false} otherwise
     * @throws XWikiException if the attachments couldn't be retrieved
     */
    boolean available(AttachmentReference attachmentLocation) throws XWikiException;

    /**
     * Return the reference of the new location of the attachment.
     *
     * @param attachmentReference the reference of the request attachment
     * @return the reference of the new location of the attachment if it exists, {@code Optional#empty} otherwise
     * @throws XWikiException in case of error when accessing the document
     */
    Optional<AttachmentReference> getRedirection(AttachmentReference attachmentReference) throws XWikiException;

    /**
     * Remove a redirection if it exists.
     *
     * @param attachmentName the name of the redirect attachment
     * @param targetDocument the document containing the redirection
     * @return {@code true} if the redirection was removed, {@code false} otherwise
     */
    boolean removeExistingRedirection(String attachmentName, XWikiDocument targetDocument);
}
