/**
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
import type { TourTask } from "../tourData";

/**
 * Present the public API of the logic used inside the Guided Tour UI.
 * It provides the operations and data to display Guided Tours. It is build to be shared by most of the UI elements of
 * a Guided Tour.
 * @since 18.4.0RC1
 * @beta
 */
export interface TaskManagerApi {
  /**
   * Get all tasks for a tour.
   * @param tourId - The id of the tour.
   */
  getTasks(tourId: string): Promise<TourTask[]>;

  /**
   * Get a single task by id.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   */
  getTask(tourId: string, taskId: string): Promise<TourTask | undefined>;

  /**
   * Create a new task in a tour.
   * @param tourId - The id of the tour.
   * @param taskData - The task data to create.
   */
  createTask(tourId: string, taskData: TourTask): Promise<void>;

  /**
   * Delete a task from a tour.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task to delete.
   */
  deleteTask(tourId: string, taskId: string): Promise<void>;

  /**
   * Update an existing task.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task to update.
   * @param taskData - The updated task data.
   */
  updateTask(tourId: string, taskId: string, taskData: TourTask): Promise<void>;
}
