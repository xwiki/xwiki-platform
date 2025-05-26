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
package org.xwiki.platform.security.requiredrights.rest;

import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRequiredRights;
import org.xwiki.security.requiredrights.rest.model.jaxb.DocumentRightsAnalysisResult;
import org.xwiki.stability.Unstable;

/**
 * Get the result of the required rights analysis of a page.
 *
 * @version $Id$
 * @since 17.4.0RC1
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/requiredRights")
@Unstable
public interface RequiredRightsRestResource
{
    /**
     * @param wiki the wiki of the document to get annotations for
     * @param spaceNames the space names of the document to get annotations for
     * @param page the name of the document to get annotation for
     * @return the result of the required rights analysis of a page
     * @throws XWikiRestException when failing to parse space
     */
    @GET
    DocumentRightsAnalysisResult analyze(@PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String page, @PathParam("wikiName") String wiki) throws XWikiRestException;

    /**
     * Updates the required rights configuration for a document.
     *
     * @param spaceNames the space names of the document for which the required rights will be updated
     * @param page the name of the document for which the required rights will be updated
     * @param wiki the wiki of the document for which the required rights will be updated
     * @param documentRequiredRights the new required rights configuration to be applied to the document
     * @return the updated configuration of the required rights for the specified document
     * @throws XWikiRestException if an error occurs while processing the update request
     */
    @PUT
    DocumentRequiredRights updateRequiredRights(@PathParam("spaceName") @Encoded String spaceNames,
        @PathParam("pageName") String page, @PathParam("wikiName") String wiki,
        DocumentRequiredRights documentRequiredRights) throws XWikiRestException;
}
