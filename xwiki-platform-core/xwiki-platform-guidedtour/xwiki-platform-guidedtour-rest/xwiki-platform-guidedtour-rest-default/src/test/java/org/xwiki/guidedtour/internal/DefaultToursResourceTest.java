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

import java.util.ArrayList;
import java.util.List;

import javax.inject.Provider;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mock;
import org.xwiki.container.Container;
import org.xwiki.container.servlet.ServletRequest;
import org.xwiki.csrf.CSRFToken;
import org.xwiki.guidedtour.api.dtos.TourDTO;
import org.xwiki.guidedtour.api.exceptions.DuplicatedIdException;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
import org.xwiki.job.JobException;
import org.xwiki.query.QueryException;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.AccessDeniedException;
import org.xwiki.security.authorization.ContextualAuthorizationManager;
import org.xwiki.security.authorization.Right;
import org.xwiki.test.LogLevel;
import org.xwiki.test.junit5.LogCaptureExtension;
import org.xwiki.test.junit5.mockito.ComponentTest;
import org.xwiki.test.junit5.mockito.InjectMockComponents;
import org.xwiki.test.junit5.mockito.MockComponent;

import com.xpn.xwiki.XWikiException;

import jakarta.servlet.http.HttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;

/**
 * Test of {@link DefaultToursResource}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@ComponentTest
class DefaultToursResourceTest
{
    private static final String CSRF_VALUE = "csrfToken";

    private final TourDTO tourDTO = new TourDTO("tourId", "name", true);

    @InjectMockComponents
    private DefaultToursResource defaultToursResource;

    @MockComponent
    private ToursManager toursManager;

    @MockComponent
    private ContextualAuthorizationManager contextualAuthorizationManager;

    @MockComponent
    private Provider<Container> containerProvider;

    @MockComponent
    private CSRFToken csrf;

    @RegisterExtension
    private LogCaptureExtension logCapture = new LogCaptureExtension(LogLevel.DEBUG);

    @Mock
    private Container container;

    @Mock
    private ServletRequest request;

    @Mock
    private HttpServletRequest httpServletRequest;

    @BeforeEach
    void setup()
    {
        when(containerProvider.get()).thenReturn(container);
        when(container.getRequest()).thenReturn(request);
        when(request.getParameter("csrf")).thenReturn(CSRF_VALUE);
        when(csrf.isTokenValid(CSRF_VALUE)).thenReturn(true);
        when(request.getRequest()).thenReturn(httpServletRequest);
        when(httpServletRequest.getHeader("xwiki-form-token")).thenReturn(CSRF_VALUE);
    }

    @Test
    void getAvailableTours() throws QueryException, XWikiException, InvalidIdException, XWikiRestException
    {
        List<TourDTO> tours = new ArrayList<>(2);
        tours.add(new TourDTO("id", "name", true));
        tours.add(new TourDTO("id2", "name2", false));
        when(toursManager.getAllTours()).thenReturn(tours);

        Response response = defaultToursResource.getAvailableTours();
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(tours, response.getEntity());
        assertEquals("Executing: Tour API: retrieving all tours.", logCapture.getMessage(0));
    }

    @Test
    void getAvailableToursInvalidCSRF()
    {
        when(csrf.isTokenValid(CSRF_VALUE)).thenReturn(false);

        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultToursResource.getAvailableTours();
        });
        assertEquals("Executing: Tour API: retrieving all tours.", logCapture.getMessage(0));
        assertEquals("Authorization error: Tour API: retrieving all tours.", logCapture.getMessage(1));
        assertEquals(401, exception.getResponse().getStatus());
    }

    @Test
    void getAvailableToursNoViewRights() throws AccessDeniedException
    {
        doThrow(new AccessDeniedException(Right.VIEW, null, null)).when(contextualAuthorizationManager)
            .checkAccess(Right.VIEW);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultToursResource.getAvailableTours();
        });
        assertEquals("Executing: Tour API: retrieving all tours.", logCapture.getMessage(0));
        assertEquals("Authorization error: Tour API: retrieving all tours.", logCapture.getMessage(1));
        assertEquals(401, exception.getResponse().getStatus());
    }

    @Test
    void createTour() throws XWikiRestException
    {
        Response response = defaultToursResource.createTour(tourDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Executing: Tour API: creating new tour.", logCapture.getMessage(0));
    }

    @Test
    void createTourDuplicated() throws XWikiException, DuplicatedIdException
    {
        doThrow(new DuplicatedIdException("duplicate id")).when(toursManager).createTour(tourDTO);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultToursResource.createTour(tourDTO);
        });
        assertEquals(Response.Status.CONFLICT.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Executing: Tour API: creating new tour.", logCapture.getMessage(0));
        assertEquals("Conflict: Tour API: creating new tour.", logCapture.getMessage(1));
    }

    @Test
    void updateTour() throws XWikiRestException
    {
        Response response = defaultToursResource.updateTour("tourId", tourDTO);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Executing: Tour API: updating tour with id [tourId].", logCapture.getMessage(0));
    }

    @Test
    void updateTourDifferentIds() throws XWikiRestException
    {
        Response response = defaultToursResource.updateTour("tourIdDif", tourDTO);
        assertEquals(Response.Status.BAD_REQUEST.getStatusCode(), response.getStatus());
        assertEquals("Path ID and Body ID mismatch", response.getEntity());
        assertEquals("Executing: Tour API: updating tour with id [tourIdDif].", logCapture.getMessage(0));
    }

    @Test
    void updateTourInvalidId() throws XWikiException, InvalidIdException
    {
        doThrow(new InvalidIdException("invalid id")).when(toursManager).updateTour(tourDTO);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultToursResource.updateTour("tourId", tourDTO);
        });
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Executing: Tour API: updating tour with id [tourId].", logCapture.getMessage(0));
        assertEquals("Resource not found: Tour API: updating tour with id [tourId].", logCapture.getMessage(1));
    }

    @Test
    void deleteTour() throws XWikiRestException
    {
        Response response = defaultToursResource.deleteTour("tourId");
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Executing: Tour API: removing tour with id [tourId].", logCapture.getMessage(0));
    }

    @Test
    void deleteTourError() throws XWikiException, InvalidIdException, JobException
    {
        doThrow(new RuntimeException("invalid id")).when(toursManager).deleteTour("tourId");
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultToursResource.deleteTour("tourId");
        });
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Executing: Tour API: removing tour with id [tourId].", logCapture.getMessage(0));
        assertEquals("Internal error: Tour API: removing tour with id [tourId].", logCapture.getMessage(1));
    }
}
