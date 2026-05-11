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
import type { StepManagerApi, TourStep } from "@xwiki/platform-guidedtour-api/";

/**
 * Default implementation of {@link StepManagerApi} that talks to the XWiki REST API.
 * @since 18.4.0RC1
 * @beta
 */
export class DefaultStepManagerApi implements StepManagerApi {
  private sharedStore: TourStore;

  constructor(
    private restClient: GuidedTourRestClient,
    sharedStore: TourStore,
  ) {
    this.sharedStore = sharedStore;
  }

  /**
   * Build the REST URL for steps, optionally targeting a specific step.
   */
  private getStepsUrl(tourId: string, taskId: string, stepId?: number): string {
    let url = `${XWiki.contextPath}/rest/guidedTour/tours/${tourId}/tasks/${taskId}/steps`;

    if (stepId !== undefined) {
      url += `/${stepId}`;
    }

    return url;
  }

  /**
   * Delete a step from a task via the REST API.
   */
  async deleteStep(
    tourId: string,
    taskId: string,
    stepId: number,
  ): Promise<void> {
    const deleteUrl = this.getStepsUrl(tourId, taskId, stepId);
    await this.restClient.request(deleteUrl, "DELETE");
    console.debug("Deleting step", { tourId, taskId, stepId });
  }

  /**
   * Update a step via the REST API. If the order changed, refetch all steps.
   */
  async updateStep(
    tourId: string,
    taskId: string,
    stepId: number,
    stepData: TourStep,
  ): Promise<void> {
    console.debug("Updating step", { tourId, taskId, stepId, stepData });
    const updateUrl = this.getStepsUrl(tourId, taskId, stepId);
    await this.restClient.request(updateUrl, "PUT", stepData);
  }

  /**
   * Create a new step via the REST API and add it to the cache.
   */
  async createStep(
    tourId: string,
    taskId: string,
    stepData: TourStep,
  ): Promise<void> {
    console.debug("Creating step", { tourId, taskId, stepData });
    const createUrl = this.getStepsUrl(tourId, taskId);
    await this.restClient.request(createUrl, "POST", stepData);
  }

  /**
   * Get all steps for a task. Fetches from REST if not cached.
   */
  async getSteps(tourId: string, taskId: string): Promise<TourStep[]> {
    const tourSteps: TourStep[] = await this.fetchSteps(tourId, taskId);
    return Promise.resolve(tourSteps);
  }

  /**
   * Fetch steps from the REST API on cache miss and post-process them
   * (convert empty element selectors to undefined for driver.js compatibility).
   */
  private async fetchSteps(
    tourId: string,
    taskId: string,
  ): Promise<TourStep[]> {
    // FIXME: Reapply this for merge commit.
    //     let parsedCachedSteps;
    // try {
    //   parsedCachedSteps = JSON.parse(
    //     SessionStorageManager.getStorageKey(
    //       SessionStorageManager.getStorageKeyPrefixStr(tourId, taskId),
    //     ) ?? "",
    //   ) as TourStep[];
    //   console.info("Using cached steps:", parsedCachedSteps);
    // } catch (e) {
    //   console.error("Error while parsing cached guidedtour steps:", e);
    //   SessionStorageManager.setStorageKey(
    //     SessionStorageManager.getStorageKeyPrefixStr(tourId, taskId),
    //     undefined,
    //   );
    // }
    // const taskSteps: TourStep[] =
    //   parsedCachedSteps ?? (await this.fetchSteps(tourId, taskId));
    // return Promise.resolve(taskSteps);
    const cache = this.sharedStore.cache;
    const tour = cache.toursMap.get(tourId);
    const task = tour?.tasksList?.find((t) => t.id === taskId);

    const needsFetch = !tour || !task || !task.steps || task.steps.length === 0;

    if (needsFetch) {
      const steps = await this.restClient.request<TourStep[]>(
        this.getStepsUrl(tourId, taskId),
        "GET",
      );
      steps.forEach((step: TourStep) => {
        if (step.element == "") {
          step.element = undefined;
        }
        // if (false == step['backdrop']) {
        //   step['element'] = 'body'; // FIXME: This is NOT FULL PROOF, this should be changed (eg. I want to highlight a random element without a backdrop).
        //   step['popover']['side'] = 'over';
        // } else {
        //   delete step.element;
        // }
      });

      this.sharedStore.updateTaskSteps(tourId, taskId, steps);
      return steps;
    }

    return task.steps ?? [];
  }
}
