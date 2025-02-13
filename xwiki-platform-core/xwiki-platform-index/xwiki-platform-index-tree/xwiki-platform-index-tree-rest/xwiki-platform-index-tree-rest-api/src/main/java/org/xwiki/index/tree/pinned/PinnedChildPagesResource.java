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

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.Response;

import org.xwiki.index.tree.pinned.rest.model.jaxb.PinnedChildPage;
import org.xwiki.index.tree.pinned.rest.model.jaxb.PinnedChildPages;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;


@Unstable
@Path("/wikis/{wikiName}{spaceName : (/spaces/[^/]+)*}/pinnedChildPages")
public interface PinnedChildPagesResource
{
    /**
     * Returns the pinned child pages metadata.
     *
     * @param wikiName the name of the wiki
     * @param spaceName the space where to look for the pinned child pages
     * @return the list of pinned child pages.
     */
    @GET
    PinnedChildPages getPinnedChildPages(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName) throws XWikiRestException;

    @PUT
    Response addPinnedChildPages(@PathParam("wikiName") String wikiName,
        @PathParam("spaceName") String spaceName, PinnedChildPage pinnedChildPage) throws XWikiRestException;
}
