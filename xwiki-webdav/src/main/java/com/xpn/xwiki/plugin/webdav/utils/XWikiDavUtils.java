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
package com.xpn.xwiki.plugin.webdav.utils;

import org.apache.jackrabbit.webdav.DavException;
import org.apache.jackrabbit.webdav.DavServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.xpn.xwiki.XWikiContext;
import com.xpn.xwiki.XWikiException;
import com.xpn.xwiki.api.Attachment;
import com.xpn.xwiki.api.Document;

/**
 * Holds all utility methods / variable for the webdav module.
 * 
 * @version $Id$.
 */
public final class XWikiDavUtils
{
    /**
     * Logger instance.
     */
    private static final Logger logger = LoggerFactory.getLogger(XWikiDavUtils.class);

    /**
     * Prefix used to indicate the beginning of a virtual grouping.
     */
    public static final String VIRTUAL_DIRECTORY_PREFIX = "_";

    /**
     * Post-fix used to indicate the beginning of a virtual grouping.
     */
    public static final String VIRTUAL_DIRECTORY_POSTFIX = "_";

    /**
     * Signature used to identify an attachment url.
     */
    public static final String XWIKI_ATTACHMENT_SIGNATURE = "/xwiki/bin/download/";

    /**
     * Signature used to identify a webdav url.
     */
    public static final String XWIKI_WEBDAV_SIGNATURE = "/xwiki/webdav/spaces/";

    /**
     * Collection of role-hint values for various components internal to xwiki-webdav.
     */
    public interface ResourceHint
    {
        /**
         * Root view.
         */
        public static final String ROOT = "root";

        /**
         * Pages base view.
         */
        public static final String PAGES = "pages-baseview";

        /**
         * Attachments base view.
         */
        public static final String ATTACHMENTS = "attachments-baseview";

        /**
         * Home base view.
         */
        public static final String HOME = "home-baseview";

        /**
         * Orphans base view.
         */
        public static final String ORPHANS = "orphans-baseview";

        /**
         * Whatsnew base view.
         */
        public static final String WHATSNEW = "whatsnew-baseview";
    }

    /**
     * Forbidden constructor.
     */
    private XWikiDavUtils()
    {

    }

    /**
     * Calculates the length of a subview (for groupings) given the total document count.
     * 
     * @param totalDocumentCount Total document count.
     * @return The calculated view name length.
     */
    public static int getSubViewNameLength(int totalDocumentCount)
    {
        // We might want to change this logic later.
        if (totalDocumentCount < 200) {
            return 1;
        } else if (totalDocumentCount < 5000) {
            return 2;
        } else {
            return 3;
        }
    }

    /**
     * @param doc The {@link Document} having the attachment.
     * @param attachment The {@link Attachment}.
     * @return The webdav url corresponding to the attachment.
     */
    public static String getDavURL(Document doc, Attachment attachment)
    {
        String docDownloadURL = doc.getExternalURL("download");
        String httpUrl =
            docDownloadURL.endsWith("/") ? docDownloadURL + attachment.getFilename()
                : docDownloadURL + "/" + attachment.getFilename();
        return getDavURL(httpUrl);
    }

    /**
     * @param httpUrl The http url of an attachment.
     * @return The calculated webdav url.
     */
    private static String getDavURL(String httpUrl)
    {
        // For the moment we'll only consider attachments.
        String webDAVUrl = "";
        if (httpUrl.contains(XWIKI_ATTACHMENT_SIGNATURE)) {
            String[] parts = httpUrl.split(XWIKI_ATTACHMENT_SIGNATURE);
            String[] elements = parts[1].split("/");
            webDAVUrl =
                parts[0] + XWIKI_WEBDAV_SIGNATURE + elements[0] + "/" + elements[1] + "/"
                    + elements[2];
        }
        return webDAVUrl;
    }

    /**
     * Returns if the user (in the context) has the given access level on the document in question.
     * 
     * @param right Access level.
     * @param docName Name of the document.
     * @param context The {@link XWikiContext}.
     * @return True if the user has the given access level for the document in question, false
     *         otherwise.
     */
    public static boolean hasAccess(String right, String docName, XWikiContext context)
    {
        boolean hasAccess = false;
        try {
            if (context.getWiki().getRightService().hasAccessLevel(right, context.getUser(),
                docName, context)) {
                hasAccess = true;
            }
        } catch (XWikiException ex) {
            logger.error("Error while validating access level.", ex);
        }
        return hasAccess;
    }

    /**
     * Validates if the user (in the context) has the given access level on the document in
     * question, if not, throws a {@link DavException}.
     * 
     * @param right Access level.
     * @param docName Name of the document.
     * @param context The {@link XWikiContext}.
     * @throws DavException If the user doesn't have enough access rights on the given document or
     *             if the access verification code fails.
     */
    public static void checkAccess(String right, String docName, XWikiContext context)
        throws DavException
    {
        try {
            if (!context.getWiki().getRightService().hasAccessLevel(right, context.getUser(),
                docName, context)) {
                throw new DavException(DavServletResponse.SC_FORBIDDEN);
            }
        } catch (XWikiException ex) {
            throw new DavException(DavServletResponse.SC_INTERNAL_SERVER_ERROR, ex);
        }
    }
}
