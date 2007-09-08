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

public interface Space extends MapObject
{

    /**
     * the space key
     */
    String getKey();

    void setKey(String key);

    /**
     * the name of the space
     */
    String getName();

    void setName(String name);

    /**
     * type of the space (not documented) 
     */
    String getType();

    void setType(String type);

    /**
     * the url to view this space online
     */
    String getUrl();

    void setUrl(String url);

    /**
     * the id of the space homepage
     */
    String getHomePage();

    void setHomePage(String homepage);

    /**
     * the HTML rendered space description
     */
    String getDescription();

    void setDescription(String description);

}
