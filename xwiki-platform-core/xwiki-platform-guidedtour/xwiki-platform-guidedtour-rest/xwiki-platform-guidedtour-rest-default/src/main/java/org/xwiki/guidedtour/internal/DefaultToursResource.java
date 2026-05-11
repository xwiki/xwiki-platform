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
package org.xwiki.guidedtour.internal;

import java.util.List;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.xwiki.component.annotation.Component;
import org.xwiki.guidedtour.api.dtos.TourDTO;
import org.xwiki.guidedtour.rest.ToursResource;
import org.xwiki.rest.XWikiRestException;

/**
 * Default implementation of {@link ToursResource}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named("org.xwiki.guidedtour.internal.DefaultToursResource")
@Singleton
public class DefaultToursResource extends AbstractGuidedTourResource implements ToursResource
{
    @Inject
    private ToursManager toursManager;

    @Override
    public Response getAvailableTours() throws XWikiRestException
    {
        return execute("Tour API: retrieving all tours.", () -> {
            List<TourDTO> json = this.toursManager.getAllTours();
            return Response.ok(json).type(MediaType.APPLICATION_JSON_TYPE).build();
        });
    }

    @Override
    public Response createTour(TourDTO tourDTO) throws XWikiRestException
    {
        return execute("Tour API: creating new tour.", () -> {
            this.toursManager.createTour(tourDTO);
            return Response.status(Response.Status.CREATED).build();
        });
    }

    @Override
    public Response updateTour(String tourId, TourDTO tourDTO) throws XWikiRestException
    {
        return execute("Tour API: updating tour with id [{}].", () -> {
            if (!tourDTO.getId().equals(tourId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Path ID and Body ID mismatch").build();
            }
            this.toursManager.updateTour(tourDTO);
            return Response.ok().build();
        }, tourId);
    }

    @Override
    public Response deleteTour(String tourId) throws XWikiRestException
    {
        return execute("Tour API: removing tour with id [{}].", () -> {
            this.toursManager.deleteTour(tourId);
            return Response.ok().build();
        }, tourId);
    }
}
