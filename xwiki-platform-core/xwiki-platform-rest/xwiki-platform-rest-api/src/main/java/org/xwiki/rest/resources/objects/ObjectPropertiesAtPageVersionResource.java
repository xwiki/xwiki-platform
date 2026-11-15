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
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Properties;

/**
 * @version $Id$
 */
// @Path annotations have very long URI templates in some object-related resources
@SuppressWarnings("checkstyle:LineLength")
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/history/{version}/objects/{className}/{objectNumber}/properties")
public interface ObjectPropertiesAtPageVersionResource
{
    /**
     * Retrieves the properties of an object attached to a given version of a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the object, for example {@code WebHome}
     * @param version the page revision to read the object from, for example {@code 2.1}
     * @param className the reference of the XClass of the object, for example {@code XWiki.XWikiUsers}
     * @param objectNumber the number identifying the object among those of the same class on the page; object numbers
     *  are 0-based and assigned in creation order, so a value that no object carries yields a {@code 404} response
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name), at some extra cost; defaults to {@code false}
     * @return all the properties of the requested object as stored in the given page version
     * @throws XWikiRestException if the properties cannot be retrieved, for example the page or the given version does
     *  not exist
     */
    @GET Properties getObjectProperties(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("version") String version,
            @PathParam("className") String className,
            @PathParam("objectNumber") Integer objectNumber,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
