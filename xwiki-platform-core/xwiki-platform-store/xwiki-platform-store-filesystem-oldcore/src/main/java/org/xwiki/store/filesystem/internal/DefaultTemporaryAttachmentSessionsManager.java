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

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import jakarta.inject.Inject;
import jakarta.inject.Provider;
import jakarta.inject.Singleton;
import jakarta.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.component.annotation.Component;
import org.xwiki.internal.attachment.XWikiAttachmentAccessWrapper;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;
import org.xwiki.store.blob.BlobPath;
import org.xwiki.store.blob.BlobStoreException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link TemporaryAttachmentSessionsManager}.
 * <p>
 * The temporary attachments are stored in the blob store {@value TemporaryAttachmentStore#STORE_NAME} (see
 * {@link TemporaryAttachmentStore}), under a scope that identifies the user and the browser (see
 * {@link TemporaryAttachmentScopeManager}), so that every cluster member can serve them and attach them when the
 * document is saved. They are deleted when the document is saved, when the edit is cancelled, at logout, and in any
 * case after {@code store.temporaryAttachments.maxAge} hours (see {@link TemporaryAttachmentsCleaner}).
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
public class DefaultTemporaryAttachmentSessionsManager implements TemporaryAttachmentSessionsManager
{
    /**
     * Minimum delay between two warnings about an unreachable store when reading, since every download of a browser
     * with pending uploads reads the store.
     */
    private static final long READ_WARNING_INTERVAL = 60_000L;

    private static final String NO_FILE_NAME = "A temporary attachment needs a file name.";

    private static final String STACK_TRACE = "Full stack trace: ";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<AttachmentValidator> attachmentValidator;

    @Inject
    private ContextualAuthorizationManager authorization;

    @Inject
    private TemporaryAttachmentScopeManager scopeManager;

    @Inject
    private TemporaryAttachmentStore store;

    @Inject
    private TemporaryAttachmentsConfiguration configuration;

    @Inject
    private Logger logger;

    private volatile long lastReadWarning;

    @Override
    public XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadAttachment(documentReference, part, null);
    }

    @Override
    public XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part, String filename)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        // The uploaded files are only attached by the users who can edit the document (save, comment, annotation).
        if (!this.authorization.hasAccess(Right.EDIT, documentReference)) {
            throw new TemporaryAttachmentException(
                "The current user is not allowed to edit [%s].".formatted(documentReference));
        }
        String actualFilename = StringUtils.isNotBlank(filename) ? filename : part.getSubmittedFileName();
        if (StringUtils.isBlank(actualFilename)) {
            throw new TemporaryAttachmentException(NO_FILE_NAME);
        }
        Optional<String> scopeId = this.scopeManager.getOrCreateScopeId();
        if (scopeId.isEmpty()) {
            throw new TemporaryAttachmentException("Cannot find a user http session.");
        }
        XWikiContext context = this.contextProvider.get();
        XWikiAttachment xWikiAttachment = new XWikiAttachment();
        try {
            xWikiAttachment.setFilename(actualFilename);
            try (InputStream inputStream = part.getInputStream()) {
                xWikiAttachment.setContent(inputStream);
            }
            xWikiAttachment.setAuthorReference(context.getUserReference());
            // Initialize an empty document with the right document reference and locale. We don't set the actual
            // document since it's a temporary attachment, but it is still useful to have a minimal knowledge of the
            // document it is stored for.
            xWikiAttachment.setDoc(new XWikiDocument(documentReference, documentReference.getLocale()), false);

            this.attachmentValidator.get()
                .validateAttachment(new XWikiAttachmentAccessWrapper(xWikiAttachment, context));

            return store(documentReference, xWikiAttachment, scopeId.get(), true);
        } catch (IOException e) {
            throw new TemporaryAttachmentException("Error while reading the content of a request part", e);
        } finally {
            // The content was copied to the blob store (or rejected): the local copy is not needed anymore.
            if (xWikiAttachment.getAttachment_content() != null) {
                xWikiAttachment.getAttachment_content().dispose();
            }
        }
    }

    @Override
    public void temporarilyAttach(XWikiAttachment attachment, DocumentReference documentReference)
        throws TemporaryAttachmentException
    {
        if (StringUtils.isBlank(attachment.getFilename())) {
            throw new TemporaryAttachmentException(NO_FILE_NAME);
        }
        Optional<String> scopeId = this.scopeManager.getOrCreateScopeId();
        if (scopeId.isPresent()) {
            store(documentReference, attachment, scopeId.get(), false);
        } else {
            this.logger.error("Cannot find a user http session to attach [{}] to [{}].", attachment.getFilename(),
                documentReference);
        }
    }

    @Override
    public Collection<XWikiAttachment> getUploadedAttachments(DocumentReference documentReference)
    {
        Optional<BlobPath> userPath = getUserPath();
        if (userPath.isEmpty()) {
            return Collections.emptyList();
        }

        BlobPath documentPath = this.store.getDocumentPath(userPath.get(), documentReference);
        try {
            List<XWikiAttachment> attachments = new ArrayList<>();
            for (BlobPath uploadPath : this.store.getNewestUploads(documentPath).values()) {
                toAttachment(documentReference, uploadPath).ifPresent(attachments::add);
            }
            return attachments;
        } catch (BlobStoreException | RuntimeException e) {
            logReadFailure("Failed to list the temporary attachments of [{}]: [{}]", documentReference, e);
            return Collections.emptyList();
        }
    }

    @Override
    public Optional<XWikiAttachment> getUploadedAttachment(DocumentReference documentReference, String filename)
    {
        Optional<BlobPath> userPath = getUserPath();
        if (userPath.isEmpty() || filename == null) {
            return Optional.empty();
        }

        BlobPath filePath =
            this.store.getFilePath(this.store.getDocumentPath(userPath.get(), documentReference), filename);
        try {
            BlobPath uploadPath = this.store.getNewestUploads(filePath).get(filePath);

            return uploadPath != null ? toAttachment(documentReference, uploadPath) : Optional.empty();
        } catch (BlobStoreException | RuntimeException e) {
            logReadFailure("Failed to get a temporary attachment of [{}]: [{}]", documentReference, e);
            return Optional.empty();
        }
    }

    @Override
    public boolean removeUploadedAttachment(DocumentReference documentReference, String filename)
    {
        if (!this.scopeManager.isAvailable()) {
            this.logger.warn("Cannot find a user http session to remove attachment [{}] from [{}].", filename,
                documentReference);
            return false;
        }

        Optional<String> scopeId = this.scopeManager.getScopeId();
        if (scopeId.isPresent() && filename != null) {
            BlobPath userPath = getUserPath(scopeId.get());
            return delete(scopeId.get(),
                this.store.getFilePath(this.store.getDocumentPath(userPath, documentReference), filename),
                documentReference);
        }

        return false;
    }

    @Override
    public boolean removeUploadedAttachments(DocumentReference documentReference)
    {
        if (!this.scopeManager.isAvailable()) {
            this.logger.warn("Cannot find a user http session to remove attachments from [{}].", documentReference);
            return false;
        }

        Optional<String> scopeId = this.scopeManager.getScopeId();
        if (scopeId.isPresent()) {
            return delete(scopeId.get(), this.store.getDocumentPath(getUserPath(scopeId.get()), documentReference),
                documentReference);
        }

        return false;
    }

    /**
     * {@inheritDoc}
     * <p>
     * A name that cannot be found (expired, already saved, uploaded in another browser) is logged as a warning and
     * skipped. An upload that is found but cannot be read fails the call with an unchecked exception: the save fails
     * instead of succeeding without the file, whose upload is deleted after a save.
     */
    @Override
    public void attachTemporaryAttachmentsInDocument(XWikiDocument document, List<String> fileNames)
    {
        for (String temporaryUploadedFile : fileNames) {
            if (StringUtils.isNotBlank(temporaryUploadedFile)) {
                Optional<XWikiAttachment> uploadedAttachment =
                    getUploadedAttachment(document.getDocumentReference(), temporaryUploadedFile);
                if (uploadedAttachment.isPresent()) {
                    TemporaryAttachments.attach(document, uploadedAttachment.get());
                } else {
                    this.logger.warn("The temporary attachment [{}] of [{}] cannot be found (expired, already saved or"
                        + " uploaded in another browser): it is not attached to the document.", temporaryUploadedFile,
                        document.getDocumentReference());
                }
            }
        }
    }

    private Optional<BlobPath> getUserPath()
    {
        return this.scopeManager.getScopeId().map(this::getUserPath);
    }

    private BlobPath getUserPath(String scopeId)
    {
        return this.store.getUserPath(scopeId, this.contextProvider.get().getUserReference());
    }

    /**
     * Deletes the given directory, then forgets the scope of the browser when it has no pending upload any more, so
     * that the next downloads of the browser do not look into the store.
     */
    private boolean delete(String scopeId, BlobPath path, DocumentReference documentReference)
    {
        try {
            boolean deleted = this.store.delete(path);
            if (!this.store.exists(this.store.getScopePath(scopeId))) {
                this.scopeManager.resetScope();
            }
            return deleted;
        } catch (BlobStoreException | RuntimeException e) {
            this.logger.warn("Failed to remove the temporary attachments of [{}]: [{}]", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
            this.logger.debug(STACK_TRACE, e);
            return false;
        }
    }

    private XWikiAttachment store(DocumentReference documentReference, XWikiAttachment source, String scopeId,
        boolean upload) throws TemporaryAttachmentException
    {
        XWikiContext context = this.contextProvider.get();
        String filename = source.getFilename();
        try {
            BlobPath userPath = this.store.getUserPath(scopeId, context.getUserReference());
            BlobPath filePath =
                this.store.getFilePath(this.store.getDocumentPath(userPath, documentReference), filename);
            long size = source.getContentLongSize(context);
            if (upload) {
                checkLimits(userPath, filePath, size, documentReference);
            }

            TemporaryAttachmentStore.Metadata metadata =
                new TemporaryAttachmentStore.Metadata(filename, source.getAuthorReference(), size);
            long uploadTime = this.store.getNextUploadTime(filePath, System.currentTimeMillis());
            BlobPath uploadPath;
            try (InputStream content = source.getContentInputStream(context)) {
                uploadPath = this.store.write(filePath, uploadTime, content, documentReference, metadata, upload);
            }
            this.store.deleteOlderUploads(filePath, uploadPath);

            return TemporaryAttachments.toAttachment(documentReference, uploadPath, metadata,
                this.store.getContent(uploadPath));
        } catch (BlobStoreException | XWikiException | IOException | RuntimeException e) {
            throw new TemporaryAttachmentException(
                "Failed to store the temporary attachment [%s] of [%s]".formatted(filename, documentReference), e);
        }
    }

    private void checkLimits(BlobPath userPath, BlobPath filePath, long size, DocumentReference documentReference)
        throws BlobStoreException, TemporaryAttachmentException
    {
        int maxCount = this.configuration.getMaxCount();
        long maxTotalSize = this.configuration.getMaxTotalSize();
        if (maxCount < 0 && maxTotalSize < 0) {
            return;
        }

        TemporaryAttachmentStore.Usage usage = this.store.getUsage(userPath, filePath);
        if (maxCount >= 0 && usage.count() >= maxCount) {
            this.logger.warn("Temporary upload to [{}] rejected: the user already has [{}] pending temporary"
                + " attachments in this browser (store.temporaryAttachments.maxCount = [{}]).", documentReference,
                usage.count(), maxCount);
            throw new TemporaryAttachmentException("Too many pending temporary attachments, save or cancel the"
                + " current edits first.");
        }
        if (maxTotalSize >= 0 && usage.size() + size > maxTotalSize) {
            this.logger.warn("Temporary upload to [{}] rejected: [{}] bytes pending plus [{}] bytes exceed"
                + " store.temporaryAttachments.maxTotalSize = [{}].", documentReference, usage.size(), size,
                maxTotalSize);
            throw new TemporaryAttachmentException("The pending temporary attachments are too big, save or cancel"
                + " the current edits first.");
        }
    }

    private Optional<XWikiAttachment> toAttachment(DocumentReference documentReference, BlobPath uploadPath)
    {
        try {
            return Optional.of(TemporaryAttachments.toAttachment(documentReference, uploadPath,
                this.store.readMetadata(uploadPath), this.store.getContent(uploadPath)));
        } catch (BlobStoreException e) {
            // Probably deleted by another request between the listing and the read.
            this.logger.debug("Failed to read a temporary attachment of [{}]: [{}]", documentReference,
                ExceptionUtils.getRootCauseMessage(e));
            return Optional.empty();
        }
    }

    /**
     * Logs a read failure as a warning at most once per minute, and at debug level otherwise, since every download of
     * a browser with pending uploads reads the store.
     */
    private void logReadFailure(String message, DocumentReference documentReference, Exception e)
    {
        long now = System.currentTimeMillis();
        if (now - this.lastReadWarning >= READ_WARNING_INTERVAL) {
            this.lastReadWarning = now;
            this.logger.warn(message, documentReference, ExceptionUtils.getRootCauseMessage(e));
        } else {
            this.logger.debug(message, documentReference, ExceptionUtils.getRootCauseMessage(e));
        }
        this.logger.debug(STACK_TRACE, e);
    }
}
