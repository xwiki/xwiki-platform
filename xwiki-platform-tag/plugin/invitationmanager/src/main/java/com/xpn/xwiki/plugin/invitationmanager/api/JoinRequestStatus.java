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

/**
 * The status of a request for joining a space
 * 
 * @see JoinRequest#setStatus(int)
 * @version $Id: $
 */
public interface JoinRequestStatus
{
    /**
     * The request status is not specified. It can have any of the allowed values. This can be
     * useful when retrieving stored requests.
     */
    int ANY = 0;

    /**
     * The request has been created but has not yet been sent
     */
    int CREATED = 1;

    /**
     * The request has been sent but no answer has been received so far
     */
    int SENT = 2;

    /**
     * The request has been accepted
     */
    int ACCEPTED = 3;

    /**
     * The request has been refused
     */
    int REFUSED = 4;

    /**
     * The request has been canceled
     */
    int CANCELLED = 5;
}
