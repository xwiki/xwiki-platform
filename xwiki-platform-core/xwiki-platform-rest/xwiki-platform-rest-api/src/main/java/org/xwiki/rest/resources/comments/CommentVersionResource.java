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
import org.xwiki.rest.model.jaxb.Comment;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/history/{version}/comments/{id}")
public interface CommentVersionResource
{
    /**
     * Returns a specific comment as it was in a given version of a page, identified by its comment id.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the comment, for example {@code WebHome}
     * @param version the revision of the page to read the comment from, for example {@code 2.1}
     * @param id the number identifying the comment to retrieve, that is its 0-based object number in that page version
     *  (for example {@code 0} for the first comment); a {@code 404} response is returned when that version has no
     *  comment with this number
     * @param start has no effect when retrieving a single comment: the comment is always located by {@code id}
     *  regardless of this value; defaults to {@code 0}
     * @param number has no effect when retrieving a single comment: the comment is always located by {@code id}
     *  regardless of this value; defaults to {@code -1}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the comment
     *  author's display name) in addition to the technical references, at some extra cost; defaults to {@code false}
     * @return the matching comment as stored in the requested page version
     * @throws XWikiRestException if the comment cannot be retrieved, for example the page or the requested version does
     *  not exist or the current user is not allowed to view it
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings("checkstyle:ParameterNumber")
    @GET Comment getCommentVersion(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("version") String version,
            @PathParam("id") Integer id,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("-1") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;
}
