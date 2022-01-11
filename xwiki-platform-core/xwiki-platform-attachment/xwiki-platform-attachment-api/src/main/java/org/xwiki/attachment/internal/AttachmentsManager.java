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

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiDocument;

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
}
