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
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Comment;
import org.xwiki.rest.model.jaxb.Comments;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/comments")
public interface CommentsResource
{
    /**
     * Returns the comments of a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose comments are listed, for example {@code WebHome}
     * @param start the 0-based index of the first comment to return, used together with {@code number} for pagination;
     *  a negative value is treated as {@code 0} and a value past the last comment yields an empty result; defaults to
     *  {@code 0}
     * @param number the maximum number of comments to return; {@code -1} (the default) means no limit, that is all
     *  comments from {@code start} onwards
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the comment
     *  author's display name) in addition to the technical references, at some extra cost; defaults to {@code false}
     * @return the requested window of comments of the page
     * @throws XWikiRestException if the comments cannot be retrieved, for example the page does not exist or the
     *  current user is not allowed to view it
     */
    @GET Comments getComments(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("-1") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;

    /**
     * Adds a new comment to a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page to add the comment to, for example {@code WebHome}; the page is created if
     *  it does not exist yet
     * @param comment the comment to add; only its {@code text}, {@code highlight} and {@code replyTo} fields are used,
     *  while the author and date are set server-side to the current user and the current time; {@code replyTo} holds
     *  the number of the comment being replied to
     * @return a {@code 201} response holding the location and the representation of the created comment, or
     *  {@code null} (a {@code 204} response) when the comment has neither text nor highlight and therefore nothing is
     *  saved
     * @throws XWikiRestException if the comment cannot be created, for example the current user is not allowed to edit
     *  the page
     */
    @POST Response postComment(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            Comment comment
    ) throws XWikiRestException;
}
