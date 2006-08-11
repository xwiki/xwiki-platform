/*
 * Copyright 2006, XpertNet SARL, and individual contributors as indicated
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
 *
 * @author moghrabix
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;

public class DownloadAction extends XWikiAction {
	public String render(XWikiContext context) throws XWikiException {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String path = request.getRequestURI();
        String filename = Utils.decode(path.substring(path.lastIndexOf("/")+1),context);
         XWikiAttachment attachment = null;

        if (request.getParameter("id")!=null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        }
        else {
            attachment = doc.getAttachment(filename);
        }

        if (attachment==null) {
            Object[] args = { filename };
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                    "Attachment {0} not found", null, args);
        }

	XWikiPluginManager plugins = context.getWiki().getPluginManager();
	attachment = plugins.downloadAttachment(attachment, context);
        // Choose the right content type
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);

        String ofilename = XWiki.getURLEncoded(attachment.getFilename()).replaceAll("\\+", " ");
        response.addHeader("Content-disposition", "inline; filename=\"" +  ofilename + "\"");
        response.setDateHeader("Last-Modified", attachment.getDate().getTime());
        // Sending the content of the attachment
        byte[] data = attachment.getContent(context);
        response.setContentLength(data.length);
        try {
            response.getOutputStream().write(data);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response", e);
        }
        return null;
	}
}
