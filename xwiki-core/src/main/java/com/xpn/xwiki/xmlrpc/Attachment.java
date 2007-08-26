/*
 * Copyright 2006-2007, XpertNet SARL, and individual contributors as indicated
 * by the contributors.txt.
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
package com.xpn.xwiki.xmlrpc;

import java.util.Map;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.web.XWikiEngineContext;

/**
 * {@inheritDoc} Notes:
 * <ul>
 * <li>XWiki ignores the content type field set by the user and uses the file extension instead to
 * determine it (Confluence requires this field to be set by the user).</li>
 * <li>XWiki always sets the id of the attachments to the empty string since this field is totally
 * useless.</li>
 * <li>XWiki always sets the title of the attachment to its file name.</li>
 * </ui>
 * 
 * @version $Id: $
 */
public class Attachment extends org.codehaus.swizzle.confluence.Attachment
{

    private static final String DEFAULT_MIME_TYPE = "application/octet-stream";

    /**
     * {@inheritDoc}
     */
    public Attachment()
    {
        super();
    }

    /**
     * {@inheritDoc}
     */
    public Attachment(Map data)
    {
        super(data);
    }

    /**
     * @param doc the (@link com.xpn.xwiki.XWikiDocument), used to create the Attachment. The reason
     *            we need its that some information for creating the Attachment is available only
     *            from the XWikiDocument object and not in the passed XWikiAttachment.
     * @param attachment the (@link com.xpn.xwiki.XWikiAttachment), used to create the Attachment
     * @param context the {@link com.xpn.xwiki.XWikiContext} object, used to get access to XWiki
     *            primitives for loading documents
     */
    public Attachment(XWikiDocument doc, XWikiAttachment attachment, XWikiContext context)
    {
        // Ids for attachments are useless so we don't set them (Confluence does)
        setId("");
        setPageId(doc.getFullName());
        // We use the filename as the document title (Confluence does the same)
        setTitle(attachment.getFilename());
        setFileName(attachment.getFilename());
        setFileSize(attachment.getFilesize());
        XWiki xwiki = context.getWiki();
        XWikiEngineContext engineContext = xwiki.getEngineContext();
        String mimeType = engineContext.getMimeType(attachment.getFilename());
        if (mimeType == null) {
            mimeType = DEFAULT_MIME_TYPE;
        }
        setContentType(mimeType);
        setCreator(attachment.getAuthor());
        setCreated(attachment.getDate());
        setUrl(doc.getAttachmentURL(attachment.getFilename(), "download", context));
        setComment(attachment.getComment());
    }
}
