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
package org.xwiki.rest.resources.spaces;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Spaces;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces")
public interface SpacesResource
{
    /**
     * Lists the spaces of a given wiki that the current user is allowed to view.
     *
     * @param wikiName the identifier of the wiki to list the spaces from, for example {@code xwiki} for the main wiki
     * @param start the 0-based index of the first space to return, used together with {@code number} for pagination;
     *  defaults to {@code 0}
     * @param number the maximum number of spaces to return; when {@code null} the wiki's configured REST query limit
     *  is used, and a value that is negative or larger than that limit is rejected with a {@code 400} response
     * @return the spaces the current user is allowed to view (hidden spaces excluded), within the requested pagination
     *  window
     * @throws XWikiRestException if the spaces cannot be retrieved
     */
    @GET Spaces getSpaces(
            @PathParam("wikiName") String wikiName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") Integer number
    ) throws XWikiRestException;
}
