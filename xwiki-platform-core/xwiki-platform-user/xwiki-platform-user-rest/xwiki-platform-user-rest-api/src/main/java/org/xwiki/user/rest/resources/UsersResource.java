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

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;
import org.xwiki.user.rest.model.jaxb.Users;

/**
 * Represent a list of users.
 *
 * @since 18.2.0RC1
 * @version $Id$
 */
@Unstable
@Path("/wikis/{wikiName}/users")
public interface UsersResource
{
    /**
     * Return the list of users on the wiki {wikiName}.
     *
     * @param wikiName the wiki from which to list the users
     * @param start a list offset
     * @param number a maximum number of entries to return
     * @return the list of users on the wiki {wikiName}
     * @throws XWikiRestException if there was an issue when compiling the list
     */
    @GET Users getUsers(
        @PathParam("wikiName") String wikiName,
        @QueryParam("start") @DefaultValue("0") Integer start,
        @QueryParam("number") Integer number
    ) throws XWikiRestException;
}
