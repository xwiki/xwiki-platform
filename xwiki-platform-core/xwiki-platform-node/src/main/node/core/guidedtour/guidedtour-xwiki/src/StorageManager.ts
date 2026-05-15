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
import type { TourTask } from "@xwiki/platform-guidedtour-api";

export class StorageManager {
  static parseStorageKeyPrefix(
    key: string,
  ): { taskId: string; tourId: string } | undefined {
    const regexp = RegExp(/^guidedtour_(.*)__(.*)$/, "g");
    const matches = key.matchAll(regexp).next();
    const len = 0;
    if (matches.value === undefined || matches.value?.length != 3) {
      console.error(
        `Error parsing "${key}": Found ${len} matches, but I wanted exactly 2.`,
      );
      return undefined;
    }
    // Also unescape the ids
    const result = {
      tourId: matches.value[1].replaceAll("_|_", "__"),
      taskId: matches.value[2].replaceAll("_|_", "__"),
    };
    return result;
  }

  static getStorageKeyPrefix(task: TourTask): string {
    return StorageManager.getStorageKeyPrefixStr(task.tourId!, task.id!);
  }

  static getStorageKeyPrefixStr(tourId: string, taskId: string): string {
    return `guidedtour_${tourId.replaceAll("__", "_|_")}__${taskId.replaceAll("__", "_|_")}`;
  }

  static getStorageKey(key: string) {
    return window.sessionStorage.getItem(key);
  }

  static getActiveTaskStorageKey(): string {
    return "guidedtour_activeTask";
  }

  static getTaskCurrentStepStorageKey(task: TourTask): string {
    return this.getStorageKeyPrefix(task) + "_currentStep";
  }

  static getTaskStepStorageStorageKey(task: TourTask): string {
    return this.getStorageKeyPrefix(task) + "_steps";
  }

  static setStorageKey(key: string, stepIndex?: string) {
    if (stepIndex === undefined) {
      window.sessionStorage.removeItem(key);
    } else {
      window.sessionStorage.setItem(key, stepIndex);
    }
  }
}
