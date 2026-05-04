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
// TODO: These 3 should be exported together from some module.
import type { TourStep } from "./step";
import type { TourTask } from "./task";
import type { TourTour } from "./tour";
import type { TourTaskStatus } from "./tourTaskStatus";

/**
 * Present the public API of the logic used inside the Guided Tour UI.
 * It provides the operations and data to display Guided Tours. It is build to be shared by most of the UI elements of
 * a Guided Tour.
 * @since 18.4.0RC1
 * @beta
 */
export interface GuidedTourManagerApi {
  /**
   * Get the Useful Links to display in the widget.
   */
  getUsefulLinks(): Promise<string[]>;
  /**
   * Get all tours available for the user.
   */
  getTours(): Promise<TourTour[]>;
  /**
   * Set the status of a task.
   * @param task - The task to set the status of
   * @param status - The new status
   */
  setTaskStatus(task: TourTask, status: TourTaskStatus): Promise<void>;
  /**
   * Get all tasks of a tour, which are available for the user.
   */
  getTask(taskId: string, tourId?: string): Promise<TourTask | undefined>;
  /**
   * Get all tasks of a tour, which are available for the user.
   */
  getTasks(tourId: string): Promise<TourTask[] | undefined>;
  /**
   * Start the task.
   * @param task - The task to start.
   * @param remember - Whether to resume the task, or to start it anew.
   */
  startTask(task: TourTask, remember: boolean): Promise<void>;
  /**
   * Get all steps of a tour.
   */
  getSteps(tourId: string, taskId: string): Promise<TourStep[] | undefined>;
}
