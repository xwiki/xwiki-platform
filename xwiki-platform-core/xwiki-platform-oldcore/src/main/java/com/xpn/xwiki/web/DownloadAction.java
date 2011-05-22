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
package com.xpn.xwiki.web;

import java.io.InputStream;
import java.io.IOException;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.plugin.XWikiPluginManager;
import com.xpn.xwiki.util.Util;
import org.apache.commons.io.IOUtils;

/**
 * The action for downloading attachments from the server.
 *
 * @version $Id$
 */
public class DownloadAction extends XWikiAction
{
    /** The identifier of the download action. */
    public static final String ACTION_NAME = "download";

    /** The URL part seperator. */
    private static final String SEPERATOR = "/";

    /**
     * Get the filename of the attachment from the path and the action.
     *
     * @param path the request URI.
     * @param action the action used to download the attachment.
     * @return the filename of the attachment.
     */
    public String getFileName(final String path, final String action)
    {
        final String subPath = path.substring(path.indexOf(SEPERATOR + action));
        int pos = 0;
        for (int i = 0; i < 3; i++) {
            pos = subPath.indexOf(SEPERATOR, pos + 1);
        }
        if (subPath.indexOf(SEPERATOR, pos + 1) > 0) {
            return subPath.substring(pos + 1, subPath.indexOf(SEPERATOR, pos + 1));
        }
        return subPath.substring(pos + 1);
    }

    /**
     * {@inheritDoc}
     *
     * @see com.xpn.xwiki.web.XWikiAction#render(XWikiContext)
     */
    public String render(XWikiContext context) throws XWikiException
    {
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();
        String path = request.getRequestURI();
        String filename = Util.decodeURI(getFileName(path, ACTION_NAME), context);
        XWikiAttachment attachment;

        final String idStr = request.getParameter("id");
        if (idStr != null) {
            int id = Integer.parseInt(idStr);
            attachment = (XWikiAttachment) doc.getAttachmentList().get(id);
        } else {
            attachment = doc.getAttachment(filename);
        }

        if (attachment == null) {
            Object[] args = {filename};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND, "Attachment {0} not found",
                null, args);
        }

        XWikiPluginManager plugins = context.getWiki().getPluginManager();
        attachment = plugins.downloadAttachment(attachment, context);
        // Choose the right content type
        String mimetype = attachment.getMimeType(context);
        response.setContentType(mimetype);
        response.setCharacterEncoding("");

        long lastModifiedOnClient = request.getDateHeader("If-Modified-Since");
        long lastModifiedOnServer = attachment.getDate().getTime();
        if (lastModifiedOnClient != -1 && lastModifiedOnClient >= lastModifiedOnServer) {
            response.setStatus(XWikiResponse.SC_NOT_MODIFIED);
            return null;
        }

        String ofilename =
            Util.encodeURI(attachment.getFilename(), context).replaceAll("\\+", " ");

        // The inline attribute of Content-Disposition tells the browser that they should display
        // the downloaded file in the page (see http://www.ietf.org/rfc/rfc1806.txt for more
        // details). We do this so that JPG, GIF, PNG, etc are displayed without prompting a Save
        // dialog box. However, all mime types that cannot be displayed by the browser do prompt a
        // Save dialog box (exe, zip, xar, etc).
        String dispType = "inline";

        if ("1".equals(request.getParameter("force-download"))) {
            dispType = "attachment";
        }
        response.addHeader("Content-disposition", dispType + "; filename=\"" + ofilename + "\"");

        response.setDateHeader("Last-Modified", lastModifiedOnServer);

        // Sending the content of the attachment
        sendContent(attachment, response, filename, context);
        return null;
    }

    /**
     * Send the attachment content in the response.
     *
     * @param attachment the attachment to get content from.
     * @param response the response to write to.
     * @param filename the filename to show in the message in case an exception needs to be thrown.
     * @param context the XWikiContext just in case it is needed to load the attachment content.
     * @throws XWikiException if someting goes wrong.
     */
    private static void sendContent(final XWikiAttachment attachment,
                                    final XWikiResponse response,
                                    final String filename,
                                    final XWikiContext context)
        throws XWikiException
    {
        InputStream stream = null;
        try {
            response.setContentLength(attachment.getContentSize(context));
            stream = attachment.getContentInputStream(context);
            IOUtils.copy(stream, response.getOutputStream());
        } catch (XWikiException e) {
            Object[] args = {filename};
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_ATTACHMENT_NOT_FOUND,
                "Attachment content {0} not found", null, args);
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response", e);
        } finally {
            if (stream != null) {
                IOUtils.closeQuietly(stream);
            }
        }
    }
}
