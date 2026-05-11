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
package org.xwiki.guidedtour.api.dtos;

import java.util.HashMap;
import java.util.Map;

import org.xwiki.guidedtour.api.enums.Status;
import org.xwiki.guidedtour.api.enums.WidgetState;
import org.xwiki.stability.Unstable;

/**
 * User tour status DTO used to represent the status of the user progress and preferences regarding the guided tour.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
public class UserTourStatusDTO
{
    private WidgetState widgetState;

    private boolean callToAction;

    private Map<String, Status> tasksStatus;

    /**
     * Default constructor.
     */
    public UserTourStatusDTO()
    {
        this.tasksStatus = new HashMap<>();
    }

    /**
     * Constructor for UserTourStatusDTO.
     *
     * @param widgetState the state of the widget representing a value from the {@link WidgetState} enum
     * @param callToAction a boolean indicating whether the next action should be called automatically or not
     */
    public UserTourStatusDTO(String widgetState, boolean callToAction)
    {
        this.tasksStatus = new HashMap<>();
        this.widgetState = WidgetState.fromString(widgetState);
        this.callToAction = callToAction;
    }

    /**
     * Gets the tasks status.
     *
     * @return a map containing the task id as key and the task status as value, where the status is a value from the
     *     {@link Status} enum
     */
    public Map<String, Status> getTasksStatus()
    {
        return this.tasksStatus;
    }

    /**
     * Sets the tasks status.
     *
     * @param tasksStatus a map containing the task id as key and the task status as value, where the status is a
     *     value from the {@link Status} enum
     */
    public void setTasksStatus(Map<String, Status> tasksStatus)
    {
        this.tasksStatus = tasksStatus;
    }

    /**
     * Sets the task status for a specific task.
     *
     * @param taskId the id of the task
     * @param status the status of the task, which should be a value from the {@link Status} enum
     */
    public void setTaskStatus(String taskId, String status)
    {
        this.tasksStatus.put(taskId, Status.fromString(status));
    }

    /**
     * Removes the task status for a specific task.
     *
     * @param taskId the id of the task
     */
    public void removeTaskStatus(String taskId)
    {
        this.tasksStatus.remove(taskId);
    }

    /**
     * Gets the widget state.
     *
     * @return the state of the widget representing a value from the {@link WidgetState} enum
     */
    public WidgetState getWidgetState()
    {
        return this.widgetState;
    }

    /**
     * Sets the widget state.
     *
     * @param widgetState the state of the widget representing a value from the {@link WidgetState} enum
     */
    public void setWidgetState(String widgetState)
    {
        this.widgetState = WidgetState.fromString(widgetState);
    }

    /**
     * Checks if the next action should be called automatically or not.
     *
     * @return {@code true} if the next action should be called automatically, {@code false} otherwise
     */
    public boolean isCallToAction()
    {
        return this.callToAction;
    }

    /**
     * Sets whether the next action should be called automatically or not.
     *
     * @param callToAction {@code true} if the next action should be called automatically, {@code false} otherwise
     */
    public void setCallToAction(boolean callToAction)
    {
        this.callToAction = callToAction;
    }
}
