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
package org.xwiki.rest.resources.objects;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Object;
import org.xwiki.rest.model.jaxb.Objects;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/objects")
public interface ObjectsResource
{
    /**
     * Retrieves the objects attached to a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the objects, for example {@code WebHome}
     * @param start the 0-based index of the first object to return, used together with {@code number} for pagination; a
     *  negative value is treated as {@code 0}; defaults to {@code 0}
     * @param number the maximum number of objects to return; {@code -1} (the default), or any other negative value,
     *  returns all of them
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name), at some extra cost; defaults to {@code false}
     * @return the requested window of objects attached to the page, across all their classes
     * @throws XWikiRestException if the objects cannot be retrieved from the store
     */
    @GET Objects getObjects(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("-1") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;

    /**
     * Adds a new object to a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page to add the object to, for example {@code WebHome}
     * @param minorRevision when {@code true}, saves the change as a minor version; when {@code null} (the default) or
     *  {@code false}, saves it as a normal (major) version
     * @param object the object to add; its {@code className} must be set to the XClass reference of the object to
     *  create (for example {@code XWiki.XWikiUsers}), and its number is assigned automatically
     * @return a response with status {@code 201} whose {@code Location} header points to the created object and whose
     *  body holds it
     * @throws XWikiRestException if the object cannot be added, for example the current user is not allowed to edit the
     *  page
     */
    @POST Response addObject(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("minorRevision") Boolean minorRevision,
            Object object
    ) throws XWikiRestException;
}
