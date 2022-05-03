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
package org.xwiki.store;

import java.util.Collection;
import java.util.Optional;

import javax.servlet.http.Part;

import org.xwiki.component.annotation.Role;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiAttachment;

/**
 * Interface for operations related to temporary upload of attachments.
 * The idea of this API is to allow obtaining directly a temporary {@link XWikiAttachment} from a {@link Part} and to
 * keep it in cache until it's saved.
 * The manager is handling a separated map of attachments for each {@link javax.servlet.http.HttpSession}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Unstable
@Role
public interface TemporaryAttachmentSessionsManager
{
    /**
     * Temporary store the given {@link Part} to a cached {@link XWikiAttachment} attached to the given
     * {@link DocumentReference}.
     *
     * @param documentReference the reference of the document that the attachment should be attached to.
     * @param part the actual data that is uploaded.
     * @return an attachment that is not saved yet but cached and contains the data of the given part.
     * @throws TemporaryAttachmentException if the part size exceeds the maximum upload size, or in case of problem
     *                                      when reading the part.
     */
    XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part)
        throws TemporaryAttachmentException;

    /**
     * Retrieve all temporary attachments related to the given document reference in the current user session.
     *
     * @param documentReference the reference for which to retrieve temporary attachments.
     * @return a collection of temporary attachments or an empty collection.
     */
    Collection<XWikiAttachment> getUploadedAttachments(DocumentReference documentReference);

    /**
     * Retrieve a specific temporary attachment related to the given document reference and matching the given filename.
     *
     * @param documentReference the reference for which to retrieve the temporary attachment.
     * @param filename the filename to look for.
     * @return an {@link Optional#empty()} if the attachment cannot be found, else an optional containing the attachment
     */
    Optional<XWikiAttachment> getUploadedAttachment(DocumentReference documentReference, String filename);

    /**
     * Retrieve a specific temporary attachment related to the given document reference and matching the given filename.
     * This method is only a helper to {@link #getUploadedAttachment(DocumentReference, String)}.
     *
     * @param attachmentReference the reference of the attachment to retrieve
     * @return an {@link Optional#empty()} if the attachment cannot be found, else an optional containing the attachment
     * @since 14.3.1
     * @since 14.4RC1
     */
    default Optional<XWikiAttachment> getUploadedAttachment(AttachmentReference attachmentReference)
    {
        return getUploadedAttachment(attachmentReference.getDocumentReference(), attachmentReference.getName());
    }

    /**
     * Search for temporary attachment related to the given document reference and matching the given filename, and
     * remove it from the cache.
     *
     * @param documentReference the reference for which to retrieve the temporary attachment.
     * @param filename the filename to look for.
     * @return {@code true} if the attachment have been found for deletion, {@code false} if no matching attachment
     *          could be find.
     */
    boolean removeUploadedAttachment(DocumentReference documentReference, String filename);

    /**
     * Remove all uploaded attachments from the cache related to the given document reference in the current user
     * session.
     *
     * @param documentReference the reference for which to retrieve the temporary attachments.
     * @return {@code true} if there was some temporary attachments in cache for the given document reference in the
     *          current user session, {@code false} if there was no matching temporary attachment in cache.
     */
    boolean removeUploadedAttachments(DocumentReference documentReference);
}
