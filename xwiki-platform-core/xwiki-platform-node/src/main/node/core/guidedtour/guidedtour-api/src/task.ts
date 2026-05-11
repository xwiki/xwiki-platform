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
import type { TourStep } from "./step";
import type { TourTaskStatus } from "./tourTaskStatus";
/**
 * Representation of a guidedtour Task.
 *
 * @since 18.4.0RC1
 * @beta
 */
export interface TourTask {
  /**
   * The pretty name of the task, to be used in the UI.
   */
  title: string;
  /**
   * The task id, from the backend.
   */
  id: string;
  /**
   * Status of the task.
   */
  status: TourTaskStatus;
  /**
   * Whether this Task is completable or not.
   */
  active?: boolean;
  /**
   * The id of the tour this task belongs to, from the backend. This is useful to avoid having to pass the tour id separately when updating a task status.
   */
  tourId?: string;
  /**
   * The id of other tasks which must be completed before this task can be started.
   */
  dependsOn?: string[];
  /**
   * The order of the task in the tour.
   */
  order: number;
  /**
   * The steps which are part of this task.
   */
  steps?: TourStep[];
}
