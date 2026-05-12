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
import type {
  TourStep,
  TourTask,
  TourTour,
} from "@xwiki/platform-guidedtour-api";
/**
 * Holds the cached tours in an array and a map keyed by tour id.
 * @since 18.4.0RC1
 * @beta
 */
export interface TourCache {
  tours: TourTour[];
  toursMap: Map<string, TourTour>;
}
/**
 * In-memory cache for tours, tasks, and steps fetched from the REST API.
 * Provides read access via the {@link cache} getter and mutation methods
 * that keep the array and the map in sync. On integrity failures the
 * entire cache is purged to avoid stale data.
 * @since 18.4.0RC1
 * @beta
 */
export class TourStore {
  private _cache: TourCache = {
    tours: [],
    toursMap: new Map(),
  };

  /**
   * Read-only access to the current cache state.
   */
  public get cache(): Readonly<TourCache> {
    return this._cache;
  }

  /**
   * Replace the full tour list, rebuild the map, and set {@link TourTask#tourId}
   * on every nested task.
   */
  public updateTours(tours: TourTour[]) {
    this.populateTourTasks(tours);
    this._cache.tours = tours;
    this._cache.toursMap = new Map(tours.map((t) => [t.id, t]));
  }

  /**
   * Get the tasks of a tour from the cache, or an empty array if the tour is unknown.
   * @param tourId - The id of the tour.
   */
  public getTourTasks(tourId: string): TourTask[] {
    // FIXME: What if we need to fetch the cache in this step? (i.e. a valid tour is not in the cache)
    return this._cache.toursMap.get(tourId)?.tasksList ?? [];
  }

  /**
   * Replace the task list of a tour and keep the array / map in sync.
   * If the tour is not found the entire cache is purged.
   * @param tourId - The id of the tour.
   * @param tasks - The new task list.
   */
  public updateTourTasks(tourId: string, tasks: TourTask[]) {
    const tour = this._cache.toursMap.get(tourId);
    if (tour) {
      // Update the reference (since it's an object, it updates in the array too)
      this.setupTasks(tasks, tourId);
      tour.tasksList = tasks;
    } else {
      console.warn(
        `Tour ${tourId} not found in store. Purging cache to ensure integrity.`,
      );
      this.clearCache();
    }
  }

  /**
   * Set {@link TourTask#tourId} on all tasks in every tour.
   */
  private populateTourTasks(tours: TourTour[]) {
    for (const tour of tours) {
      this.setupTasks(tour.tasksList ?? [], tour.id);
    }
  }

  /**
   * Assign the tour id to each task so that tasks know their parent tour.
   */
  private setupTasks(tasks: TourTask[], tourId: string) {
    for (const task of tasks) {
      task.tourId = tourId;
    }
  }

  /**
   * Find a task by tour and task id. If either is missing, purge the cache.
   * @returns The task if found, otherwise undefined.
   */
  private getTaskOrPurge(tourId: string, taskId: string): TourTask | undefined {
    const tour = this._cache.toursMap.get(tourId);
    const task = tour?.tasksList?.find((t) => t.id === taskId);

    if (!tour || !task) {
      console.warn(
        `Integrity failure: Tour ${tourId} or Task ${taskId} missing. Purging cache.`,
      );
      this.clearCache();
      return undefined;
    }
    return task;
  }

  /**
   * Replace the step list of a task.
   * @param tourId - The id of the tour.
   * @param taskId - The id of the task.
   * @param steps - The new step list.
   */
  public updateTaskSteps(
    tourId: string,
    taskId: string,
    steps: TourStep[],
  ): void {
    const task = this.getTaskOrPurge(tourId, taskId);
    if (task) {
      task.steps = steps;
    }
  }

  /**
   * Empty the cache entirely.
   */
  public clearCache() {
    this._cache.tours = [];
    this._cache.toursMap.clear();
  }
}
