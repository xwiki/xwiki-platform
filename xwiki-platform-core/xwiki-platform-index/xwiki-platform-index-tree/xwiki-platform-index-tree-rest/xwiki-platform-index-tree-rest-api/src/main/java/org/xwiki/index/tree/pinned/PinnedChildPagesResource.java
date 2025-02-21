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
package org.xwiki.index.tree.pinned;

import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * REST resource dedicated for manipulation of pinned pages.
 *
 * @version $Id$
 * @since 17.2.0RC1
 * @since 16.10.5
 * @since 16.4.7
 */
@Unstable
@Path("/wikis/{wikiName}{spaceName : (/spaces/[^/]+)*}/pinnedChildPages")
public interface PinnedChildPagesResource
{
    /**
     * Returns the pinned child pages metadata.
     *
     * @param wikiName the name of the wiki
     * @param spaceName the space where to look for the pinned child pages
     * @return the list of pinned child pages as serialized references.
     * @throws XWikiRestException in case of problem when accessing the information
     */
    @GET
    List<String> getPinnedChildPages(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName) throws XWikiRestException;

    /**
     * Set a new pinned page value.
     *
     * @param wikiName the name of the wiki
     * @param spaceName the space where to look for the pinned child pages
     * @param pinnedChildPages the list of pinned child pages
     * @return an http response with the status of the operation
     * @throws XWikiRestException in case of problem when accessing the information
     */
    @POST
    Response setPinnedChildPages(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName, List<String> pinnedChildPages)
        throws XWikiRestException;
}
