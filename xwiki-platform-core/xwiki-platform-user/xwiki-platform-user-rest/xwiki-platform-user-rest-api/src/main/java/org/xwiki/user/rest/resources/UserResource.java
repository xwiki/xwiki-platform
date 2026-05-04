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
package org.xwiki.user.rest.resources;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;
import org.xwiki.user.rest.model.jaxb.User;

/**
 * Represent a user identified by an id.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@Unstable
@Path("/wikis/{wikiName}/users/{userId}")
public interface UserResource
{
    /**
     * Return all the available information on the user {userId}.
     * This requires the logged-in user to have read access on the profile of {userId}.
     *
     * @param wikiName the wiki used to resolve the id to a user
     * @param userId the id of the user
     * @param preferences whether to include user preferences in the response
     * @return the information of the user identified by {userId}
     * @throws XWikiRestException if no user could be found or if the requester is missing rights to read the info
     */
    @GET User getUser(
        @PathParam("wikiName") String wikiName,
        @PathParam("userId") String userId,
        @QueryParam("preferences") boolean preferences
    ) throws XWikiRestException;
}
