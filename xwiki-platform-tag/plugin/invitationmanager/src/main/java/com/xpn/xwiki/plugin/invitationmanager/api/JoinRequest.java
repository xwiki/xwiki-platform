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
package com.xpn.xwiki.plugin.invitationmanager.api;

import java.util.Date;
import java.util.List;
import java.util.Map;

import com.xpn.xwiki.XWikiException;

/**
 * Base interface for {@link Invitation} and {@link MembershipRequest}
 * 
 * @version $Id: $
 */
public interface JoinRequest
{
    /**
     * Returns true is this invitation is new
     */
    boolean isNew();

    /**
     * @return A map with additional parameters for this request
     */
    Map getMap();

    /**
     * @return The date when this request has been sent, or <code>null</code> if it has not been
     *         sent.
     */
    Date getRequestDate();

    /**
     * @see #getRequestDate()
     */
    void setRequestDate(Date requestDate);

    /**
     * @return The date when this request has been answered, or <code>null</code> if it has not
     *         been answered.
     */
    Date getResponseDate();

    /**
     * @see #getResponseDate()
     */
    void setResponseDate(Date responseDate);

    /**
     * @return The list of roles this requests addresses
     */
    List getRoles();

    /**
     * @see #getMap()
     */
    void setMap(Map map);

    /**
     * @see #getRoles()
     */
    void setRoles(List roles);

    /**
     * @return The space to join
     */
    String getSpace();

    /**
     * @see #getSpace()
     */
    void setSpace(String space);

    /**
     * @return The status of this request
     * @see JoinRequestStatus
     */
    int getStatus();

    /**
     * @see #getStatus()
     */
    void setStatus(int status);

    /**
     * @return An explanatory text for this request
     */
    String getText();

    /**
     * @see #getText()
     */
    void setText(String text);

    /**
     * Saves the modified request
     * 
     * @throws XWikiException
     */
    void save() throws XWikiException;

    /**
     * Saves the modified request
     * 
     * @throws XWikiException
     */
    void saveWithProgrammingRights() throws XWikiException;

}
