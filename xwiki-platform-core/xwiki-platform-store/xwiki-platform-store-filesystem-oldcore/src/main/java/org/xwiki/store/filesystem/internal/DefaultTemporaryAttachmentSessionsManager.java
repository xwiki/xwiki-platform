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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import jakarta.servlet.http.HttpSession;
import jakarta.servlet.http.Part;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.xwiki.attachment.validation.AttachmentValidationException;
import org.xwiki.attachment.validation.AttachmentValidator;
import org.xwiki.component.annotation.Component;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletSession;
import org.xwiki.internal.attachment.XWikiAttachmentAccessWrapper;
import org.xwiki.jakartabridge.servlet.JakartaServletBridge;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Default implementation of {@link TemporaryAttachmentSessionsManager}.
 *
 * @version $Id$
 * @since 14.3RC1
 */
@Component
@Singleton
public class DefaultTemporaryAttachmentSessionsManager implements TemporaryAttachmentSessionsManager
{
    private static final String ATTRIBUTE_KEY = "xwikiTemporaryAttachments";

    @Inject
    private Provider<XWikiContext> contextProvider;

    @Inject
    private Provider<AttachmentValidator> attachmentValidator;

    @Inject
    private Provider<Container> container;

    @Inject
    private Logger logger;

    private HttpSession getSession()
    {
        return ((ServletSession) this.container.get().getSession()).getSession();
    }

    private Optional<TemporaryAttachmentSession> getOrCreateSession()
    {
        Optional<TemporaryAttachmentSession> result = Optional.empty();
        HttpSession session = this.getSession();
        if (session != null) {
            TemporaryAttachmentSession temporaryAttachmentSession =
                (TemporaryAttachmentSession) session.getAttribute(ATTRIBUTE_KEY);
            if (temporaryAttachmentSession == null) {
                temporaryAttachmentSession = new TemporaryAttachmentSession(session.getId());
                session.setAttribute(ATTRIBUTE_KEY, temporaryAttachmentSession);
            }
            result = Optional.of(temporaryAttachmentSession);
        }
        return result;
    }

    @Override
    public XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadAttachment(documentReference, part, null);
    }

    @Override
    @Deprecated
    public XWikiAttachment uploadAttachment(DocumentReference documentReference, javax.servlet.http.Part part,
        String filename) throws TemporaryAttachmentException, AttachmentValidationException
    {
        return uploadAttachment(documentReference, JakartaServletBridge.toJakarta(part), filename);
    }

    @Override
    public XWikiAttachment uploadAttachment(DocumentReference documentReference, Part part, String filename)
        throws TemporaryAttachmentException, AttachmentValidationException
    {
        Optional<TemporaryAttachmentSession> optionalSession = getOrCreateSession();
        if (optionalSession.isEmpty()) {
            throw new TemporaryAttachmentException("Cannot find a user http session.");
        }
        TemporaryAttachmentSession temporaryAttachmentSession = optionalSession.get();
        XWikiContext context = this.contextProvider.get();
        try {
            XWikiAttachment xWikiAttachment = new XWikiAttachment();
            String actualFilename;
            if (StringUtils.isNotBlank(filename)) {
                actualFilename = filename;
            } else {
                actualFilename = part.getSubmittedFileName();
            }
            xWikiAttachment.setFilename(actualFilename);
            xWikiAttachment.setContent(part.getInputStream());
            xWikiAttachment.setAuthorReference(context.getUserReference());
            // Initialize an empty document with the right document reference and locale. We don't set the actual
            // document since it's a temporary attachment, but it is still useful to have a minimal knowledge of the
            // document it is stored for.
            xWikiAttachment.setDoc(new XWikiDocument(documentReference, documentReference.getLocale()), false);

            this.attachmentValidator.get()
                .validateAttachment(new XWikiAttachmentAccessWrapper(xWikiAttachment, context));
            temporaryAttachmentSession.addAttachment(documentReference, xWikiAttachment);
            return xWikiAttachment;
        } catch (IOException e) {
            throw new TemporaryAttachmentException("Error while reading the content of a request part", e);
        }
    }

    @Override
    public void temporarilyAttach(XWikiAttachment attachment, DocumentReference documentReference)
    {
        Optional<TemporaryAttachmentSession> optionalSession = getOrCreateSession();
        if (optionalSession.isPresent()) {
            TemporaryAttachmentSession temporaryAttachmentSession = optionalSession.get();
            temporaryAttachmentSession.addAttachment(documentReference, attachment);
        } else {
            this.logger.error("Cannot find a user http session to attach [{}] to [{}].", attachment, documentReference);
        }
    }

    @Override
    public Collection<XWikiAttachment> getUploadedAttachments(DocumentReference documentReference)
    {
        Optional<TemporaryAttachmentSession> optionalSession = getOrCreateSession();
        if (optionalSession.isEmpty()) {
            return Collections.emptyList();
        } else {
            TemporaryAttachmentSession temporaryAttachmentSession = optionalSession.get();
            return temporaryAttachmentSession.getAttachments(documentReference);
        }
    }

    @Override
    public Optional<XWikiAttachment> getUploadedAttachment(DocumentReference documentReference, String filename)
    {
        Optional<TemporaryAttachmentSession> optionalSession = getOrCreateSession();
        if (optionalSession.isPresent()) {
            TemporaryAttachmentSession temporaryAttachmentSession = optionalSession.get();
            return temporaryAttachmentSession.getAttachment(documentReference, filename);
        } else {
            return Optional.empty();
        }
    }

    @Override
    public boolean removeUploadedAttachment(DocumentReference documentReference, String filename)
    {
        Optional<TemporaryAttachmentSession> optionalSession = getOrCreateSession();
        if (optionalSession.isPresent()) {
            TemporaryAttachmentSession temporaryAttachmentSession = optionalSession.get();
            return temporaryAttachmentSession.removeAttachment(documentReference, filename);
        } else {
            this.logger.warn("Cannot find a user http session to remove attachment [{}] from [{}].", filename,
                documentReference);
            return false;
        }
    }

    @Override
    public boolean removeUploadedAttachments(DocumentReference documentReference)
    {
        Optional<TemporaryAttachmentSession> optionalSession = getOrCreateSession();
        if (optionalSession.isPresent()) {
            TemporaryAttachmentSession temporaryAttachmentSession = optionalSession.get();
            return temporaryAttachmentSession.removeAttachments(documentReference);
        } else {
            this.logger.warn("Cannot find a user http session to remove attachments from [{}].", documentReference);
            return false;
        }
    }

    @Override
    public void attachTemporaryAttachmentsInDocument(XWikiDocument document, List<String> fileNames)
    {
        if (!fileNames.isEmpty()) {
            for (String temporaryUploadedFile : fileNames) {
                Optional<XWikiAttachment> uploadedAttachmentOpt =
                    getUploadedAttachment(document.getDocumentReference(), temporaryUploadedFile);
                uploadedAttachmentOpt.ifPresent(uploadedAttachment -> {
                    XWikiAttachment previousAttachment = document.setAttachment(uploadedAttachment);
                    if (previousAttachment != null) {
                        uploadedAttachment.setVersion(previousAttachment.getNextVersion());
                    }
                });
            }
        }
    }
}
