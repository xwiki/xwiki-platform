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
import type { GuidedTourRestClient } from "./GuidedTourRestClient";
import type { TourStore } from "./TourStore";
import type { TaskManagerApi, TourTask } from "@xwiki/platform-guidedtour-api/";

/**
 * Default implementation of {@link TaskManagerApi} that talks to the XWiki REST API.
 * @since 18.4.0RC1
 * @beta
 */

export class DefaultTaskManagerApi implements TaskManagerApi {
  private sharedStore: TourStore;

  constructor(
    private restClient: GuidedTourRestClient,
    sharedStore: TourStore,
  ) {
    this.sharedStore = sharedStore;
  }

  /**
   * Get all tasks for a tour. Returns cached data if available.
   */
  public async getTasks(tourId: string): Promise<TourTask[]> {
    const tasks = this.sharedStore.getTourTasks(tourId);

    if (tasks.length == 0) {
      return await this.prepareTasks(tourId);
    }

    return tasks;
  }

  /**
   * Get a single task by id (fetches tasks first if needed).
   */
  public async getTask(
    taskId: string,
    tourId: string,
  ): Promise<TourTask | undefined> {
    const tasks = await this.getTasks(tourId);
    return tasks.find((t) => t.id === taskId);
  }

  /**
   * Create a new task via the REST API.
   */
  async createTask(tourId: string, taskData: TourTask): Promise<void> {
    const url = this.getTasksUrl(tourId);
    await this.restClient.request<TourTask>(url, "POST", taskData);
  }

  /**
   * Delete a task via the REST API and remove it from the cache.
   */
  async deleteTask(tourId: string, taskId: string): Promise<void> {
    const url = this.getTasksUrl(tourId, taskId);
    await this.restClient.request(url, "DELETE");
    const tourTasks = this.sharedStore.cache.toursMap.get(tourId)?.tasksList;
    if (tourTasks) {
      const index = tourTasks.findIndex((t) => t.id === taskId);
      if (index !== -1) {
        tourTasks.splice(index, 1);
      }
    } else {
      this.sharedStore.clearCache();
    }
  }

  /**
   * Update a task via the REST API.
   */
  async updateTask(
    tourId: string,
    taskId: string,
    taskData: TourTask,
  ): Promise<void> {
    const url = this.getTasksUrl(tourId, taskId);
    await this.restClient.request<TourTask>(url, "PUT", taskData);
  }

  /**
   * Build the REST URL for tasks, optionally targeting a specific task.
   */
  private getTasksUrl(tourId: string, taskId?: string): string {
    let url = `${XWiki.contextPath}/rest/guidedTour/tours/${tourId}/tasks`;
    if (taskId !== undefined) {
      url += `/${taskId}`;
    }
    return url;
  }

  /**
   * Fetch tasks from the REST API and update the store.
   */
  private async prepareTasks(tourId: string) {
    const url = this.getTasksUrl(tourId);
    const tasks = await this.restClient.request<TourTask[]>(url, "GET");
    for (const task of tasks) {
      task.tourId = tourId;
    }
    this.sharedStore.updateTourTasks(tourId, tasks);
    return tasks;
  }
}
