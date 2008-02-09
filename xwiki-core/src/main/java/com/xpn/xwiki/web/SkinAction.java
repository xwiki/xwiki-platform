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
 *
 */
package com.xpn.xwiki.web;

import java.io.IOException;
import java.util.Date;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;

public class SkinAction extends XWikiAction
{
    /** Loggin helper */
    private static final Log log = LogFactory.getLog(SkinAction.class);

    /**
     * {@inheritDoc}
     * 
     * @see XWikiAction#render(XWikiContext)
     */
    public String render(XWikiContext context) throws XWikiException
    {
        XWiki xwiki = context.getWiki();
        XWikiRequest request = context.getRequest();
        XWikiDocument doc = context.getDoc();

        String baseskin = xwiki.getBaseSkin(context, true);
        XWikiDocument baseskindoc = xwiki.getDocument(baseskin, context);
        String defaultbaseskin = xwiki.getDefaultBaseSkin(context);
        String path = request.getPathInfo();
        log.debug("document: " + doc.getFullName() + " ; baseskin: " + baseskin
            + " ; defaultbaseskin: " + defaultbaseskin);
        int idx = path.lastIndexOf("/");
        boolean found = false;
        while (idx > 0) {
            try {
                String filename = Utils.decode(path.substring(idx + 1), context);
                log.debug("Trying '" + filename + "'");

                if (renderSkin(filename, doc, context)) {
                    found = true;
                    break;
                }

                if (!doc.getName().equals(baseskin)) {
                    if (renderSkin(filename, baseskindoc, context)) {
                        found = true;
                        break;
                    }
                }

                if (!(doc.getName().equals(defaultbaseskin) || baseskin.equals(defaultbaseskin))) {
                    // defaultbaseskin can only be on the filesystem, so don't try to use it as a
                    // skin document.
                    if (renderSkinFromFilesystem(filename, defaultbaseskin, context)) {
                        found = true;
                        break;
                    }
                }
            } catch (XWikiException ex) {
                // TODO: ignored for the moment, this must be rethinked
                log.debug(new Integer(idx), ex);
            }
            idx = path.lastIndexOf("/", idx - 1);
        }
        if (!found) {
            context.getResponse().setStatus(404);
            return "docdoesnotexist";
        }
        return null;
    }

    private boolean renderSkin(String filename, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        log.debug("Rendering file '" + filename + "' within the '" + doc.getFullName()
            + "' document");
        try {
            if (doc.isNew()) {
                log.debug(doc.getName() + " is not a document");
                if ("skins".equals(doc.getSpace())) {
                    log.debug("Trying on the filesystem");
                }
            } else {
                return renderFileFromObjectField(filename, doc, context)
                    || renderFileFromAttachment(filename, doc, context)
                    || renderSkinFromFilesystem(filename, doc.getName(), context);
            }
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response",
                e);
        }

        return renderSkinFromFilesystem(filename, doc.getName(), context);
    }

    private boolean renderSkinFromFilesystem(String filename, String skin, XWikiContext context)
        throws XWikiException
    {
        log.debug("Rendering file '" + filename + "' from the '" + skin + "' skin directory");
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        try {
            String path = "/skins/" + skin + "/" + filename;
            byte[] data;
            try {
                data = context.getWiki().getResourceContentAsBytes(path);
            } catch (Exception ex) {
                log.info("Skin file '" + path + "' does not exist");
                return false;
            }
            // Choose the right content type
            String mimetype = context.getEngineContext().getMimeType(filename.toLowerCase());

            // Sending the content of the file
            if (data == null || data.length == 0) {
                return false;
            }

            if ("text/css".equals(mimetype) || isJavascriptMimeType(mimetype)) {
                data = context.getWiki().parseContent(new String(data), context).getBytes();
            }
            setupHeaders(response, mimetype, new Date(), data.length);
            response.getOutputStream().write(data);
            return true;
        } catch (IOException e) {
            if (skin.equals(xwiki.getDefaultBaseSkin(context))) {
                throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                    XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                    "Exception while sending response",
                    e);
            } else {
                return false;
            }
        }
    }

    public boolean renderFileFromObjectField(String filename, XWikiDocument doc,
        XWikiContext context) throws IOException
    {
        log.debug("... as object property");
        BaseObject object = doc.getObject("XWiki.XWikiSkins");
        XWiki xwiki = context.getWiki();
        XWikiResponse response = context.getResponse();
        String content = null;
        if (object != null) {
            content = object.getStringValue(filename);
        }

        if (!StringUtils.isBlank(content)) {
            // Choose the right content type
            String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
            if (mimetype.equals("text/css") || isJavascriptMimeType(mimetype)) {
                content = context.getWiki().parseContent(content, context);
            }
            setupHeaders(response, mimetype, doc.getDate(), content.length());
            response.getWriter().write(content);
            return true;
        } else {
            log.debug("Object field not found or empty");
        }
        return false;
    }

    public boolean renderFileFromAttachment(String filename, XWikiDocument doc,
        XWikiContext context) throws IOException, XWikiException
    {
        log.debug("... as attachment");
        XWikiAttachment attachment = doc.getAttachment(filename);
        if (attachment != null) {
            XWiki xwiki = context.getWiki();
            XWikiResponse response = context.getResponse();
            // Sending the content of the attachment
            byte[] data = attachment.getContent(context);
            String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
            if ("text/css".equals(mimetype) || isJavascriptMimeType(mimetype)) {
                data = context.getWiki().parseContent(new String(data), context).getBytes();
            }
            setupHeaders(response, mimetype, attachment.getDate(), data.length);
            response.getOutputStream().write(data);
            return true;
        } else {
            log.debug("Attachment not found");
        }
        return false;
    }

    /**
     * Checks if a mimetype indicates a javascript file.
     * 
     * @param mimetype The mime type to check
     * @return true if the mime type represents a javascript file
     */
    public boolean isJavascriptMimeType(String mimetype)
    {
        return ("text/javascript".equalsIgnoreCase(mimetype)
            || "application/x-javascript".equalsIgnoreCase(mimetype)
            || "application/javascript".equalsIgnoreCase(mimetype)
            || "application/ecmascript".equalsIgnoreCase(mimetype) || "text/ecmascript"
            .equalsIgnoreCase(mimetype));
    }

    protected void setupHeaders(XWikiResponse response, String mimetype, Date lastChanged,
        int length)
    {
        if (!StringUtils.isBlank(mimetype)) {
            response.setContentType(mimetype);
        } else {
            response.setContentType("application/octet-stream");
        }
        response.setDateHeader("Last-Modified", lastChanged.getTime());
        // Cache for one month (30 days)
        response.setDateHeader("Expires", (new Date()).getTime() + 30 * 24 * 3600 * 1000L);
        response.setContentLength(length);
    }
}
