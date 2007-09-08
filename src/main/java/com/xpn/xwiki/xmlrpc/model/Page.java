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

public interface Page extends MapObject
{
    /**
     * the id of the page
     */
    String getId();

    void setId(String id);

    /**
     * the key of the space that this page belongs to
     */
    String getSpace();

    void setSpace(String space);

    /**
     * the id of the parent page
     */
    String getParentId();

    void setParentId(String parentId);

    /**
     * the title of the page
     */
    String getTitle();

    void setTitle(String title);

    /**
     * the url to view this page online
     */
    String getUrl();

    void setUrl(String url);

    /**
     * the number of locks current on this page
     */
    int getLocks();

    void setLocks(int locks);
    
    /**
     * the version number of this page
     */
    int getVersion();

    void setVersion(int version);

    /**
     * the page content
     */
    String getContent();

    void setContent(String content);

    /**
     * timestamp page was created
     */
    Date getCreated();

    void setCreated(Date created);

    /**
     * username of the creator
     */
    String getCreator();

    void setCreator(String creator);

    /**
     * timestamp page was modified
     */
    Date getModified();

    void setModified(Date modified);

    /**
     * username of the page's last modifier
     */
    String getModifier();

    void setModifier(String modifier);

    /**
     * whether or not this page is the space's homepage
     */
    boolean isHomePage();

    void setHomePage(boolean homePage);

    /**
     * status of the page (eg current or deleted)
     */
    String getContentStatus();

    void setContentStatus(String contentStatus);

    /**
     * whether the page is current and not deleted
     */
    boolean isCurrent();

    void setCurrent(boolean current);

}
