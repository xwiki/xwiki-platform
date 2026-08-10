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
package org.xwiki.rest.resources.tags;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Pages;

/**
 * Resource for listing the pages that are tagged with one or more given tags.
 *
 * @version $Id$
 */
@Path("/wikis/{wikiName}/tags/{tagNames}")
public interface PagesForTagsResource
{
    /**
     * Returns the pages tagged with any of the given tags, with optional pagination.
     *
     * @param wikiName the identifier of the wiki whose tagged pages are listed, for example {@code xwiki} for the main
     *  wiki
     * @param tagNames one or more tag names separated by commas; a page is returned when it carries at least one of
     *  them (logical OR) and surrounding whitespace around each name is ignored, for example {@code holidays} or
     *  {@code holidays,photos}
     * @param start the 0-based index of the first matching page to return, used together with {@code number} for
     *  pagination; defaults to {@code 0}
     * @param number the maximum number of pages to return; when {@code null} the wiki's configured REST query limit is
     *  used, and a value that is negative or larger than that configured limit is rejected with a {@code 400} response
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @return the pages tagged with any of the given tags, ordered alphabetically by their full name
     * @throws XWikiRestException if the tagged pages cannot be retrieved
     */
    @GET Pages getTags(
            @PathParam("wikiName") String wikiName,
            @PathParam("tagNames") String tagNames,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
