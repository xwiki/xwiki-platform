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

import java.util.List;

import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.Encoded;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.xwiki.rest.XWikiRestException;
import org.xwiki.rest.model.jaxb.Page;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}")
public interface PageResource
{
    /**
     * Return all the metadata and content for a given page resource.
     *
     * @param wikiName the wiki storing the page
     * @param spaceNames the space where the page is located in
     * @param pageName the name of the page
     * @param withPrettyNames also return the pretty name for various document information
     *  (like the author display name, etc), disabled by default
     * @param withObjects (since 7.3M1) also return the objects, disabled by default
     * @param withClass (since 7.3M1) also return the class, disabled by default
     * @param withAttachments (since 7.3M1) also return the attachments metadata, disabled by default
     * @param checkRights (since 18.1.0RC1) also return whether the user has the listed rights on the page,
     *  empty by default
     * @param supportedSyntaxes (since 18.2.0RC1) also return an HTML rendered version of the content
     *  if set and the page's syntax is not in the list, empty by default
     * @return the metadata and content of the page
     * @throws XWikiRestException if the user is not authorized
     */
    // Needs a lot of parameters to bind path and query parameters
    @SuppressWarnings({"checkstyle:ParameterNumber"})
    @GET Page getPage(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceNames,
            @PathParam("pageName") String pageName,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames,
            @QueryParam("objects") @DefaultValue("false") Boolean withObjects,
            @QueryParam("class") @DefaultValue("false") Boolean withClass,
            @QueryParam("attachments") @DefaultValue("false") Boolean withAttachments,
            @QueryParam("checkRight") List<String> checkRights,
            @QueryParam("supportedSyntax") List<String> supportedSyntaxes
    ) throws XWikiRestException;

    // FIXME: Write Javadoc describing the REST API parameters
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @PUT Response putPage(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceNames,
            @PathParam("pageName") String pageName,
            @QueryParam("minorRevision") Boolean minorRevision,
            Page page
    ) throws XWikiRestException;

    // FIXME: Write Javadoc describing the REST API parameters
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @DELETE void deletePage(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceNames,
            @PathParam("pageName") String pageName
    ) throws XWikiRestException;
}
