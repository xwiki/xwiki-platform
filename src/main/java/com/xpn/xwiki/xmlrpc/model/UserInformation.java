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

package com.xpn.xwiki.xmlrpc.model;

import java.util.Date;

public interface UserInformation extends MapObject
{

    /**
     * the username of this user
     */
    String getUsername();

    void setUsername(String username);

    /**
     * the user description
     */
    String getContent();

    void setContent(String content);

    /**
     * the creator of the user
     */
    String getCreatorName();

    void setCreatorName(String creatorName);

    /**
     * the url to view this user online
     */
    String getLastModifierName();

    void setLastModifierName(String lastModifierName);

    /**
     * the version
     */
    int getVersion();

    void setVersion(int version);

    /**
     * the ID of the user
     */
    String getId();

    void setId(String id);

    /**
     * the date the user was created
     */
    Date getCreationDate();

    void setCreationDate(Date creationDate);

    /**
     * the date the user was last modified
     */
    Date getLastModificationDate();

    void setLastModificationDate(Date lastModificationDate);

}
