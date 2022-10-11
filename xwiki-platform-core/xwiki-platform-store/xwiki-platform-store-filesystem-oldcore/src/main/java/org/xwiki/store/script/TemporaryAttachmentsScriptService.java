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
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Provider;
import javax.inject.Singleton;
import javax.servlet.ServletException;
import javax.servlet.http.Part;

import org.slf4j.Logger;
import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.script.service.ScriptService;
import org.xwiki.stability.Unstable;
import org.xwiki.store.TemporaryAttachmentException;
import org.xwiki.store.TemporaryAttachmentSessionsManager;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;
import com.xpn.xwiki.doc.XWikiAttachment;

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
@Unstable
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
     */
    public Attachment uploadTemporaryAttachment(DocumentReference documentReference, String fieldName)
    {
        XWikiContext context = this.contextProvider.get();
        Optional<XWikiAttachment> result = Optional.empty();
        try {
            Part part = context.getRequest().getPart(fieldName);
            if (part != null) {
                result = Optional.of(this.temporaryAttachmentSessionsManager.uploadAttachment(documentReference, part));
            }
        } catch (IOException | ServletException e) {
            this.logger.warn("Error while reading the request content part: [{}]", getRootCauseMessage(e));
        } catch (TemporaryAttachmentException e) {
            this.logger.warn("Error while uploading the attachment: [{}]", getRootCauseMessage(e));
        }

        return result.map(attachment -> {
            Document document = Optional.ofNullable(attachment.getDoc())
                .map(doc -> doc.newDocument(context))
                .orElse(null);
            return new Attachment(document, attachment, context);
        }).orElse(null);
    }
}
