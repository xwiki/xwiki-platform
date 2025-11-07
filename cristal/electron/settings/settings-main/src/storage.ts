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

import defaultSettings from "./defaultSettings.json";
import Store from "electron-store";
import type { Configurations } from "@xwiki/cristal-configuration-api";

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

type StoreType = {
  _raw: string;
  configuration: object;
};

const storeInstance: Store<StoreType> = new Store<StoreType>({
  name: "settings",
  // Here, we want to keep in the store the raw string representation of the
  // file, since it should be parsed and serialized by the SettingsManager
  // service. We still include the deserialized object in case we want to check
  // some settings values on Electron's main process.
  serialize: (value: Record<string, unknown>) => value._raw as string,
  deserialize: (value: string) => ({ _raw: value, ...JSON.parse(value) }),
  schema,
});

function setSettings(value: string): void {
  storeInstance.set(settingsKey, value);
}

function getSettings(): string {
  if (storeInstance.size < 2) {
    setSettings(JSON.stringify(defaultSettings, null, 2));
  }
  return storeInstance.get(settingsKey);
}

function deleteSettings(): void {
  storeInstance.delete(settingsKey);
}

/**
 * @since 0.18
 * @beta
 */
function getConfigurations(): Configurations {
  return storeInstance.get(configurationKey) as Configurations;
}

export { deleteSettings, getConfigurations, getSettings, setSettings };
