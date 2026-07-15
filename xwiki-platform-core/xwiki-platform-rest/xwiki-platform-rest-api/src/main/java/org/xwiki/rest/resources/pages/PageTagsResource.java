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

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Tags;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/tags")
public interface PageTagsResource
{
    /**
     * Returns the tags currently associated with a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose tags are returned, for example {@code WebHome}
     * @return the tags of the page (an empty list when the page carries no tag object), each linked to the resource
     *  listing the pages sharing that tag
     * @throws XWikiRestException if the tags cannot be retrieved, for example the page does not exist
     */
    @GET Tags getPageTags(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName
    ) throws XWikiRestException;

    /**
     * Sets the tags associated with a page, replacing the existing ones with the supplied list.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose tags are set, for example {@code WebHome}
     * @param minorRevision when {@code true}, the change is saved as a minor version; when {@code null} (the default)
     *  or {@code false}, it is saved as a normal (major) version
     * @param tags the complete set of tags to store on the page; the page's existing tags are entirely replaced by
     *  this list (an empty list clears all tags)
     * @return a response with status {@code 202} holding the tags that were stored
     * @throws XWikiRestException if the current user is not allowed to edit the page or if the tags cannot be stored
     */
    @PUT Response setTags(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("minorRevision") Boolean minorRevision,
            Tags tags
    ) throws XWikiRestException;
}
