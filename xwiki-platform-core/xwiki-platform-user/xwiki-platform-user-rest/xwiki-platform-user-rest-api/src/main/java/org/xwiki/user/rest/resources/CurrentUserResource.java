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
 * Represent the user currently logged-in.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@Unstable
@Path("/wikis/{wikiName}/user")
public interface CurrentUserResource
{
    /**
     * Return all the available information on the user making the request.
     * If the user is Guest (unauthenticated), only include id, name fields and avatar.
     *
     * @param wikiName the wiki to log in
     * @param preferences whether to include user preferences in the response
     * @return the information of the current user
     * @throws XWikiRestException if there was a problem fetching the data
     */
    @GET User getUser(
        @PathParam("wikiName") String wikiName,
        @QueryParam("preferences") boolean preferences
    ) throws XWikiRestException;
}
