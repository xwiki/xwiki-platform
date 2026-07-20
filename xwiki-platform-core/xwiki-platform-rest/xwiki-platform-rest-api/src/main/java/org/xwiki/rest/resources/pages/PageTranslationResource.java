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
    /**
     * Returns all the metadata and content for a given translation of a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose translation is returned, for example {@code WebHome}
     * @param language the locale of the translation to return, for example {@code fr} or {@code en}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @return the metadata and content of the requested page translation
     * @throws XWikiRestException if the current user is not allowed to view the page or if the translation cannot be
     *  retrieved
     */
    @GET Page getPageTranslation(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;

    /**
     * Creates the translation if it does not exist yet, or updates it otherwise, from the supplied representation.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose translation is created or updated, for example {@code WebHome}
     * @param language the locale of the translation to create or update, for example {@code fr} or {@code en}
     * @param minorRevision when {@code true}, the change is saved as a minor version; when {@code null} (the default)
     *  or {@code false}, it is saved as a normal (major) version
     * @param page the page representation holding the content and metadata to store; its {@code comment} field is
     *  used as the version comment
     * @return a response holding the stored translation, with status {@code 201} when the translation was created,
     *  {@code 202} when an existing translation was updated, or {@code 304} when the submitted data left it unchanged
     * @throws XWikiRestException if the current user is not allowed to edit the page or if the translation cannot be
     *  stored
     */
    @PUT Response putPageTranslation(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("minorRevision") Boolean minorRevision,
            Page page
    ) throws XWikiRestException;

    /**
     * Deletes one translation of a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page whose translation is deleted, for example {@code WebHome}
     * @param language the locale of the translation to delete, for example {@code fr} or {@code en}
     * @param skipRecycleBin (since 18.6.0RC1) when {@code true}, the page is deleted permanently instead of being sent
     *  to the recycle bin. This is only honored for advanced users and when the wiki configuration allows skipping the
     *  recycle bin; otherwise the page is sent to the recycle bin as usual. Defaults to {@code false}
     * @throws XWikiRestException if the current user is not allowed to delete the page or if the deletion fails
     */
    @DELETE void deletePageTranslation(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("language") String language,
            @QueryParam("skipRecycleBin") @DefaultValue("false") Boolean skipRecycleBin
    ) throws XWikiRestException;
}
