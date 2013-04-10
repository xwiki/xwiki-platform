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

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

import org.xwiki.model.reference.AttachmentReference;
import org.xwiki.model.reference.DocumentReference;

import com.xpn.xwiki.XWikiContext;

/**
 * Computes Image and Link URLs for attachments by using an absolute URLs but stores a Map in the XWiki Context to
 * associate attachment URLs to Attachment Entity References so that when executing a PDF export the custom URI Resolver
 * we use can stream image attachment content so that they are embedded in the PDF.
 * 
 * @version $Id$
 */
public class PdfURLFactory extends FileSystemURLFactory
{
    /**
     * Key used to save image attachment data.
     * @see #saveAttachmentReference(java.net.URL, String, String, String, String, com.xpn.xwiki.XWikiContext)
     */
    static final String PDF_EXPORT_CONTEXT_KEY = "pdfExportImageURLMap";

    @Override
    public URL createAttachmentURL(String filename, String space, String name, String action, String querystring,
        String wiki, XWikiContext context)
    {
        URL url = super.createAttachmentURL(filename, space, name, action, querystring, wiki, context);
        saveAttachmentReference(url, wiki, space, name, filename, context);
        return url;
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String space, String name, String revision, String wiki,
        XWikiContext context)
    {
        URL url = super.createAttachmentURL(filename, space, name, revision, wiki, context);
        saveAttachmentReference(url, wiki, space, name, filename, context);
        return url;
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String web, String name, String revision,
        String querystring, String xwikidb, XWikiContext context)
    {
        URL url = super.createAttachmentRevisionURL(filename, web, name, revision, querystring, xwikidb,
            context);
        saveAttachmentReference(url, xwikidb, web, name, filename, context);
        return url;
    }

    @Override
    public URL createAttachmentRevisionURL(String filename, String web, String name, String revision, long recycleId,
        String querystring, String xwikidb, XWikiContext context)
    {
        URL url = super.createAttachmentRevisionURL(filename, web, name, revision, recycleId, querystring, xwikidb,
            context);
        saveAttachmentReference(url, xwikidb, web, name, filename, context);
        return url;
    }

    /**
     * @param url the URL to save in the attachment map
     * @param wiki the wiki where the attachment is located
     * @param space the space where the attachment is located
     * @param page the page where the attachment is located
     * @param fileName the name of the attachment file
     * @param context the XWiki Context where to find the attachment map that we add to
     * @see PDFURIResolver
     */
    private void saveAttachmentReference(URL url, String wiki, String space, String page, String fileName,
        XWikiContext context)
    {
        // Save Entity Reference pointed to by this URL in the Context so that it can be used at export time in the
        // PDF URIResolver code.
        Map<String, AttachmentReference> attachmentMap =
            (Map<String, AttachmentReference>) context.get(PDF_EXPORT_CONTEXT_KEY);
        if (attachmentMap == null) {
            attachmentMap = new HashMap<String, AttachmentReference>();
            context.put(PDF_EXPORT_CONTEXT_KEY, attachmentMap);
        }
        attachmentMap.put(url.toString(), new AttachmentReference(fileName, new DocumentReference(wiki, space, page)));
    }
}
