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
package org.xwiki.bridge.event;

import org.xwiki.model.reference.DocumentReference;
import org.xwiki.observation.event.EndEvent;

/**
 * An event triggered after a user is authenticated.
 * <p>
 * The event also send the following parameters:
 * </p>
 * <ul>
 * <li>data: the current {com.xpn.xwiki.XWikiContext} instance</li>
 * </ul>
 * 
 * @version $Id$
 * @since 10.0RC1
 */

public class UserAuthenticatedEvent implements EndEvent
{
    private static final long serialVersionUID = 1L;

    public UserAuthenticatedEvent()
    {
        super();
    }
    
    /**
     * Constructor initializing the user refrences of the event.
     * 
     * @param DocumentReference the refrences of the authenticated user
     */
     public UserAuthenticatedEvent(DocumentReference documentReference)
    {
        super(documentReference);
    } 
}
