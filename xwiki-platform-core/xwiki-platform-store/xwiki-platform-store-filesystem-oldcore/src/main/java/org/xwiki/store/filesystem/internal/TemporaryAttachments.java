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
package org.xwiki.store.filesystem.internal;

import java.io.InputStream;
import java.util.Date;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.UnexpectedException;
import org.xwiki.store.blob.Blob;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.legacy.doc.internal.FilesystemAttachmentContent;

import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Conversions between the uploads of {@link TemporaryAttachmentStore} and {@link XWikiAttachment} instances.
 *
 * @version $Id$
 * @since 18.10.0RC1
 */
public final class TemporaryAttachments
{
    private TemporaryAttachments()
    {
        // Utility class.
    }

    /**
     * Creates the attachment returned for an upload: its content is read from the blob store.
     *
     * @param documentReference the document the upload is for
     * @param uploadPath the upload directory
     * @param metadata the metadata of the upload
     * @param content the blob holding the content of the upload
     * @return the attachment
     */
    public static XWikiAttachment toAttachment(DocumentReference documentReference, BlobPath uploadPath,
        TemporaryAttachmentStore.Metadata metadata, Blob content)
    {
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.setFilename(metadata.filename());
        attachment.setAuthorReference(metadata.author());
        attachment.setDate(new Date(TemporaryAttachmentStore.getUploadTime(uploadPath).orElse(0)));
        attachment.setLongSize(metadata.size());
        // A minimal document with the right reference and locale, as for the upload.
        attachment.setDoc(new XWikiDocument(documentReference, documentReference.getLocale()), false);

        // The content is marked dirty, as the content of a new upload, so that a caller saving the attachment as is
        // saves its content too.
        FilesystemAttachmentContent attachmentContent = new FilesystemAttachmentContent(content);
        attachmentContent.setContentDirty(true);
        attachment.setAttachment_content(attachmentContent);

        return attachment;
    }

    /**
     * Copies the content of a temporary attachment into a regular local attachment content and sets it in the
     * document, so that the attachment store saves it as any other attachment and so that the document does not depend
     * on a blob that is deleted right after the save.
     *
     * @param document the document to attach to
     * @param uploadedAttachment the temporary attachment
     * @throws UnexpectedException if the content of the temporary attachment cannot be read: the save must fail rather
     *     than lose the file
     */
    public static void attach(XWikiDocument document, XWikiAttachment uploadedAttachment)
    {
        XWikiAttachment attachment = new XWikiAttachment();
        attachment.setFilename(uploadedAttachment.getFilename());
        attachment.setAuthorReference(uploadedAttachment.getAuthorReference());
        attachment.setDate(uploadedAttachment.getDate());
        try (InputStream stream = uploadedAttachment.getAttachment_content().getContentInputStream()) {
            attachment.setContent(stream);
        } catch (Exception e) {
            throw new UnexpectedException("Failed to read the temporary attachment [%s] of [%s]"
                .formatted(uploadedAttachment.getFilename(), document.getDocumentReference()), e);
        }

        XWikiAttachment previousAttachment = document.setAttachment(attachment);
        if (previousAttachment != null) {
            attachment.setVersion(previousAttachment.getNextVersion());
        }
    }
}
