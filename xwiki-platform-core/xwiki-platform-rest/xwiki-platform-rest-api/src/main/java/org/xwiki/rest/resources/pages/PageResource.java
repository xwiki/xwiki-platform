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
     * Returns all the metadata and content for a given page resource.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceNames the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page to return, for example {@code WebHome}
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name and the document title) in addition to the technical references, at some extra cost; defaults to
     *  {@code false}
     * @param withObjects (since 7.3M1) when {@code true}, also returns the XObjects attached to the page; defaults to
     *  {@code false}
     * @param withClass (since 7.3M1) when {@code true}, also returns the XClass definition of the page; defaults to
     *  {@code false}
     * @param withAttachments (since 7.3M1) when {@code true}, also returns the attachments metadata; defaults to
     *  {@code false}
     * @param checkRights (since 18.1.0RC1) a repeatable parameter listing the rights to test for the current user on
     *  the page (for example {@code checkRight=edit&checkRight=delete}); each returned right reports whether it is
     *  granted; an unknown right name is rejected with a {@code 400} response; empty by default (no right is tested)
     * @param supportedSyntaxes (since 18.2.0RC1) a repeatable parameter listing the syntaxes the caller can render
     *  (for example {@code xwiki/2.1}); when non-empty and the page's own syntax is not in the list, an HTML rendered
     *  version of the content is also returned; empty by default (no HTML rendering is added)
     * @return the metadata and content of the page, enriched with the optionally requested objects, class,
     *  attachments, right checks and rendered content
     * @throws XWikiRestException if the current user is not allowed to view the page or if it cannot be retrieved
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

    /**
     * Creates the page if it does not exist yet, or updates it otherwise, from the supplied representation.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceNames the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page to create or update, for example {@code WebHome}
     * @param minorRevision when {@code true}, the change is saved as a minor version; when {@code null} (the default)
     *  or {@code false}, it is saved as a normal (major) version
     * @param page the page representation holding the content and metadata to store; its {@code comment} field is
     *  used as the version comment
     * @return a response holding the stored page, with status {@code 201} when the page was created, {@code 202} when
     *  an existing page was updated, or {@code 304} when the submitted data left the page unchanged
     * @throws XWikiRestException if the current user is not allowed to edit the page or if it cannot be stored
     */
    @PUT Response putPage(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceNames,
            @PathParam("pageName") String pageName,
            @QueryParam("minorRevision") Boolean minorRevision,
            Page page
    ) throws XWikiRestException;

    /**
     * Deletes a page.
     *
     * @param wikiName the identifier of the wiki storing the page, for example {@code xwiki} for the main wiki
     * @param spaceNames the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page to delete, for example {@code WebHome}
     * @param skipRecycleBin (since 18.6.0RC1) when {@code true}, the page is deleted permanently instead of being sent
     *  to the recycle bin. This is only honored for advanced users and when the wiki configuration allows skipping the
     *  recycle bin; otherwise the page is sent to the recycle bin as usual. Defaults to {@code false}
     * @throws XWikiRestException if the user is not authorized or if the deletion fails
     */
    @DELETE void deletePage(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceNames,
            @PathParam("pageName") String pageName,
            @QueryParam("skipRecycleBin") @DefaultValue("false") Boolean skipRecycleBin
    ) throws XWikiRestException;
}
