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
package org.xwiki.rest.resources;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.History;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/modifications")
public interface ModificationsResource
{
    /**
     * Returns the modifications (individual document versions) made in a wiki strictly after a given date, ordered by
     * modification date.
     *
     * @param wikiName the identifier of the wiki to look for modifications in, for example {@code xwiki} for the main
     *  wiki
     * @param start the 0-based index of the first modification to return, used together with {@code number} for
     *  pagination; defaults to {@code 0}
     * @param number the maximum number of modifications to return; defaults to {@code 25}, and a value that is negative
     *  or larger than the wiki's configured REST query limit is rejected with a {@code 400} response
     * @param order the sort order on the modification date, either {@code asc} (oldest first) or {@code desc} (most
     *  recent first); any other value falls back to {@code desc}; defaults to {@code desc}
     * @param ts only return modifications made strictly after this instant, expressed in milliseconds since the epoch
     *  (1970-01-01T00:00:00Z), for example {@code 1704067200000}; passed through the query parameter named
     *  {@code date} and defaulting to {@code 0}, which returns every modification since the epoch
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @return the modifications matching the given criteria that the current user is allowed to view, ordered by
     *  modification date; modifications on documents the user cannot view are silently omitted
     * @throws XWikiRestException if the modifications cannot be retrieved, for example the underlying query fails
     */
    @GET History getModifications(
            @PathParam("wikiName") String wikiName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("25") Integer number,
            @QueryParam("order") @DefaultValue("desc") String order,
            @QueryParam("date") @DefaultValue("0") Long ts,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
