/**
 * See the LICENSE file distributed with this work for additional
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

import type { Settings } from "./settings";

/**
 * A SettingsManager handles operations on Cristal's settings.
 * @since 0.18
 * @beta
 */
export interface SettingsManager {
  /**
   * Updates the stored settings for the correct type of settings.
   * The type should be deduced from the "key" attribute of the instance.
   * @param settings - the settings instance to store.
   */
  set(settings: Settings): void;

  /**
   * Retrieves the settings associated to a given type.
   * @param type - the type of settings to retrieve.
   * @returns the stored instance, or undefined if not found.
   */
  get<T extends Settings>(type: new () => T): T | undefined;

  /**
   * Converts all the settings stored into a JSON representation.
   * @returns a JSON representation.
   */
  toJSON(): string;

  /**
   * Populates settings from a JSON representation.
   * @param json - the JSON representation.
   */
  fromJSON(json: string): void;
}
