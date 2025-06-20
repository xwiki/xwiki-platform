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
package org.xwiki.rest.resources.wikis;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Pages;
import org.xwiki.stability.Unstable;

/**
 * The top level pages in the page hierarchy of a given wiki.
 *
 * @version $Id$
 * @since 16.4.0RC1
 */
@Path("/wikis/{wikiName}/children")
@Unstable
public interface WikiChildrenResource
{
    /**
     * Retrieve the child pages (i.e. top level pages) of a specified wiki, optionally filtered by a search string.
     *
     * @param wikiName the wiki to retrieve the top level pages from
     * @param offset the index of the first child page to return
     * @param limit the maximum number of child pages to return
     * @param search a search string to filter the child pages by name or title
     * @return the child pages that match the search criteria
     * @throws XWikiRestException if there was an error while retrieving the child pages
     */
    @GET Pages getChildren(
        @PathParam("wikiName") String wikiName,
        @QueryParam("offset") @DefaultValue("0") Integer offset,
        @QueryParam("limit") @DefaultValue("-1") Integer limit,
        @QueryParam("search") @DefaultValue("") String search
    ) throws XWikiRestException;
}
