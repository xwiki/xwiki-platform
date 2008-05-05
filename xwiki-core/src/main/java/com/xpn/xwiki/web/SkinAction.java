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
import java.util.Arrays;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import com.xpn.xwiki.XWiki;
import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.doc.XWikiAttachment;
import com.xpn.xwiki.doc.XWikiDocument;
import com.xpn.xwiki.objects.BaseObject;
import com.xpn.xwiki.util.Util;

/**
 * <p>
 * Action for serving skin files. It allows skins to be defined using XDocuments as skins, by
 * letting files be placed as text fields in an XWiki.XWikiSkins object, or as attachments to the
 * document, or as a file in the filesystem. If the file is not found in the current skin, then it
 * is searched in its base skin, and eventually in the default base skins,
 * </p>
 * <p>
 * This action indicates that the results should be publicly cacheable for 30 days.
 * </p>
 * 
 * @version $Id: $
 * @since 1.0
 */
public class SkinAction extends XWikiAction
{
    /** Logging helper. */
    private static final Log LOG = LogFactory.getLog(SkinAction.class);

    /** Path delimiter. */
    private static final String DELIMITER = "/";

    /** The directory where the skins are placed in the webapp. */
    private static final String SKINS_DIRECTORY = "skins";

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

        if (LOG.isDebugEnabled()) {
            LOG.debug("document: " + doc.getFullName() + " ; baseskin: " + baseskin
                + " ; defaultbaseskin: " + defaultbaseskin);
        }

        int idx = path.lastIndexOf(DELIMITER);
        boolean found = false;
        while (idx > 0) {
            try {
                String filename = Util.decodeURI(path.substring(idx + 1), context);
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Trying '" + filename + "'");
                }

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
                if (ex.getCode() == XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION) {
                    // This means that the response couldn't be sent, although the file was
                    // successfully found. Signal this further, and stop trying to render.
                    throw ex;
                }
                LOG.debug(new Integer(idx), ex);
            }
            idx = path.lastIndexOf(DELIMITER, idx - 1);
        }
        if (!found) {
            context.getResponse().setStatus(404);
            return "docdoesnotexist";
        }
        return null;
    }

    /**
     * Tries to serve a skin file using <tt>doc</tt> as a skin document. The file is searched in
     * the following places:
     * <ol>
     * <li>As the content of a property with the same name as the requested filename, from an
     * XWikiSkins object attached to the document.</li>
     * <li>As the content of an attachment with the same name as the requested filename.</li>
     * <li>As a file located on the filesystem, in the directory with the same name as the current
     * document (in case the URL was actually pointing to <tt>/skins/directory/file</tt>).</li>
     * </ol>
     * 
     * @param filename The name of the skin file that should be rendered.
     * @param doc The skin {@link XWikiDocument document}.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the attachment was found and the content was successfully sent.
     * @throws XWikiException If the attachment cannot be loaded.
     */
    private boolean renderSkin(String filename, XWikiDocument doc, XWikiContext context)
        throws XWikiException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Rendering file '" + filename + "' within the '" + doc.getFullName()
                + "' document");
        }
        try {
            if (doc.isNew()) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug(doc.getName() + " is not a document");
                }
            } else {
                return renderFileFromObjectField(filename, doc, context)
                    || renderFileFromAttachment(filename, doc, context)
                    || (SKINS_DIRECTORY.equals(doc.getSpace()) && renderSkinFromFilesystem(
                        filename, doc.getName(), context));
            }
        } catch (IOException e) {
            throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                "Exception while sending response:",
                e);
        }

        return renderSkinFromFilesystem(filename, doc.getName(), context);
    }

    /**
     * Tries to serve a skin file from the filesystem.
     * 
     * @param filename The name of the skin file that should be rendered.
     * @param skin The skin name, it should be a subdirectory in &lt;webapp-root&gt;/skins/
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the file was found and its content was successfully sent.
     * @throws XWikiException If the response cannot be sent.
     */
    private boolean renderSkinFromFilesystem(String filename, String skin, XWikiContext context)
        throws XWikiException
    {
        if (LOG.isDebugEnabled()) {
            LOG.debug("Rendering filesystem file '" + filename + "' from the '" + skin
                + "' skin directory");
        }
        XWikiResponse response = context.getResponse();
        String path = DELIMITER + SKINS_DIRECTORY + DELIMITER + skin + DELIMITER + filename;
        try {
            byte[] data;
            data = context.getWiki().getResourceContentAsBytes(path);
            if (data != null && data.length > 0) {
                String mimetype = context.getEngineContext().getMimeType(filename.toLowerCase());
                Date modified = null;
                if (isCssMimeType(mimetype) || isJavascriptMimeType(mimetype)) {
                    byte[] newdata = context.getWiki().parseContent(new String(data), context).getBytes();
                    // If the content contained velocity code, then it should not be cached
                    if (Arrays.equals(newdata, data)) {
                        modified = context.getWiki().getResourceLastModificationDate(path);
                    }
                    else {
                        modified = new Date();
                        data = newdata;
                    }
                }
                else {
                    modified = context.getWiki().getResourceLastModificationDate(path);
                }
                setupHeaders(response, mimetype, modified, data.length);
                try {
                    response.getOutputStream().write(data);
                } catch (IOException e) {
                    throw new XWikiException(XWikiException.MODULE_XWIKI_APP,
                        XWikiException.ERROR_XWIKI_APP_SEND_RESPONSE_EXCEPTION,
                        "Exception while sending response",
                        e);
                }
                return true;
            }
        } catch (IOException ex) {
            LOG.info("Skin file '" + path + "' does not exist or cannot be accessed");
        }
        return false;
    }

    /**
     * Tries to serve the content of an XWikiSkins object field as a skin file.
     * 
     * @param filename The name of the skin file that should be rendered.
     * @param doc The skin {@link XWikiDocument document}.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the object exists, and the field is set to a non-empty value, and
     *         its content was successfully sent.
     * @throws IOException If the response cannot be sent.
     */
    public boolean renderFileFromObjectField(String filename, XWikiDocument doc,
        XWikiContext context) throws IOException
    {
        LOG.debug("... as object property");
        BaseObject object = doc.getObject("XWiki.XWikiSkins");
        String content = null;
        if (object != null) {
            content = object.getStringValue(filename);
        }

        if (!StringUtils.isBlank(content)) {
            XWiki xwiki = context.getWiki();
            XWikiResponse response = context.getResponse();
            String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
            if (isCssMimeType(mimetype) || isJavascriptMimeType(mimetype)) {
                content = context.getWiki().parseContent(content, context);
            }
            setupHeaders(response, mimetype, doc.getDate(), content.length());
            response.getWriter().write(content);
            return true;
        } else {
            LOG.debug("Object field not found or empty");
        }
        return false;
    }

    /**
     * Tries to serve the content of an attachment as a skin file.
     * 
     * @param filename The name of the skin file that should be rendered.
     * @param doc The skin {@link XWikiDocument document}.
     * @param context The current {@link XWikiContext request context}.
     * @return <tt>true</tt> if the attachment was found and its content was successfully sent.
     * @throws IOException If the response cannot be sent.
     * @throws XWikiException If the attachment cannot be loaded.
     */
    public boolean renderFileFromAttachment(String filename, XWikiDocument doc,
        XWikiContext context) throws IOException, XWikiException
    {
        LOG.debug("... as attachment");
        XWikiAttachment attachment = doc.getAttachment(filename);
        if (attachment != null) {
            XWiki xwiki = context.getWiki();
            XWikiResponse response = context.getResponse();
            byte[] data = attachment.getContent(context);
            String mimetype = xwiki.getEngineContext().getMimeType(filename.toLowerCase());
            if (isCssMimeType(mimetype) || isJavascriptMimeType(mimetype)) {
                data = context.getWiki().parseContent(new String(data), context).getBytes();
            }
            setupHeaders(response, mimetype, attachment.getDate(), data.length);
            response.getOutputStream().write(data);
            return true;
        } else {
            LOG.debug("Attachment not found");
        }
        return false;
    }

    /**
     * Checks if a mimetype indicates a javascript file.
     * 
     * @param mimetype The mime type to check.
     * @return <tt>true</tt> if the mime type represents a javascript file.
     */
    public boolean isJavascriptMimeType(String mimetype)
    {
        boolean result =
            "text/javascript".equalsIgnoreCase(mimetype)
                || "application/x-javascript".equalsIgnoreCase(mimetype)
                || "application/javascript".equalsIgnoreCase(mimetype);
        result |=
            "application/ecmascript".equalsIgnoreCase(mimetype)
                || "text/ecmascript".equalsIgnoreCase(mimetype);
        return result;
    }

    /**
     * Checks if a mimetype indicates a CSS file.
     * 
     * @param mimetype The mime type to check.
     * @return <tt>true</tt> if the mime type represents a css file.
     */
    public boolean isCssMimeType(String mimetype)
    {
        return "text/css".equalsIgnoreCase(mimetype);
    }

    /**
     * Sets several headers to properly identify the response.
     * 
     * @param response The servlet response object, where the headers should be set.
     * @param mimetype The mimetype of the file. Used in the "Content-Type" header.
     * @param lastChanged The date of the last change of the file. Used in the "Last-Modified"
     *            header.
     * @param length The length of the content (in bytes). Used in the "Content-Length" header.
     */
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
        response.setHeader("Cache-Control", "public");
        response.setDateHeader("Expires", (new Date()).getTime() + 30 * 24 * 3600 * 1000L);
        response.setContentLength(length);
    }
}
