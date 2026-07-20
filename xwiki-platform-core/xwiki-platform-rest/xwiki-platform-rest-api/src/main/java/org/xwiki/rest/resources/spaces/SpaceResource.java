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

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Space;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}")
public interface SpaceResource
{
    /**
     * Returns the metadata of a space, including a reference to its home page ({@code WebHome}) when that page exists.
     *
     * @param wikiName the identifier of the wiki storing the space, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) to return the metadata for; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @return the metadata of the space; the home page details are omitted when the space has no {@code WebHome} page
     * @throws XWikiRestException if the space metadata cannot be retrieved
     */
    @GET Space getSpace(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName
    ) throws XWikiRestException;
}
