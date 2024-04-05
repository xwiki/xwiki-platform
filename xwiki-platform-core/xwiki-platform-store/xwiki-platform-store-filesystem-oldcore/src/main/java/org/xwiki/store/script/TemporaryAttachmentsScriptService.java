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
package org.xwiki.store.script;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.store.filesystem.StoreFilesystemOldcoreException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

import static org.apache.commons.lang3.exception.ExceptionUtils.getRootCauseMessage;

/**
 * Script service dedicated to the handling of temporary attachments.
 *
 * @version $Id$
 * @see TemporaryAttachmentSessionsManager
 * @since 14.3RC1
 */
@Component
@Singleton
@Named("temporaryAttachments")
public class TemporaryAttachmentsScriptService implements ScriptService
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private TemporaryAttachmentSessionsManager temporaryAttachmentSessionsManager;

    @Inject
    private Logger logger;

    /**
     * Temporary upload the attachment identified by the given field name: the request should be of type
     * {@code multipart/form-data}.
     *
     * @param documentReference the target document reference the attachment should be later attached to
     * @param fieldName the name of the field of the uploaded data
     * @return a temporary {@link Attachment} not yet persisted attachment, or {@code null} in case of error
     * @throws TemporaryAttachmentException in case of problem when reading the attachment
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     */
    public Attachment uploadTemporaryAttachment(DocumentReference documentReference, String fieldName)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadTemporaryAttachment(documentReference, fieldName, null);
    }

    /**
     * Temporary upload the attachment identified by the given field name: the request should be of type
     * {@code multipart/form-data}.
     *
     * @param documentReference the target document reference the attachment should be later attached to
     * @param fieldName the name of the field of the uploaded data
     * @param filename an optional filename used instead of using the filename of the file passing in
     *     {@code fieldName}, ignored when {@code null}
     * @return a temporary {@link Attachment} not yet persisted attachment, or {@code null} in case of error
     * @throws TemporaryAttachmentException in case of problem when reading the attachment
     * @throws AttachmentValidationException in case of error when validating the attachment (e.g., the maximum
     *     filesize is reached)
     * @since 14.9RC1
     */
    public Attachment uploadTemporaryAttachment(DocumentReference documentReference, String fieldName, String filename)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        XWikiContext context = this.contextProvider.get();

        try {
            Part part = context.getRequest().getPart(fieldName);
            if (part != null) {
                XWikiAttachment attachment =
                    this.temporaryAttachmentSessionsManager.uploadAttachment(documentReference, part, filename);
                Document document = Optional.ofNullable(attachment.getDoc())
                    .map(doc -> doc.newDocument(context))
                    .orElse(null);
                return new Attachment(document, attachment, context);
            } else {
                return null;
            }
        } catch (IOException | ServletException e) {
            this.logger.warn("Error while reading the request content part: [{}]", getRootCauseMessage(e));
            return null;
        }
    }

    /**
     * Return a list of the temporary attachments, sorted by filenames (ignoring the case).
     *
     * @param documentReference the target document reference the attachments should be later attached to
     * @return the list of temporary attachments linked to the given document reference. The list is sorted by the
     *     attachments filenames ({@link XWikiAttachment#getFilename()})
     * @since 14.9RC1
     */
    public List<Attachment> listTemporaryAttachments(DocumentReference documentReference)
        throws StoreFilesystemOldcoreException
    {
        XWikiDocument document = getDocument(documentReference);
        return this.temporaryAttachmentSessionsManager.getUploadedAttachments(documentReference)
            .stream()
            .map(attachment -> convertToAttachment(document, attachment))
            .sorted(getLowerCaseStringComparator())
            .collect(Collectors.toList());
    }

    /**
     * Build a list of all the attachments of a given document, sorted by filenames (ignoring the case). The list
     * contains the persisted attachments of the document, merged with the temporary attachments. The persisted
     * attachments are replaced by the temporary one if their names match.
     *
     * @param documentReference the target document reference the temporary attachments should be later attached to
     * @return the list of all attachments linked to the given document reference. Persisted attachments are overridden
     *     by temporary one if names match. The list is sorted by the attachments filenames
     *     ({@link XWikiAttachment#getFilename()})
     * @since 14.9RC1
     */
    public List<Attachment> listAllAttachments(DocumentReference documentReference)
        throws StoreFilesystemOldcoreException
    {
        Collection<XWikiAttachment> temporaryAttachments =
            new ArrayList<>(this.temporaryAttachmentSessionsManager.getUploadedAttachments(documentReference));
        XWikiDocument document = getDocument(documentReference);
        List<Attachment> fullList = temporaryAttachments.stream()
            .map(attachment -> convertToAttachment(document, attachment))
            .collect(Collectors.toList());
        Stream<Attachment> nonOverriddenAttachments =
            document.getAttachmentList()
                .stream()
                .map(xWikiAttachment -> convertToAttachment(document, xWikiAttachment))
                .filter(persistedAttachment -> temporaryAttachments.stream()
                    .map(xWikiAttachment -> convertToAttachment(document, xWikiAttachment))
                    .noneMatch(attachmentEqualityPredicate(persistedAttachment)));
        fullList.addAll(nonOverriddenAttachments.collect(Collectors.toList()));

        fullList.sort(getLowerCaseStringComparator());
        return fullList;
    }

    /**
     * Check if a given attachment is found in the temporary attachment session.
     * {@link #persistentAttachmentExists(Attachment)} exists as well to check if a given attachment can be found in the
     * persisted attachments. Note that both method can return {@code true} for the same {@link XWikiAttachment} when an
     * attachment is overridden in the temporary attachment session.
     *
     * @param attachment an attachment
     * @return {@code true} if a matching attachment exists in the temporary attachment session (i.e., same filename and
     *     document reference), {@code false} otherwise
     * @see #persistentAttachmentExists(Attachment)
     * @since 14.9RC1
     */
    public boolean temporaryAttachmentExists(Attachment attachment)
    {
        return this.temporaryAttachmentSessionsManager.getUploadedAttachment(attachment.getReference()).isPresent();
    }

    /**
     * Check if a given attachment is persisted. {@link #temporaryAttachmentExists(Attachment)} exists as well to check
     * if a given attachment can be found in the temporary attachment session. Note that both method can return
     * {@code true} at for the same {@link XWikiAttachment} when an attachment is overridden in the temporary attachment
     * session.
     *
     * @param attachment an attachment
     * @return {@code true} if a matching persisted attachment exists (i.e., same filename and document reference),
     *     {@code false} otherwise
     * @throws StoreFilesystemOldcoreException in case of error when accessing the attachment's document
     * @see #temporaryAttachmentExists(Attachment)
     * @since 14.9RC1
     */
    public boolean persistentAttachmentExists(Attachment attachment)
        throws StoreFilesystemOldcoreException
    {
        XWikiDocument document = getDocument(attachment.getReference().getDocumentReference());
        return document.getAttachment(attachment.getFilename()) != null;
    }

    /**
     * Define the equality condition between two attachments. Two attachments are considered equals if their
     * {@link XWikiAttachment#getFilename()} match.
     *
     * @param attachment0 the attachment used to build the predicate
     * @return a predicate to compare other attachments against {@code attachment0}
     */
    private Predicate<Attachment> attachmentEqualityPredicate(Attachment attachment0)
    {
        String attachment0Filename = attachment0.getFilename();
        return attachment1 -> Objects.equals(attachment1.getFilename(), attachment0Filename);
    }

    private XWikiDocument getDocument(DocumentReference documentReference) throws StoreFilesystemOldcoreException
    {
        try {
            XWikiContext context = this.contextProvider.get();
            return context.getWiki().getDocument(documentReference, context);
        } catch (XWikiException e) {
            throw new StoreFilesystemOldcoreException(String.format("Failed to load document [%s]", documentReference),
                e);
        }
    }

    private Attachment convertToAttachment(XWikiDocument document, XWikiAttachment attachment)
    {
        if (attachment == null) {
            return null;
        }
        return new Attachment(new Document(document, this.contextProvider.get()), attachment,
            this.contextProvider.get());
    }

    private static Comparator<Attachment> getLowerCaseStringComparator()
    {
        return Comparator.comparing(attachment -> Objects.toString(attachment.getFilename(), "").toLowerCase());
    }
}
