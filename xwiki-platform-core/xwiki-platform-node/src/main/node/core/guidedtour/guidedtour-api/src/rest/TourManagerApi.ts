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
import type { TourTour } from "../tourData";

/**
 * Present the public API of the logic used inside the Guided Tour UI.
 * It provides the operations and data to display Guided Tours. It is build to be shared by most of the UI elements of
 * a Guided Tour.
 * @since 18.4.0RC1
 * @beta
 */
export interface TourManagerApi {
  /**
   * Get all tours available for the user.
   */
  getTours(): Promise<TourTour[]>;

  /**
   * Get a single tour by id.
   * @param tourId - The id of the tour.
   */
  getTour(tourId: string): Promise<TourTour | undefined>;

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
   * @param tourId - The tour id.
   * @param tour - The updated tour data.
   */
  updateTour(tourId: string, tour: TourTour): Promise<void>;
}
