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
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;

import java.io.IOException;

public class DownloadAction extends XWikiAction
{
    public String getFileName(String path, String action)
    {
        path = path.substring(path.indexOf("/" + action));
        int pos = 0;
        for (int i = 0; i < 3; i++) {
            pos = path.indexOf("/", pos + 1);
        }
        if (path.indexOf("/", pos + 1) > 0) {
            return path.substring(pos + 1, path.indexOf("/", pos + 1));
        }
        return path.substring(pos + 1);
    }

    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String path = request.getRequestURI();
        String filename = Utils.decode(getFileName(path, "download"), context);
        XWikiAttachment attachment;

        if (request.getParameter("id") != null) {
            int id = Integer.parseInt(request.getParameter("id"));
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        } else {
            attachment = doc.getAttachment(filename);
        }

        if (attachment == null) {
            Object[] args = {filename};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                "Attachment {0} not found", null, args);
        }

        XWikiPluginManager plugins = context.getWiki().getPluginManager();
        attachment = plugins.downloadAttachment(attachment, context);
        // Choose the right content type
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);

        String ofilename =
            context.getWiki().getURLEncoded(attachment.getFilename()).replaceAll("\\+", " ");

        // The inline attribute of Content-Disposition tells the browser that they should display
        // the downloaded file in the page (see http://www.ietf.org/rfc/rfc1806.txt for more
        // details). We do this so that JPG, GIF, PNG, etc are displayed without prompting a Save
        // dialog box. However, all mime types that cannot be displayed by the browser do prompt a
        // Save dialog box (exe, zip, xar, etc).
        response.addHeader("Content-disposition", "inline; filename=\"" + ofilename + "\"");

        response.setDateHeader("Last-Modified", attachment.getDate().getTime());

        // Sending the content of the attachment
        byte[] data;
        try {
            data = attachment.getContent(context);
        }
        catch (XWikiException e) {
            Object[] args = {filename};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                "Attachment content {0} not found", null, args);
        }

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
