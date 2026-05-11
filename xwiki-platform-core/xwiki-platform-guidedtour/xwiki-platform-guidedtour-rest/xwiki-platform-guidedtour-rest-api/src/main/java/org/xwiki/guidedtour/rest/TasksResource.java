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

import org.xwiki.guidedtour.api.dtos.TaskDTO;
import org.xwiki.rest.XWikiRestComponent;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.stability.Unstable;

/**
 * Exposes the guided tour tasks through REST API.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
@Path("/guidedTour/tours/{tourId}/tasks")
public interface TasksResource extends XWikiRestComponent
{
    /**
     * Returns the list of tasks for a given tour.
     *
     * @param tourId the tour id
     * @return the list of tasks for the given tour and 200 status code if the retrieval is successful, 401 if the user
     *     lacks rights or if the CSRF token is invalid and 500 if any other error occurs
     * @throws XWikiRestException
     */
    @GET
    Response getTourTasks(@PathParam("tourId") String tourId) throws XWikiRestException;

    /**
     * Returns a specific task of a given tour.
     *
     * @param tourId the tour id
     * @param taskId the task id
     * @return the task and 200 status code if the retrieval is successful, 404 if the task is not found, 401 if the
     *     user lacks rights or if the CSRF token is invalid and 500 if any other error occurs
     */
    @GET
    @Path("/{taskId}")
    Response getTourTask(@PathParam("tourId") String tourId, @PathParam("taskId") String taskId)
        throws XWikiRestException;

    /**
     * Creates a new task for a given tour.
     *
     * @param tourId the tour id
     * @param taskDTO the task data to create
     * @return 201 status code if the task has been created successfully, 401 if the user lacks rights, 404 if the tour
     *     is not found, 409 if the task already exists and 500 if any other error occurs
     */
    @POST
    @Consumes(MediaType.APPLICATION_JSON)
    Response createTask(@PathParam("tourId") String tourId, TaskDTO taskDTO) throws XWikiRestException;

    /**
     * Updates an existing task of a given tour.
     *
     * @param tourId the tour id
     * @param taskId the task id
     * @param taskDTO the task data to update
     * @return 200 status code if the task has been updated successfully, 401 if the user lacks rights, 404 if the tour
     *     or the task is not found and 500 if any other error occurs
     */
    @PUT
    @Path("/{taskId}")
    @Consumes(MediaType.APPLICATION_JSON)
    Response updateTask(@PathParam("tourId") String tourId, @PathParam("taskId") String taskId, TaskDTO taskDTO)
        throws XWikiRestException;

    /**
     * Deletes an existing task of a given tour.
     *
     * @param tourId the tour id
     * @param taskId the task id
     * @return 200 status code if the task has been deleted successfully, 401 if the user lacks rights, 404 if the tour
     *     or the task is not found and 500 if any other error occurs
     */
    @DELETE
    @Path("/{taskId}")
    Response deleteTask(@PathParam("tourId") String tourId, @PathParam("taskId") String taskId)
        throws XWikiRestException;
}
