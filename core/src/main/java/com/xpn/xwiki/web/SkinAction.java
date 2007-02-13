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
 * @author erwan
 * @author sdumitriu
 */
package com.xpn.xwiki.web;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

public class SkinAction extends XWikiAction
{
    private static final Log log = LogFactory.getLog(XWiki.class);

    public String render(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiResponse response = context.getResponse();
        XWikiDocument doc = context.getDoc();

        String path = request.getPathInfo();
        int idx = path.lastIndexOf("/");
        while (idx > 0) {
            try {
                String filename = Utils.decode(path.substring(idx + 1), context);

                if (renderSkin(filename, doc, context))
                    return null;

                String baseskin = xwiki.getBaseSkin(context, true);
                if (renderSkin(filename, baseskin, context))
                    return null;

                XWikiDocument baseskindoc = xwiki.getDocument(baseskin, context);
                if (renderSkin(filename, baseskindoc, context))
                    return null;

                String defaultbaseskin = xwiki.getDefaultBaseSkin(context);
                if (renderSkin(filename, defaultbaseskin, context))
                    return null;
            } catch (XWikiException ex) {
                // TODO: ignored for the moment, this must be rethinked
                log.debug(new Integer(idx), ex);
            }
            idx = path.lastIndexOf("/", idx - 1);
        }
        return "docdoesnotexist";
    }

    private boolean renderSkin(String filename, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();

        try {
            BaseObject object = doc.getObject("XWiki.XWikiSkins", 0);
            String content = null;
            if (object != null) {
                content = object.getStringValue(filename);
            }

            if ((content != null) && (!content.equals(""))) {
                // Choose the right content type
                String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
                if (mimetype.equals("text/css")) {
                    content = context.getWiki().parseContent(content, context);
                }
                response.setContentType(mimetype);
                response.setDateHeader("Last-Modified", doc.getDate().getTime());
                // Sending the content of the attachment
                response.setContentLength(content.length());
                response.getWriter().write(content);
                return true;
            }

            XWikiAttachment attachment = doc.getAttachment(filename);
            if (attachment != null) {
                // Sending the content of the attachment
                byte[] data = attachment.getContent(context);
                String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
                if (mimetype.equals("text/css")) {
                    data = context.getWiki().parseContent(new String(data), context).getBytes();
                }
                response.setContentType(mimetype);
                response.setDateHeader("Last-Modified", attachment.getDate().getTime());
                response.setContentLength(data.length);
                response.getOutputStream().write(data);
                return true;
            }

            if (doc.getWeb().equals("skins")) {
                String path = "skins/" + doc.getName() + "/" + filename;
                if (!context.getWiki().resourceExists(path))
                    path = "skins/" + context.getWiki().getBaseSkin(context) + "/" + filename;

                byte[] data = context.getWiki().getResourceContentAsBytes(path);
                if ((data != null) && (data.length != 0)) {
                    // Choose the right content type
                    String mimetype =
                        xwiki.getEngineContext().getMimeType(filename.toLowerCase());
                    if (mimetype.equals("text/css")) {
                        data =
                            context.getWiki().parseContent(new String(data), context).getBytes();
                    }
                    response.setContentType(mimetype);
                    response.setDateHeader("Last-Modified", (new Date()).getTime());
                    // Sending the content of the attachment
                    response.setContentLength(data.length);
                    response.getOutputStream().write(data);
                    return true;
                } else {

                }
            }
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response",
                e);
        }

        return false;
    }

    private boolean renderSkin(String filename, String skin, XWikiContext context)
        throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        try {
            response.setDateHeader("Expires", (new Date()).getTime() + 30 * 24 * 3600 * 1000L);
            String path = "/skins/" + skin + "/" + filename;
            // Choose the right content type
            String mimetype = context.getEngineContext().getMimeType(filename.toLowerCase());
            if (mimetype != null)
                response.setContentType(mimetype);
            else
                response.setContentType("application/octet-stream");

            // Sending the content of the file
            byte[] data = context.getWiki().getResourceContentAsBytes(path);
            if (data == null || data.length == 0)
                return false;

            if (mimetype.equals("text/css")) {
                data = context.getWiki().parseContent(new String(data), context).getBytes();
            }
            response.getOutputStream().write(data);
            return true;
        } catch (IOException e) {
            if (skin.equals(xwiki.getDefaultBaseSkin(context)))
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response",
                    e);
            else
                return false;
        }
    }
}
