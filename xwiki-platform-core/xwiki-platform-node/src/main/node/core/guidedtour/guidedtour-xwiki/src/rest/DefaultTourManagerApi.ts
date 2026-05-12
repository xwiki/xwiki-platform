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

// @ts-expect-error this is a JavaScript file, it is expected to not have types.
import { XWiki } from "../services/xwiki.js";
import { TourTaskStatus } from "@xwiki/platform-guidedtour-api";
import type { GuidedTourRestClient } from "./GuidedTourRestClient";
import type { TourStore } from "./TourStore";
import type {
  TourManagerApi,
  TourTask,
  TourTour,
} from "@xwiki/platform-guidedtour-api/";

/**
 * Default implementation of {@link TourManagerApi} that talks to the XWiki REST API.
 * @since 18.4.0RC1
 * @beta
 */
export class DefaultTourManagerApi implements TourManagerApi {
  private sharedStore: TourStore;

  constructor(
    private restClient: GuidedTourRestClient,
    sharedStore: TourStore,
  ) {
    this.sharedStore = sharedStore;
  }

  /**
   * Fetch all tours. Returns cached data if available.
   */
  async getTours(): Promise<TourTour[]> {
    if (this.sharedStore.cache.tours?.length > 0) {
      return this.sharedStore.cache.tours;
    }
    const tours = await this.restClient.request<TourTour[]>(
      this.getTasksUrl(),
      "GET",
    );
    this.computeToursStatus(tours ?? []);
    this.sharedStore.updateTours(tours ?? []);
    return this.sharedStore.cache.tours ?? [];
  }

  /**
   * Get a single tour by id from the cache (fetches all tours first if needed).
   */
  async getTour(tourId: string): Promise<TourTour | undefined> {
    if (this.sharedStore.cache.tours?.length == 0) {
      await this.getTours();
    }
    const toursMap = this.sharedStore.cache.toursMap;
    return Promise.resolve(toursMap!.get(tourId));
  }

  /**
   * Derive each tour's status from its tasks:
   * - SKIPPED if the tour has no tasks or any task is SKIPPED.
   * - TODO if any task is still TODO.
   * - DONE otherwise.
   */
  computeToursStatus(tours: TourTour[]) {
    for (const tour of tours) {
      if (tour.tasksList?.length == 0) {
        tour.status = TourTaskStatus.SKIPPED;
      } else if (
        tour.tasksList!.find(
          (t: TourTask) => t.status == TourTaskStatus.TODO,
        ) !== undefined
      ) {
        tour.status = TourTaskStatus.TODO;
      } else if (
        tour.tasksList!.find(
          (t: TourTask) => t.status == TourTaskStatus.SKIPPED,
        ) !== undefined
      ) {
        tour.status = TourTaskStatus.SKIPPED;
      } else {
        tour.status = TourTaskStatus.DONE;
      }
    }
  }

  /**
   * Create a new tour via the REST API.
   */
  async createTour(tour: TourTour): Promise<void> {
    console.debug("Creating tour", tour);
    await this.restClient.request(this.getTasksUrl(), "POST", tour);
  }

  /**
   * Delete a tour by id via the REST API.
   */
  async deleteTour(tourId: string): Promise<void> {
    console.debug("Deleting tour", tourId);
    await this.restClient.request(this.getTasksUrl(tourId), "DELETE");
  }

  /**
   * Update a tour via the REST API.
   */
  async updateTour(tourId: string, tour: TourTour): Promise<void> {
    console.debug("Updating tour", tour);
    await this.restClient.request(this.getTasksUrl(tourId), "PUT", tour);
  }

  /**
   * Build the REST URL for tours, optionally targeting a specific tour.
   */
  private getTasksUrl(tourId?: string): string {
    let url = `${XWiki.contextPath}/rest/guidedTour/tours`;
    if (tourId !== undefined) {
      url += `/${tourId}`;
    }
    return url;
  }
}
