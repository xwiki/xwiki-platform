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
import org.xwiki.rest.model.jaxb.Pages;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages")
public interface PagesResource
{
    /**
     * Returns the list of pages directly contained in a given space, restricted to those the current user is
     * allowed to view.
     *
     * @param wikiName the identifier of the wiki containing the space, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) whose pages are listed; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param start the 0-based index of the first page to return, used together with {@code number} for pagination;
     *  defaults to {@code 0}
     * @param number the maximum number of pages to return; when {@code null} the wiki's configured REST query limit
     *  is used, and a value that is negative or larger than that configured limit is rejected with a {@code 400}
     *  response
     * @param parentFilterExpression filters the returned pages by their parent; when absent ({@code null}) no parent
     *  filtering is applied; the literal string {@code null} keeps only pages that have no parent; any other value is
     *  treated as a regular expression that must fully match the parent's prefixed full name (for example
     *  {@code xwiki:Main.WebHome})
     * @param order the ordering of the returned pages; the only recognized value is {@code date}, which sorts them by
     *  descending modification date; any other value (including absent) uses the default document-name ordering
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @return the viewable pages of the space, within the requested pagination window and matching the parent filter
     * @throws XWikiRestException if the pages cannot be retrieved
     */
    @GET Pages getPages(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") Integer number,
            @QueryParam("parentId") String parentFilterExpression,
            @QueryParam("order") String order,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
