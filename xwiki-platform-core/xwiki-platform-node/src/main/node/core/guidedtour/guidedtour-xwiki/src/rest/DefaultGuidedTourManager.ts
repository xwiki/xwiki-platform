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

import { DefaultStepManagerApi } from "./DefaultStepManagerApi";
import { DefaultTaskManagerApi } from "./DefaultTaskManagerApi";
import { DefaultTourManagerApi } from "./DefaultTourManagerApi";
import { GuidedTourRestClient } from "./GuidedTourRestClient";
import { SessionStorageManager } from "../SessionStorageManager";
import { driver, getDriverConfigForSteps, wrapTask } from "../driverjsMain";
import { TourTaskStatus } from "@xwiki/platform-guidedtour-api";
import { DocumentReference } from "@xwiki/platform-model-api";
import type { TourStore } from "./TourStore";
import type {
  GuidedTourManager,
  TourStep,
  TourTask,
  TourTour,
} from "@xwiki/platform-guidedtour-api";
import type { Driver } from "driver.js";

/**
 * Facade that implements {@link GuidedTourManager} by delegating to the three
 * Default*ManagerApi classes and orchestrating driver.js tour playback.
 *
 * @since 18.4.0RC1
 * @beta
 */
export class DefaultGuidedTourManager implements GuidedTourManager {
  private defaultTourManagerApi: DefaultTourManagerApi;
  private defaultTaskManagerApi: DefaultTaskManagerApi;
  private defaultStepManagerApi: DefaultStepManagerApi;

  /**
   * The currently active driver.js instance, if a task is in progress.
   */
  activeDriverTask?: Driver;

  /**
   * The currently active task, if a task is in progress.
   */
  activeTask?: TourTask;

  /**
   * @param xm - A promise resolving to the xwiki-meta module (provides the CSRF form token).
   * @param sharedStore - Shared in-memory cache for tours, tasks, and steps.
   */
  // @ts-expect-error xm is any
  constructor(xm: Promise, sharedStore: TourStore) {
    const restClient = new GuidedTourRestClient(xm);
    this.defaultTourManagerApi = new DefaultTourManagerApi(
      restClient,
      sharedStore,
    );
    this.defaultTaskManagerApi = new DefaultTaskManagerApi(
      restClient,
      sharedStore,
    );
    this.defaultStepManagerApi = new DefaultStepManagerApi(
      restClient,
      sharedStore,
    );
  }

  async getTours(): Promise<TourTour[]> {
    return await this.defaultTourManagerApi.getTours();
  }

  async createTour(tour: TourTour): Promise<void> {
    await this.defaultTourManagerApi.createTour(tour);
  }

  async deleteTour(tourId: string): Promise<void> {
    await this.defaultTourManagerApi.deleteTour(tourId);
  }

  async updateTour(tourId: string, tour: TourTour): Promise<void> {
    await this.defaultTourManagerApi.updateTour(tourId, tour);
  }

  /**
   * Persist a task status to the server.
   * TODO: Implement server synchronisation.
   */
  async saveTaskStatus(
    tourId: string,
    taskId: string,
    status: TourTaskStatus,
  ): Promise<void> {
    console.log(tourId, taskId, status);
  }

  /**
   * Delete a step by delegating to {@link DefaultStepManagerApi}.
   */
  async deleteStep(
    tourId: string,
    taskId: string,
    stepId: number,
  ): Promise<void> {
    await this.defaultStepManagerApi.deleteStep(tourId, taskId, stepId);
  }

  /**
   * Update a step by delegating to {@link DefaultStepManagerApi}.
   */
  async updateStep(
    tourId: string,
    taskId: string,
    stepId: number,
    stepData: TourStep,
  ): Promise<void> {
    await this.defaultStepManagerApi.updateStep(
      tourId,
      taskId,
      stepId,
      stepData,
    );
  }

  /**
   * Create a step by delegating to {@link DefaultStepManagerApi}.
   */
  async createStep(
    tourId: string,
    taskId: string,
    stepData: TourStep,
  ): Promise<void> {
    await this.defaultStepManagerApi.createStep(tourId, taskId, stepData);
  }

  getTask(taskId: string, tourId: string): Promise<TourTask | undefined> {
    return this.defaultTaskManagerApi.getTask(taskId, tourId);
  }

  getTasks(tourId: string): Promise<TourTask[]> {
    return this.defaultTaskManagerApi.getTasks(tourId);
  }

  updateTask(
    tourId: string,
    taskId: string,
    taskData: TourTask,
  ): Promise<void> {
    return this.defaultTaskManagerApi.updateTask(tourId, taskId, taskData);
  }

  createTask(tourId: string, taskData: TourTask): Promise<void> {
    return this.defaultTaskManagerApi.createTask(tourId, taskData);
  }

  deleteTask(tourId: string, taskId: string): Promise<void> {
    return this.defaultTaskManagerApi.deleteTask(tourId, taskId);
  }

  /**
   * Get the sandbox space document reference name.
   * TODO: Make this configurable via Admin Settings.
   */
  getSandboxSpace(): Promise<string> {
    return Promise.resolve(
      new DocumentReference("GuidedTour.SandboxSpace").name,
    );
  }

  /**
   * Get useful links to display in the widget.
   */
  getUsefulLinks(): Promise<string[]> {
    const usefulLinks: string[] = [
      "<a>Useful link 1</a>",
      "<a>Useful link 2</a>",
    ];
    return Promise.resolve(usefulLinks);
  }

  /**
   * Start a guided tour task.
   * Fetches the steps, creates a driver.js instance, wraps it for session
   * persistence, and begins the tour at the remembered or first step.
   * @param task - The task to start.
   * @param remember - Whether to resume from a saved step index.
   */
  async startTask(task: TourTask, remember = true): Promise<void> {
    const steps = await this.getSteps(task.tourId!, task.id);
    let stepindex = 0;
    if (remember) {
      stepindex = Number.parseInt(
        window.sessionStorage.getItem(
          SessionStorageManager.getTaskStepStorageKey(task),
        ) ?? "0",
      );
    }
    this.setupStep(steps[stepindex]);
    const driverTour = driver(getDriverConfigForSteps(steps, task, this));
    SessionStorageManager.setStorageKey(
      SessionStorageManager.getActiveTaskStorageKey(),
      SessionStorageManager.getStorageKeyPrefix(task),
    );

    this.activeTask = task;
    this.activeDriverTask = wrapTask(driverTour, this);
    this.activeDriverTask.drive(stepindex);
  }

  /**
   * Reset a task to its initial state.
   * TODO: Implement actual reset logic.
   */
  async resetTask(task: TourTask): Promise<void> {
    console.log(task);
  }

  /**
   * TODO: Discuss why is this needed
   */
  setupStep(step: TourStep): void {
    console.log(step);
  }

  /**
   * Check session storage for an in-progress task and resume it.
   * Called on page load to recover tours that span multiple pages.
   */
  async initExistingTask() {
    const existingActiveTask = SessionStorageManager.getStorageKey(
      SessionStorageManager.getActiveTaskStorageKey(),
    );
    if (existingActiveTask) {
      const parsedIds =
        SessionStorageManager.parseStorageKeyPrefix(existingActiveTask);
      if (parsedIds !== undefined) {
        const task = await this.getTask(
          parsedIds["tourId"],
          parsedIds["taskId"],
        );
        if (task !== undefined) {
          this.startTask(task, true);
        } else {
          console.error(
            "Tried to get task for ",
            parsedIds,
            ", it didn't work.",
          );
        }
      } else {
        console.error("No good task parsing value:", parsedIds);
      }
    }
  }

  /**
   * Update the status of a task and clear the associated session storage keys.
   * @param task - The task whose status to change.
   * @param status - The new status (TODO, SKIPPED, or DONE).
   */
  async setTaskStatus(task: TourTask, status: TourTaskStatus): Promise<void> {
    task.status = status;
    SessionStorageManager.setStorageKey(
      SessionStorageManager.getTaskStepStorageKey(task),
      undefined,
    );
    SessionStorageManager.setStorageKey(
      SessionStorageManager.getActiveTaskStorageKey(),
      undefined,
    );
  }

  /**
   * Get all steps for a task by delegating to {@link DefaultStepManagerApi}.
   */
  async getSteps(tourId: string, taskId: string): Promise<TourStep[]> {
    const tourSteps: TourStep[] = await this.defaultStepManagerApi.getSteps(
      tourId,
      taskId,
    );
    return Promise.resolve(tourSteps);
  }
}
