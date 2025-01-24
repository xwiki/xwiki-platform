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
import config from "./defaultConfig.json";
import { Configurations } from "@xwiki/cristal-configuration-api";
import { ipcMain } from "electron";
import Store from "electron-store";

const schema = {
  configuration: {
    type: "object",
  },
};

const storeInstance: Store = new Store({
  name: "config",
  schema,
});

/**
 * Get access to the configuration from the store instance.
 *
 * @since 0.14
 */
function readConfiguration(): Configurations {
  // Create the configuration with default values the first time Cristal is loaded.
  // @ts-expect-error type resolution failing because of electron-store library bug
  if (!storeInstance.has("configuration")) {
    // @ts-expect-error type resolution failing because of electron-store library bug
    storeInstance.set("configuration", config);
  }
  // @ts-expect-error type resolution failing because of electron-store library bug
  return storeInstance.get("configuration");
}

function load(): void {
  ipcMain.handle("configuration:load", () => {
    return readConfiguration();
  });
}

export { load, readConfiguration };
