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
import org.xwiki.guidedtour.api.dtos.StepDTO;
import org.xwiki.guidedtour.api.exceptions.InvalidIdException;
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

@ComponentTest
class DefaultStepsResourceTest
{
    private static final String CSRF_VALUE = "csrfToken";

    private static final String TOUR_ID = "tourId";

    private static final String TASK_ID = "taskId";

    private final StepDTO stepDTO = new StepDTO();

    @InjectMockComponents
    private DefaultStepsResource defaultStepsResource;

    @MockComponent
    private StepsManager stepsManager;

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
    void getTaskSteps() throws XWikiException, InvalidIdException, XWikiRestException
    {
        List<StepDTO> steps = new ArrayList<>(2);
        steps.add(new StepDTO());
        steps.add(new StepDTO());
        when(stepsManager.getAllSteps(TOUR_ID, TASK_ID)).thenReturn(steps);

        Response response = defaultStepsResource.getTaskSteps(TOUR_ID, TASK_ID);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals(steps, response.getEntity());
        assertEquals("Executing: Steps API: retrieving the steps for task [taskId] from tour [tourId].",
            logCapture.getMessage(0));
    }

    @Test
    void createStep() throws XWikiRestException
    {
        Response response = defaultStepsResource.createStep(TOUR_ID, TASK_ID, stepDTO);
        assertEquals(Response.Status.CREATED.getStatusCode(), response.getStatus());
        assertEquals("Executing: Steps API: creating step for task [taskId] from tour [tourId].",
            logCapture.getMessage(0));
    }

    @Test
    void updateTask() throws XWikiRestException
    {
        Response response = defaultStepsResource.updateStep(TOUR_ID, TASK_ID, 2, stepDTO);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Executing: Steps API: updating step on position [2] for task [taskId] from tour [tourId].",
            logCapture.getMessage(0));
    }

    @Test
    void deleteTask() throws XWikiRestException
    {
        Response response = defaultStepsResource.deleteStep(TOUR_ID, TASK_ID, 2);
        assertEquals(Response.Status.OK.getStatusCode(), response.getStatus());
        assertEquals("Executing: Steps API: removing step on position [2] for task [taskId] from tour [tourId].",
            logCapture.getMessage(0));
    }

    @Test
    void deleteTaskError() throws AccessDeniedException
    {
        doThrow(new AccessDeniedException(Right.DELETE, null, null)).when(contextualAuthorizationManager)
            .checkAccess(Right.DELETE);
        WebApplicationException exception = assertThrows(WebApplicationException.class, () -> {
            defaultStepsResource.deleteStep(TOUR_ID, TASK_ID, 2);
        });
        assertEquals(Response.Status.UNAUTHORIZED.getStatusCode(), exception.getResponse().getStatus());
        assertEquals("Executing: Steps API: removing step on position [2] for task [taskId] from tour [tourId].",
            logCapture.getMessage(0));
        assertEquals(
            "Authorization error: Steps API: removing step on position [2] for task [taskId] from tour [tourId].",
            logCapture.getMessage(1));
    }
}
