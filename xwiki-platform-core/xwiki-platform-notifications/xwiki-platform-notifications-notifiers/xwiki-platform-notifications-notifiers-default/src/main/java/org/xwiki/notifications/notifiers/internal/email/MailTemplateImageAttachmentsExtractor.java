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
package org.xwiki.notifications.notifiers.internal.email;

import java.util.Collection;
import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.xwiki.component.annotation.Component;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;

/**
 * Extract all image from the attachments of a document.
 *
 * @version $Id$
 * @since 9.11RC1
 */
@Component(roles = MailTemplateImageAttachmentsExtractor.class)
@Singleton
public class MailTemplateImageAttachmentsExtractor
{
    @Inject
    private Provider<XWikiContext> contextProvider;

    /**
     * @param documentReference the reference of the document
     * @return all images contained in the attachments of the given document
     * @throws Exception if an error occurs
     */
    public Collection<Attachment> getImages(DocumentReference documentReference) throws Exception
    {
        XWikiContext context = contextProvider.get();
        XWiki xwiki = context.getWiki();
        Document document = new Document(xwiki.getDocument(documentReference, context), context);
        return document.getAttachmentList().stream().filter(att -> att.isImage()).collect(Collectors.toList());
    }
}
