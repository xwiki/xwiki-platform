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

import type { TourStep } from "../tourData";

/**
 * Present the public API of the logic used inside the Guided Tour UI.
 * It provides the operations and data to display Guided Tours. It is build to be shared by most of the UI elements of
 * a Guided Tour.
 * @since 18.4.0RC1
 * @beta
 */
export interface StepManagerApi {
  /**
   * Get all steps for a task in a tour.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   */
  getSteps(tourId: string, taskId: string): Promise<TourStep[]>;

  /**
   * Create a new step in a task.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   * @param stepData - The step data to create.
   */
  createStep(tourId: string, taskId: string, stepData: TourStep): Promise<void>;

  /**
   * Delete a step from a task.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   * @param stepId - The id of the step to delete.
   */
  deleteStep(tourId: string, taskId: string, stepId: number): Promise<void>;

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
}
