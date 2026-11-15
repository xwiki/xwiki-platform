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
package org.xwiki.rest.resources.comments;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Comments;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/history/{version}/comments")
public interface CommentsVersionResource
{
    /**
     * Returns the comments of a page as they were in a given version of that page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose comments are listed, for example {@code WebHome}
     * @param version the revision of the page to read the comments from, for example {@code 2.1}
     * @param start the 0-based index of the first comment to return, used together with {@code number} for pagination;
     *  a negative value is treated as {@code 0} and a value past the last comment yields an empty result; defaults to
     *  {@code 0}
     * @param number the maximum number of comments to return; {@code -1} (the default) means no limit, that is all
     *  comments from {@code start} onwards
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the comment
     *  author's display name) in addition to the technical references, at some extra cost; defaults to {@code false}
     * @return the requested window of comments as stored in the given page version
     * @throws XWikiRestException if the comments cannot be retrieved, for example the page or the requested version
     *  does not exist or the current user is not allowed to view it
     */
    @GET Comments getCommentsVersion(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("version") String version,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("-1") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
