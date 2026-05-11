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

import java.util.ArrayList;
import java.util.List;

import org.xwiki.guidedtour.api.enums.Status;
import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Task DTO used to represent a task with its properties and the list of tasks it depends on.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
@JsonIgnoreProperties(ignoreUnknown = true)
public class TaskDTO
{
    private String id;

    private String title;

    private int order;

    private List<String> dependsOn;

    // TODO: to be set depending on user tour object
    private Status status = Status.TODO;

    private boolean isActive;

    /**
     * Default constructor.
     */
    public TaskDTO()
    {
        this.dependsOn = new ArrayList<>();
    }

    /**
     * Constructor for TaskDTO.
     *
     * @param id the id of the task
     * @param title the title of the task
     * @param order the order of the task in the tour
     * @param isActive {@code true} if the task is active, {@code false} otherwise
     * @param dependsOn the list of task ids that this task depends on
     */
    public TaskDTO(String id, String title, int order, boolean isActive, List<String> dependsOn)
    {
        this.id = id;
        this.title = title;
        this.order = order;
        this.dependsOn = dependsOn;
        this.isActive = isActive;
    }

    /**
     * Gets the active status of the task.
     *
     * @return {@code true} if the task is active, {@code false} otherwise
     */
    public boolean isActive()
    {
        return this.isActive;
    }

    /**
     * Sets the active status of the task.
     *
     * @param active {@code true} to set the task as active, {@code false} to set it as inactive
     */
    public void setActive(boolean active)
    {
        this.isActive = active;
    }

    /**
     * Gets the title of the task.
     *
     * @return the title of the task
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * Sets the title of the task.
     *
     * @param title the title of the task
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Gets the order of the task in the tour.
     *
     * @return the order of the task
     */
    public int getOrder()
    {
        return this.order;
    }

    /**
     * Sets the order of the task in the tour.
     *
     * @param order the order to set for the task
     */
    public void setOrder(int order)
    {
        this.order = order;
    }

    /**
     * Gets the id of the task.
     *
     * @return the id of the task
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Sets the id of the task.
     *
     * @param id the id to set for the task
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Gets the list of task ids that this task depends on.
     *
     * @return the list of task ids that this task depends on
     */
    public List<String> getDependsOn()
    {
        return this.dependsOn;
    }

    /**
     * Sets the list of task ids that this task depends on.
     *
     * @param dependsOn the list of task ids that this task depends on
     */
    public void setDependsOn(List<String> dependsOn)
    {
        this.dependsOn = dependsOn;
    }

    /**
     * Gets the status of the task.
     *
     * @return the status of the task, which is a value from the {@link Status} enum
     */
    public Status getStatus()
    {
        return this.status;
    }

    /**
     * Sets the status of the task.
     *
     * @param status the status to set for the task, which should be a value from the {@link Status} enum
     */
    public void setStatus(Status status)
    {
        this.status = status;
    }
}
