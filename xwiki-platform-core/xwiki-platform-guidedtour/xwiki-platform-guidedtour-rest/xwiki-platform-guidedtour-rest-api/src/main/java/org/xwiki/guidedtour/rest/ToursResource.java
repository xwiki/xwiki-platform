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
package org.xwiki.guidedtour.rest;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.guidedtour.api.dtos.TourDTO;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Exposes the guided tours through REST.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
@Path("/guidedTour/tours")
public interface ToursResource extends XWikiRestComponent
{
    /**
     * Returns the available guided tours.
     *
     * @return the list of available guided tours and 200 status code if the retrieval is successful, 401 if the user
     *     lacks rights or if the CSRF token is invalid and 500 if any other error occurs
     */
    @GET
    Response getAvailableTours() throws XWikiRestException;

    /**
     * Creates a new guided tour.
     *
     * @param tourDTO the tour data to update
     * @return 201 status code if the tour has been created successfully, 401 if the user lacks rights, 409 if the tour
     *     already exists and 500 if any other error occurs
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response createTour(TourDTO tourDTO) throws XWikiRestException;

    /**
     * Updates an existing guided tour.
     *
     * @param tourId the id of the tour to update
     * @param tourDTO the tour data to update
     * @return 200 status code if the tour has been updated successfully, 401 if the user lacks rights, 404 if the tour
     *     is not found and 500 if any other error occurs
     */
    @PUT
    @Path("/{tourId}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateTour(@PathParam("tourId") String tourId, TourDTO tourDTO) throws XWikiRestException;

    /**
     * Deletes an existing guided tour.
     *
     * @param tourId the id of the tour to delete
     * @return 200 status code if the tour has been deleted successfully, 401 if the user lacks rights, 404 if the tour
     *     is not found and 500 if any other error occurs
     */
    @DELETE
    @Path("/{tourId}")
    Response deleteTour(@PathParam("tourId") String tourId) throws XWikiRestException;
}
