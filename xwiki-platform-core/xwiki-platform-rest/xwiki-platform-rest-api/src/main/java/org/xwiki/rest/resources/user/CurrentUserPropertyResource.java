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
package org.xwiki.rest.resources.user;

import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Update a boolean or static list property of the current user to its next value (for a Boolean this means set it
 * to true if it was false and vice versa).
 *
 * @version $Id$
 * @since 9.7RC1
 */
@Path("/currentuser/properties/{propertyName}/next")
@Unstable
public interface CurrentUserPropertyResource
{
    /**
     * @param propertyName the xproperty name to modify
     * @return the REST response object with the Code and the content
     * @throws XWikiRestException if an error occurred while setting the next value
     */
    @PUT Response setNextPropertyValue(
        @PathParam("propertyName") String propertyName
    ) throws XWikiRestException;
}
