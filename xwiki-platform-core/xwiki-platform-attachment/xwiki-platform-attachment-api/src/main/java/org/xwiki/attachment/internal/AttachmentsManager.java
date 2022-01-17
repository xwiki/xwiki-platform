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

import java.util.List;
import java.util.Objects;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;
import org.xwiki.model.reference.DocumentReferenceResolver;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.SOURCE_NAME_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_LOCATION_FIELD;
import static org.xwiki.attachment.internal.RedirectAttachmentClassDocumentInitializer.TARGET_NAME_FIELD;

/**
 * Provide operations to inspect and manipulate attachments.
 *
 * @version $Id$
 * @since 14.0RC1
 */
@Component(roles = AttachmentsManager.class)
@Singleton
public class AttachmentsManager
{
    @Inject
    private Provider<XWikiContext> xcontextProvider;

    @Inject
    private DocumentReferenceResolver<String> documentReferenceResolver;

    /**
     * Check if an attachment exists.
     *
     * @param attachmentLocation the reference of the attachment to check
     * @return {@code true} if the attachment is found at the requested location, {@code false} otherwise
     * @throws XWikiException if the attachments couldn't be retrieved
     */
    public boolean available(AttachmentReference attachmentLocation) throws XWikiException
    {
        XWikiDocument document = this.xcontextProvider.get().getWiki()
            .getDocument(attachmentLocation.getDocumentReference(), this.xcontextProvider.get());
        boolean exists;
        if (document == null) {
            exists = false;
        } else {
            exists = document.getAttachment(attachmentLocation.getName()) != null;
        }
        return exists;
    }

    /**
     * Return the reference of the new location of the attachment.
     *
     * @param attachmentReference the reference of the request attachment
     * @return the reference of the new location of the attachment if it exists, {@code Optional#empty} otherwise
     * @throws XWikiException in case of error when accessing the document
     */
    public Optional<AttachmentReference> getRedirection(AttachmentReference attachmentReference) throws XWikiException
    {
        XWikiDocument document = this.xcontextProvider.get().getWiki()
            .getDocument(attachmentReference.getDocumentReference(), this.xcontextProvider.get());
        if (document == null) {
            // TODO:  log warning ?
            return Optional.empty();
        } else {
            return document.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE).stream()
                .filter(redirectObj -> Objects.equals(redirectObj.getStringValue(SOURCE_NAME_FIELD),
                    attachmentReference.getName()))
                .findFirst()
                .map(redirectObj -> {
                    String targetName = redirectObj.getStringValue(TARGET_NAME_FIELD);
                    DocumentReference targetLocation =
                        this.documentReferenceResolver.resolve(redirectObj.getStringValue(TARGET_LOCATION_FIELD));
                    return new AttachmentReference(targetName, targetLocation);
                });
        }
    }

    /**
     * Remove a redirection if it exists.
     *
     * @param attachmentName the name of the redirect attachment
     * @param targetDocument the document containing the redirection
     * @return {@code true} if the redirection was removed, {@code false} otherwise
     */
    public boolean removeExistingRedirection(String attachmentName, XWikiDocument targetDocument)
    {
        boolean changed = false;
        List<BaseObject> targetRedirections =
            targetDocument.getXObjects(RedirectAttachmentClassDocumentInitializer.REFERENCE);
        for (BaseObject targetRedirection : targetRedirections) {
            if (targetRedirection.getStringValue(SOURCE_NAME_FIELD).equals(attachmentName)) {
                changed = true;
                targetDocument.removeXObject(targetRedirection);
            }
        }
        return changed;
    }
}
