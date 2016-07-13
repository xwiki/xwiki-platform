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

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.URI;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.inject.Singleton;

import org.apache.fop.apps.io.ResourceResolverFactory;
import org.apache.xmlgraphics.io.Resource;
import org.apache.xmlgraphics.io.ResourceResolver;
import org.xwiki.component.annotation.Component;
import org.xwiki.formula.ImageData;
import org.xwiki.formula.ImageStorage;
import org.xwiki.model.EntityType;
import org.xwiki.model.reference.AttachmentReference;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.Utils;

/**
 * Resolves URIs sent by Apache FOP to embed images in the exported PDF. The strategy is the following:
 * <ul>
 * <li>When an attachment is rendered during the export (specifically when {@code pdf.vm} is rendered), the
 * {@link PdfURLFactory} is called and it saves the Attachment Entity Reference in a map in the XWiki Context</li>
 * <li>When Apache FOP embeds an image it calls this URI Resolver and we try to locate the Attachment Entity Reference
 * from that map and return the attachment stream.</li>
 * <li>Attachment links do not call the Resolver and are thus exported correctly using a full URL to the XWiki server
 * </li>
 * </ul>
 *
 * @version $Id$
 * @since 7.4M2
 */
@Component(roles = PDFResourceResolver.class)
@Singleton
public class PDFResourceResolver implements ResourceResolver
{
    private static final String TEX_ACTION = "/tex/";

    @Inject
    private Provider<XWikiContext> xcontextProvider;

    private ResourceResolver standardResolver = ResourceResolverFactory.createDefaultResourceResolver();

    @Override
    public Resource getResource(URI uri) throws IOException
    {
        XWikiContext xcontext = xcontextProvider.get();

        Map<String, AttachmentReference> attachmentMap =
            (Map<String, AttachmentReference>) xcontext.get(PdfURLFactory.PDF_EXPORT_CONTEXT_KEY);

        if (attachmentMap != null) {
            String uriString = uri.toString();

            // TODO: HACK
            // We're going through the getAttachmentURL() API so that when the PdfURLFactory is used, the generated
            // image is saved and then embedded in the exported PDF thanks to PDFURIResolver. In the future we need
            // to remove this hack by introduce a proper Resource for generated image (say TemporaryResource),
            // implement a TemporaryResourceSerializer<URL> and introduce a ResourceLoader interface and have it
            // implemented for TemporaryResource...
            if (uriString.contains(TEX_ACTION)) {
                // Note: See the comments in FormulaMacro to understand why we do a replace...
                AttachmentReference reference = attachmentMap.get(uriString.replace(TEX_ACTION, "/download/"));
                if (reference != null) {
                    // Get the generated image's input stream
                    ImageStorage storage = Utils.getComponent(ImageStorage.class);
                    ImageData image = storage.get(reference.getName());
                    return new Resource(new ByteArrayInputStream(image.getData()));
                }
            }
            // TODO: end HACK

            AttachmentReference reference = attachmentMap.get(uriString);
            if (reference != null) {
                try {
                    XWikiDocument xdoc =
                        xcontext.getWiki().getDocument(reference.extractReference(EntityType.DOCUMENT), xcontext);
                    // TODO: handle revisions
                    XWikiAttachment attachment =
                        xdoc.getAttachment(reference.extractReference(EntityType.ATTACHMENT).getName());
                    return new Resource(attachment.getContentInputStream(xcontext));
                } catch (Exception e) {
                    throw new IOException(String.format("Failed to resolve export URI [%s]", uriString), e);
                }
            }
        }

        return this.standardResolver.getResource(uri);
    }

    @Override
    public OutputStream getOutputStream(URI uri) throws IOException
    {
        // Not easy to implement in for attachment but not really needed in the context of PDF export anyway
        return this.standardResolver.getOutputStream(uri);
    }

}
