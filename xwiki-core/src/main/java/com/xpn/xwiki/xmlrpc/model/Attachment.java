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
 *
 */

package com.xpn.xwiki.xmlrpc.model;

import java.util.Date;

/**
 * Represents an Attachment as described in the <a href="Confluence specification">
 * http://confluence.atlassian.com/display/DOC/Remote+API+Specification</a>.
 * 
 * @version $Revision$ $Date$
 */
public interface Attachment extends MapObject
{

    /**
     * numeric id of the attachment
     */
    String getId();

    void setId(String id);

    /**
     * page ID of the attachment
     */

    String getPageId();

    void setPageId(String pageId);

    /**
     * @return the title of the attachment
     */
    String getTitle();

    void setTitle(String title);

    /**
     * @return The file name of the attachment.
     */
    String getFileName();

    /**
     * @param fileName The file name of the attachment {color:#cc3300}(Required){color}.
     * For example "picture.jpg".
     */
    void setFileName(String fileName);

    /**
     * @return the size of the attachment in bytes
     */
    int getFileSize();

    /**
     * @param fileSize the size of the attachment in bytes (encoded as a String)
     * Confluence expects this number encoded as a String and swizzle takes care of the encoding.
     */
    void setFileSize(int fileSize);

    /**
     * @return the MIME content type of the attachment.
     */
    String getContentType();

    /**
     * @param contentType the MIME content type of the attachment.
     * {color:#cc0000}Required{color} by Confluence.
     * Ignored by XWiki which computes the content type from the file extension. 
     */
    void setContentType(String contentType);

    /**
     * @return the creation date of the attachment
     */
    Date getCreated();

    void setCreated(Date created);

    /**
     * @return the name of the user that created of the attachment
     */
    String getCreator();

    void setCreator(String creator);

    /**
     * @return the url to download the attachment
     */
    String getUrl();

    void setUrl(String url);

    /**
     * @return comment for the attachment {color:#cc3300}(Required){color}
     */
    String getComment();

    void setComment(String comment);

}
