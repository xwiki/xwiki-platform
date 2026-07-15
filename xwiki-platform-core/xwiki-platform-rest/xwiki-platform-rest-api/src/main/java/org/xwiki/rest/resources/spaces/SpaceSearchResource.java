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

import java.util.List;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.SearchResults;

/**
 * Resource for searching the content of a given space (and its nested spaces) using a keyword query.
 *
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/search")
public interface SpaceSearchResource
{
    /**
     * Searches the content of a given space (and its nested spaces) using the provided keywords.
     *
     * @param wikiName the identifier of the wiki storing the space, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) to search in; nested spaces are separated by {@code /spaces/}
     *  (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param keywords the text to search for, for example {@code release notes}
     * @param searchScopeStrings which parts of the entities to search, given as repeated {@code scope} values;
     *  accepted values (case-insensitive) are {@code CONTENT}, {@code NAME}, {@code TITLE}, {@code SPACES} and
     *  {@code OBJECTS}; unrecognized values are ignored and, when none is valid, {@code CONTENT} is used
     * @param number the maximum number of results to return; when {@code null} the wiki's configured REST query limit
     *  is used, and a value that is negative or larger than that limit is rejected with a {@code 400} response
     * @param start the 0-based index of the first result to return, used together with {@code number} for pagination;
     *  defaults to {@code 0}
     * @param orderField the name of the document field to order the results by, for example {@code fullName} or
     *  {@code name}; must be alphanumeric; when empty the results are ordered by {@code fullName} ascending
     * @param order the sort direction, {@code asc} or {@code desc}; any other value falls back to {@code asc};
     *  defaults to {@code asc}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name) in addition to the technical references, at some extra cost; defaults to {@code false}
     * @param isLocaleAware when {@code true}, takes the locale of the results into account when searching; defaults to
     *  {@code false}
     * @return the search results matching the given keywords, ordered as requested
     * @throws XWikiRestException if the search cannot be performed
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET SearchResults search(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @QueryParam("q") String keywords,
            @QueryParam("scope") List<String> searchScopeStrings,
            @QueryParam("number") Integer number,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("orderField") String orderField,
            @QueryParam("order") @DefaultValue("asc") String order,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames,
            @QueryParam("localeAware") @DefaultValue("false") Boolean isLocaleAware
    ) throws XWikiRestException;
}
