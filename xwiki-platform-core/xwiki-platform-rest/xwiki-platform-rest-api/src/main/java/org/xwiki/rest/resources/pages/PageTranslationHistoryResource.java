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
package org.xwiki.rest.resources.pages;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.History;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/translations/{language}/history")
public interface PageTranslationHistoryResource
{
    /**
     * Returns the revision history of one translation of a page, one entry per stored version.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose translation history is returned, for example {@code WebHome}
     * @param language the locale of the translation whose history is returned, for example {@code fr} or {@code en}
     * @param start the 0-based index of the first revision to return, used together with {@code number} for
     *  pagination; defaults to {@code 0}
     * @param number the maximum number of revisions to return; when {@code null} the wiki's configured REST query
     *  limit is used, and a value that is negative or larger than that configured limit is rejected with a
     *  {@code 400} response
     * @param order the sort order applied to the revision date, either {@code asc} (oldest first) or {@code desc}
     *  (newest first, the default); any other value falls back to {@code desc}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name) in addition to the technical references, at some extra cost; defaults to {@code false}
     * @return the requested window of translation revisions, each carrying that version's number, date, author and
     *  comment
     * @throws XWikiRestException if the current user is not allowed to view the page or if the history cannot be
     *  retrieved
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET History getPageTranslationHistory(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") Integer number,
            @QueryParam("order") @DefaultValue("desc") String order,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
