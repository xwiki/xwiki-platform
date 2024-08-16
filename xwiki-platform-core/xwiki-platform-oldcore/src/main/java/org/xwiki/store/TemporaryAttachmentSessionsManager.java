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
import java.util.List;
import java.util.Optional;

import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.component.annotation.Role;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.stability.Unstable;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import jakarta.servlet.http.Part;

/**
 * Interface for operations related to temporary upload of attachments. The idea of this API is to allow obtaining
 * directly a temporary {@link XWikiAttachment} from a {@link Part} and to keep it in cache until it's saved. The
 * manager is handling a separated map of attachments for each {@link jakarta.servlet.http.HttpSession}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
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
     * @throws TemporaryAttachmentException in case of problem when reading the part
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum filesize
     *             is reached)
     * @deprecated use {@link #uploadAttachment(DocumentReference, Part)} instead
     */
    @Deprecated(since = "42.0.0")
    default XWikiAttachment uploadAttachment(DocumentReference documentReference, javax.servlet.http.Part part)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadAttachment(documentReference, part, null);
    }

    /**
     * Temporary store the given {@link Part} to a cached {@link XWikiAttachment} attached to the given
     * {@link DocumentReference}.
     *
     * @param documentReference the reference of the document that the attachment should be attached to.
     * @param part the actual data that is uploaded.
     * @return an attachment that is not saved yet but cached and contains the data of the given part.
     * @throws TemporaryAttachmentException in case of problem when reading the part
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum filesize
     *             is reached)
     * @since 42.0.0
     */
    @Unstable
    default XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadAttachment(documentReference, part, null);
    }

    /**
     * Temporary store the given {@link Part} to a cached {@link XWikiAttachment} attached to the given
     * {@link DocumentReference}.
     *
     * @param documentReference the reference of the document that the attachment should be attached to
     * @param part the actual data that is uploaded
     * @param filename an optional filename used instead of using {@link Part#getSubmittedFileName()}, ignored when
     *            {@code null} or blank
     * @return an attachment that is not saved yet but cached and contains the data of the given part
     * @throws TemporaryAttachmentException in case of problem when reading the part
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum filesize
     *             is reached)
     * @since 14.9RC1
     * @deprecated use {@link #uploadAttachment(DocumentReference, Part, String)} instead
     */
    @Deprecated(since = "42.0.0")
    XWikiAttachment uploadAttachment(DocumentReference documentReference, javax.servlet.http.Part part, String filename)
        throws TemporaryAttachmentException, AttachmentValidationException;

    /**
     * Temporary store the given {@link Part} to a cached {@link XWikiAttachment} attached to the given
     * {@link DocumentReference}.
     *
     * @param documentReference the reference of the document that the attachment should be attached to
     * @param part the actual data that is uploaded
     * @param filename an optional filename used instead of using {@link Part#getSubmittedFileName()}, ignored when
     *            {@code null} or blank
     * @return an attachment that is not saved yet but cached and contains the data of the given part
     * @throws TemporaryAttachmentException in case of problem when reading the part
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum filesize
     *             is reached)
     * @since 42.0.0
     */
    @Unstable
    default XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part, String filename)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadAttachment(documentReference, JakartaServletBridge.toJavax(part), filename);
    }

    /**
     * Allow to temporarily attach the given instance of {@link XWikiAttachment} to the given document reference. This
     * can be useful if an API manipulates an {@link XWikiAttachment} without saving it, and want it to be discoverable
     * when parsing a document through the download action for example. Note that consumer of this API needs to be aware
     * that the file attached to the {@link XWikiAttachment} might be deleted at the end of the session, as any
     * temporary attachment.
     *
     * @param attachment the attachment to be temporarily attached to the document
     * @param documentReference the reference of the document to link this attachment to
     * @throws TemporaryAttachmentException in case of problem when performing the link
     * @since 14.10
     */
    void temporarilyAttach(XWikiAttachment attachment, DocumentReference documentReference)
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
     *         could be find.
     */
    boolean removeUploadedAttachment(DocumentReference documentReference, String filename);

    /**
     * Remove all uploaded attachments from the cache related to the given document reference in the current user
     * session.
     *
     * @param documentReference the reference for which to retrieve the temporary attachments.
     * @return {@code true} if there was some temporary attachments in cache for the given document reference in the
     *         current user session, {@code false} if there was no matching temporary attachment in cache.
     */
    boolean removeUploadedAttachments(DocumentReference documentReference);

    /**
     * This method aims at attaching the {@link XWikiAttachment} that might have been previously temporary upload to the
     * {@link XWikiDocument} they are targeting. This method should only be called before performing a save of the
     * document to actually persist them. Also note that this method cannot call
     * {@link #removeUploadedAttachment(DocumentReference, String)} as removing the attachment would delete the data
     * before they can be properly saved in the persistent storage. So the consumer of the API should take care of
     * properly calling this API when needed.
     *
     * @param document the actual document instance that should receive the attachments
     * @param fileNames the names of the uploaded files to attach
     * @since 14.10
     */
    default void attachTemporaryAttachmentsInDocument(XWikiDocument document, List<String> fileNames)
    {
    }
}
