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
package com.xpn.xwiki.pdf.impl;

import java.util.Map;

import javax.xml.transform.Source;
import javax.xml.transform.TransformerException;
import javax.xml.transform.URIResolver;
import javax.xml.transform.stream.StreamSource;

import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;

/**
 * Resolves URIs sent by Apache FOP to embed images in the exported PDF. The strategy is the following:
 * <ul>
 *   <li>When an attachment is rendered during the export (specifically when {@code pdf.vm} is rendered), the
 *   {@link PdfURLFactory} is called and it saves the Attachment Entity Reference in a map in the XWiki Context</li>
 *   <li>When Apache FOP embeds an image it calls this URI Resolver and we try to locate the Attachment Entity
 *   Reference from that map and return the attachment stream.</li>
 *   <li>Attachment links do not call the Resolver and are thus exported correctly using a full URL to the XWiki
 *   server</li>
 * </ul>
 *
 * @version $Id$
 * @since 5.0RC1
 */
public class PDFURIResolver implements URIResolver
{
    /**
     * @see #PDFURIResolver(com.xpn.xwiki.XWikiContext)
     */
    private Map<String, AttachmentReference> attachmentMap;

    /**
     * @see #PDFURIResolver(com.xpn.xwiki.XWikiContext)
     */
    private XWikiContext context;

    /**
     * @param context the XWiki Context from where we try to find the attachment map saved in the {@link PdfURLFactory}
     *        earlier on
     */
    public PDFURIResolver(XWikiContext context)
    {
        this.attachmentMap = (Map<String, AttachmentReference>) context.get(PdfURLFactory.PDF_EXPORT_CONTEXT_KEY);
        this.context = context;
    }

    @Override
    public Source resolve(String href, String base) throws TransformerException
    {
        if (this.attachmentMap != null) {
            AttachmentReference reference = this.attachmentMap.get(href);
            if (reference != null) {
                try {
                    XWikiDocument xdoc = this.context.getWiki().getDocument(
                        reference.extractReference(EntityType.DOCUMENT), this.context);
                    // TODO: handle revisions
                    XWikiAttachment attachment = xdoc.getAttachment(
                        reference.extractReference(EntityType.ATTACHMENT).getName());
                    return new StreamSource(attachment.getContentInputStream(this.context));
                } catch (Exception e) {
                    throw new TransformerException(String.format("Failed to resolve export URI [%s]", href), e);
                }
            }
        }

        // Defaults to the default URI Resolver in FO
        return null;
    }
}
