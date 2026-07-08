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
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/translations/{language}")
public interface PageTranslationResource
{
    // FIXME: Write Javadoc describing the REST API parameters
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @GET Page getPageTranslation(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;

    // FIXME: Write Javadoc describing the REST API parameters
    @SuppressWarnings("checkstyle:MissingJavadocMethod")
    @PUT Response putPageTranslation(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("minorRevision") Boolean minorRevision,
            Page page
    ) throws XWikiRestException;

    /**
     * Delete a page translation.
     *
     * @param wikiName the wiki storing the page
     * @param spaceName the space where the page is located in
     * @param pageName the name of the page
     * @param language the language of the translation to delete
     * @param skipRecycleBin (since 18.6.0RC1) when {@code true}, the page is deleted permanently instead of being sent
     *  to the recycle bin. This is only honored for advanced users and when the wiki configuration allows skipping the
     *  recycle bin; otherwise the page is sent to the recycle bin as usual. Defaults to {@code false}
     * @throws XWikiRestException if the user is not authorized or if the deletion fails
     */
    @DELETE void deletePageTranslation(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("skipRecycleBin") @DefaultValue("false") Boolean skipRecycleBin
    ) throws XWikiRestException;
}
