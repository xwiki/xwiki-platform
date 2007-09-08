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

public interface Comment extends MapObject
{

    /**
     * numeric id of the comment
     */
    String getId();

    void setId(String id);

    /**
     * page ID of the comment
     */
    String getPageId();

    void setPageId(String pageId);

    /**
     * title of the comment
     */
    String getTitle();

    void setTitle(String title);

    /**
     * notated content of the comment (use renderContent to render)
     */
    String getContent();

    void setContent(String content);

    /**
     * url to view the comment online
     */
    String getUrl();

    void setUrl(String url);

    /**
     * creation date of the attachment
     */
    Date getCreated();

    void setCreated(Date created);

    /**
     * creator of the attachment
     */
    String getCreator();

    void setCreator(String creator);

}
