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
import type { TourStep, TourTask, TourTour } from "../tourData";
import type { TourTaskStatus } from "../tourTaskStatus";

/**
 * Present the public API of the logic used inside the Guided Tour UI.
 * It provides the operations and data to display Guided Tours. It is build to be shared by most of the UI elements of
 * a Guided Tour.
 * @since 18.4.0RC1
 * @beta
 */
export interface GuidedTourManager {
  /**
   * Get the Useful Links to display in the widget.
   */
  getUsefulLinks(): Promise<string[]>;

  /**
   * Get all tours available for the user.
   */
  getTours(): Promise<TourTour[]>;

  /**
   * Create a new tour.
   * @param tour - The tour data to create.
   */
  createTour(tour: TourTour): Promise<void>;

  /**
   * Delete a tour by its id.
   * @param tourId - The id of the tour to delete.
   */
  deleteTour(tourId: string): Promise<void>;

  /**
   * Update an existing tour.
   * @param tourId - The id of the tour to update.
   * @param tour - The updated tour data.
   */
  updateTour(tourId: string, tour: TourTour): Promise<void>;

  /**
   * Set the status of a task.
   * @param task - The task to set the status of
   * @param status - The new status
   */
  setTaskStatus(task: TourTask, status: TourTaskStatus): Promise<void>;

  /**
   * Start the task.
   * @param task - The task to start.
   * @param remember - Whether to resume the task, or to start it anew.
   */
  startTask(task: TourTask, remember: boolean): Promise<void>;

  /**
   * Get all steps of a task in a tour.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   */
  getSteps(tourId: string, taskId: string): Promise<TourStep[] | undefined>;

  /**
   * Delete a step from a task.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   * @param stepId - The id of the step to delete.
   */
  deleteStep(tourId: string, taskId: string, stepId: number): Promise<void>;

  /**
   * Create a new step in a task.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   * @param stepData - The step data to create.
   */
  createStep(tourId: string, taskId: string, stepData: TourStep): Promise<void>;

  /**
   * Update an existing step.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   * @param stepId - The id of the step to update.
   * @param stepData - The updated step data.
   */
  updateStep(
    tourId: string,
    taskId: string,
    stepId: number,
    stepData: TourStep,
  ): Promise<void>;

  /**
   * Get all tasks of a tour.
   * @param tourId - The id of the tour.
   */
  getTasks(tourId: string): Promise<TourTask[] | undefined>;

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

  /**
   * Update the status of a task.
   * @param tourId - The id of the tour the task belongs to.
   * @param taskId - The id of the task to update the status of.
   * @param status - The new status of the task.
   */
  saveTaskStatus(
    tourId: string,
    taskId: string,
    status: TourTaskStatus,
  ): Promise<void>;
  /**
   * Auto-start a task which was in progress (eg. after redirecting to a new page during task steps)
   */
  initExistingTask(): void;
}
