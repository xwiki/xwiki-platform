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
package org.xwiki.rest.resources.objects;

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
import org.xwiki.rest.model.jaxb.Object;

/**
 * @version $Id$
 */
@Path("/wikis/{wikiName}/spaces/{spaceName: .+}/pages/{pageName}/objects/{className}/{objectNumber}")
public interface ObjectResource
{
    /**
     * Retrieves an object attached to a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the object, for example {@code WebHome}
     * @param className the reference of the XClass of the object to retrieve, for example {@code XWiki.XWikiUsers}
     * @param objectNumber the number identifying the object among those of the same class on the page; object numbers
     *  are 0-based and assigned in creation order, so a value that no object carries yields a {@code 404} response
     * @param withPrettyNames when {@code true}, also computes human-readable display names (for example the author's
     *  display name), at some extra cost; defaults to {@code false}
     * @return the requested object
     * @throws XWikiRestException if the object cannot be retrieved from the store
     */
    @GET Object getObject(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("className") String className,
            @PathParam("objectNumber") Integer objectNumber,
            @QueryParam("prettyNames") @DefaultValue("false") Boolean withPrettyNames
    ) throws XWikiRestException;

    /**
     * Updates an object attached to a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the object, for example {@code WebHome}
     * @param className the reference of the XClass of the object to update, for example {@code XWiki.XWikiUsers}
     * @param objectNumber the number identifying the object among those of the same class on the page; object numbers
     *  are 0-based and assigned in creation order, so a value that no object carries yields a {@code 404} response
     * @param minorRevision when {@code true}, saves the change as a minor version; when {@code null} (the default) or
     *  {@code false}, saves it as a normal (major) version
     * @param object the new state of the object; its property values replace those of the stored object
     * @return a response with status {@code 202} holding the updated object
     * @throws XWikiRestException if the object cannot be updated, for example the current user is not allowed to edit
     *  the page
     */
    @PUT Response updateObject(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("className") String className,
            @PathParam("objectNumber") Integer objectNumber,
            @QueryParam("minorRevision") Boolean minorRevision,
            Object object
    ) throws XWikiRestException;

    /**
     * Deletes an object attached to a page.
     *
     * @param wikiName the identifier of the wiki containing the page, for example {@code xwiki} for the main wiki
     * @param spaceName the reference of the space(s) containing the page; nested spaces are separated by
     *  {@code /spaces/} (for example {@code A/spaces/B/spaces/C} for the space {@code A.B.C})
     * @param pageName the name of the page holding the object, for example {@code WebHome}
     * @param className the reference of the XClass of the object to delete, for example {@code XWiki.XWikiUsers}
     * @param objectNumber the number identifying the object among those of the same class on the page; object numbers
     *  are 0-based and assigned in creation order, so a value that no object carries yields a {@code 404} response
     * @throws XWikiRestException if the object cannot be deleted, for example the current user is not allowed to edit
     *  the page
     */
    @DELETE void deleteObject(
            @PathParam("wikiName") String wikiName,
            @PathParam("spaceName") @Encoded String spaceName,
            @PathParam("pageName") String pageName,
            @PathParam("className") String className,
            @PathParam("objectNumber") Integer objectNumber
    ) throws XWikiRestException;
}
