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
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Objects;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/classes/{className}/objects")
public interface AllObjectsForClassNameResource
{
    /**
     * Retrieves all the objects of a given class across the wiki.
     *
     * @param wikiName the identifier of the wiki to search in, for example {@code xwiki} for the main wiki
     * @param className the reference of the XClass whose objects are retrieved, for example {@code XWiki.XWikiUsers}
     * @param start the 0-based index of the first object to return, used together with {@code number} for pagination;
     *  defaults to {@code 0}
     * @param number the maximum number of objects to return; when {@code null} (the default) the wiki's configured REST
     *  query limit is used, and a value that is negative or larger than that configured limit is rejected with a
     *  {@code 400} response
     * @param order the ordering of the results; {@code date} orders by descending modification date, while any other
     *  value or {@code null} (the default) keeps the store's natural order
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name), at some extra cost; defaults to {@code false}
     * @return the objects of the given class found across the wiki that the current user is allowed to view, within the
     *  requested pagination window
     * @throws XWikiRestException if the objects cannot be retrieved from the store
     */
    @GET Objects getObjects(
            @PathParam("wikiName") String wikiName,
            @PathParam("className") String className,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") Integer number,
            @QueryParam("order") String order,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
