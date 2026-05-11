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

import org.xwiki.stability.Unstable;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Tour DTO used to represent a tour with its properties and the list of tasks it contains.
 *
 * @version $Id$
 * @since 18.4.0RC1
 */
@Unstable
@JsonIgnoreProperties(ignoreUnknown = true)
public class TourDTO
{
    private String id;

    private String title;

    private boolean isActive;

    private List<TaskDTO> tasks;

    /**
     * Default constructor.
     */
    public TourDTO()
    {
        this.isActive = false;
        this.tasks = new ArrayList<>();
    }

    /**
     * Constructor for TourDTO.
     *
     * @param id the id of the tour
     * @param title the title of the tour
     * @param isActive {@code true} if the tour is active, {@code false} otherwise
     */
    public TourDTO(String id, String title, boolean isActive)
    {
        this.title = title;
        this.id = id;
        this.isActive = isActive;
        this.tasks = new ArrayList<>();
    }

    /**
     * Gets the active status of the tour.
     *
     * @return {@code true} if the tour is active, {@code false} otherwise
     */
    public boolean isActive()
    {
        return this.isActive;
    }

    /**
     * Sets the active status of the tour.
     *
     * @param active {@code true} to set the tour as active, {@code false} to set it as inactive
     */
    public void setActive(boolean active)
    {
        this.isActive = active;
    }

    /**
     * Gets the title of the tour.
     *
     * @return the title of the tour
     */
    public String getTitle()
    {
        return this.title;
    }

    /**
     * Sets the title of the tour.
     *
     * @param title the title to set for the tour
     */
    public void setTitle(String title)
    {
        this.title = title;
    }

    /**
     * Gets the id of the tour.
     *
     * @return the id of the tour
     */
    public String getId()
    {
        return this.id;
    }

    /**
     * Sets the id of the tour.
     *
     * @param id the id to set for the tour
     */
    public void setId(String id)
    {
        this.id = id;
    }

    /**
     * Adds a task to the tour.
     *
     * @param taskDTO the task to be added to the tour
     */
    public void addTask(TaskDTO taskDTO)
    {
        this.tasks.add(taskDTO);
    }

    /**
     * Sets the list of tasks for the tour.
     *
     * @param tasks the list of tasks to set for the tour
     */
    public void setTasks(List<TaskDTO> tasks)
    {
        this.tasks = tasks;
    }

    /**
     * Gets the list of tasks for the tour.
     *
     * @return the list of tasks for the tour
     */
    public List<TaskDTO> getTasksList()
    {
        return this.tasks;
    }
}
