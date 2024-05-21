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
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/children")
public interface PageChildrenResource
{
    /**
     * Retrieve the child pages of a specified parent page, using the specified hierarchy, optionally filtered by a
     * search string.
     *
     * @param wikiName the wiki that contains the parent page
     * @param spaceName the space that contains the parent page
     * @param pageName the name of the parent page
     * @param start the index of the first child page to return
     * @param number the maximum number of child pages to return
     * @param withPrettyNames whether to include rendered page titles in the response (can slow down the response)
     * @param hierarchy the type of hierarchy to use when searching for child pages; for backwards compatibility, the
     *            default hierarchy used is "parentchild"; use "nestedpages" hierarchy if you want results that match
     *            the current XWiki UI
     * @param search a search string to filter the child pages by name or title
     * @return the child pages that match the search criteria
     * @throws XWikiRestException if there was an error while retrieving the child pages
     */
    @GET Pages getPageChildren(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @QueryParam("start") @DefaultValue("0") Integer start,
            @QueryParam("number") @DefaultValue("-1") Integer number,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames,
            @QueryParam("hierarchy") @DefaultValue("parentchild") String hierarchy,
            @QueryParam("search") @DefaultValue("") String search
    ) throws XWikiRestException;
}
