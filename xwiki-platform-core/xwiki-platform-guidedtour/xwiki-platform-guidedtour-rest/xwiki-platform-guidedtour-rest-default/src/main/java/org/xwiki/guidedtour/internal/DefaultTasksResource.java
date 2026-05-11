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
import org.xwiki.guidedtour.api.dtos.TaskDTO;
import org.xwiki.guidedtour.rest.TasksResource;
import org.xwiki.rest.XWikiRestException;
import org.xwiki.security.authorization.Right;

/**
 * Default implementation of {@link TasksResource}.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Component
@Named("org.xwiki.guidedtour.internal.DefaultTasksResource")
@Singleton
public class DefaultTasksResource extends AbstractGuidedTourResource implements TasksResource
{
    @Inject
    private TasksManager tasksManager;

    @Override
    public Response getTourTasks(String tourId) throws XWikiRestException
    {
        return execute("Tasks API: retrieving the tasks for tour [{}].", () -> {
            List<TaskDTO> tasks = this.tasksManager.getAllTasks(tourId);
            return Response.ok(tasks).type(MediaType.APPLICATION_JSON_TYPE).build();
        }, tourId);
    }

    @Override
    public Response getTourTask(String tourId, String taskId) throws XWikiRestException
    {
        return execute("Tasks API: retrieving the task [{}] from tour [{}].", () -> {
            TaskDTO task = this.tasksManager.getTask(tourId, taskId);
            return Response.ok(task).type(MediaType.APPLICATION_JSON_TYPE).build();
        }, taskId, tourId);
    }

    @Override
    public Response createTask(String tourId, TaskDTO taskDTO) throws XWikiRestException
    {
        return execute("Tasks API: creating task [{}] for tour [{}].", () -> {
            this.tasksManager.createTask(tourId, taskDTO);
            return Response.status(Response.Status.CREATED).build();
        }, taskDTO.getId(), tourId);
    }

    @Override
    public Response updateTask(String tourId, String taskId, TaskDTO taskDTO) throws XWikiRestException
    {
        return execute("Tasks API: updating task [{}] from tour [{}].", () -> {
            if (!taskDTO.getId().equals(taskId)) {
                return Response.status(Response.Status.BAD_REQUEST).entity("Path and Body ID mismatch for given task.")
                    .build();
            }
            this.tasksManager.updateTask(tourId, taskDTO);
            return Response.ok().build();
        }, taskDTO.getId(), tourId);
    }

    @Override
    public Response deleteTask(String tourId, String taskId) throws XWikiRestException
    {
        return execute("Tasks API: removing task [{}] from tour [{}].", () -> {
            this.contextualAuthorizationManager.checkAccess(Right.DELETE);
            this.tasksManager.deleteTask(tourId, taskId);
            return Response.ok().build();
        }, taskId, tourId);
    }
}
