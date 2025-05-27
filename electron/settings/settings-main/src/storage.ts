/*
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

import defaultSettings from "./defaultSettings.json";
import { Configurations } from "@xwiki/cristal-configuration-api";
import Store from "electron-store";

const configurationKey = "configuration";
const settingsKey = "_raw";

const schema = {
  _raw: {
    type: "string",
  },
  configuration: {
    type: "object",
  },
};

const storeInstance: Store = new Store({
  name: "settings",
  // Here, we want to keep in the store the raw string representation of the
  // file, since it should be parsed and serialized by the SettingsManager
  // service. We still include the deserialized object in case we want to check
  // some settings values on Electron's main process.
  serialize: (value: Record<string, unknown>) => value._raw as string,
  deserialize: (value: string) => ({ _raw: value, ...JSON.parse(value) }),
  schema,
});

function set<T>(key: string, value: T) {
  // @ts-expect-error type resolution failing because of electron-store library bug
  storeInstance.set(key, value);
}

function get<T>(key: string): T {
  // @ts-expect-error type resolution failing because of electron-store library bug
  return storeInstance.get(key) as T;
}

function rm(key: string) {
  // @ts-expect-error type resolution failing because of electron-store library bug
  storeInstance.delete(key);
}

function setSettings(value: string): void {
  set(settingsKey, value);
}

function getSettings(): string {
  // @ts-expect-error type resolution failing because of electron-store library bug
  if (storeInstance.size < 2) {
    setSettings(JSON.stringify(defaultSettings, null, 2));
  }
  return get(settingsKey);
}

function deleteSettings(): void {
  rm(settingsKey);
}

function getConfigurations(): Configurations {
  return get(configurationKey);
}

export { deleteSettings, getConfigurations, getSettings, setSettings };
