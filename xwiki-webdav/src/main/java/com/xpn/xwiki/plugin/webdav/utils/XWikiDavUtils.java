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
    public static final String XWIKI_WEBDAV_SIGNATURE = "/xwiki/webdav/pages/";

    /**
     * Resource hint for root view.
     */
    public static final String ROOT_VIEW = "root";

    /**
     * Resource hint for 'pages' base view.
     */
    public static final String PAGES_BASEVIEW = "pages";

    /**
     * Resource hint for 'attachments' base view.
     */
    public static final String ATTACHMENTS_BASEVIEW = "attachments";

    /**
     * Resource hint for 'home' base view.
     */
    public static final String HOME_BASEVIEW = "home";

    /**
     * Resource hint for 'orphans' base view.
     */
    public static final String ORPHANS_BASEVIEW = "orphans";

    /**
     * Resource hint for 'whatsnew' base view.
     */
    public static final String WHATSNEW_BASEVIEW = "whatsnew";

    /**
     * Array of all base view identifiers.
     */
    public static final String[] BASE_VIEWS =
        new String[] {PAGES_BASEVIEW, ATTACHMENTS_BASEVIEW, HOME_BASEVIEW, ORPHANS_BASEVIEW,
        WHATSNEW_BASEVIEW};

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
}
