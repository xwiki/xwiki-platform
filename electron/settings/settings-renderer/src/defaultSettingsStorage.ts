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

import { injectable } from "inversify";
import type {
  SettingsManager,
  SettingsStorage,
} from "@xwiki/cristal-settings-api";

interface SettingsWindow extends Window {
  settings: {
    save(settings: string): Promise<void>;

    load(): Promise<string>;
  };
}
declare const window: SettingsWindow;

/**
 * Default implementation for {@link SettingsStorage} on Electron.
 * It stores and retrieves settings as JSON from Electron storage.
 * @since 0.18
 * @beta
 */
@injectable()
export class DefaultSettingsStorage implements SettingsStorage {
  async save(settingsManager: SettingsManager): Promise<void> {
    await window.settings.save(settingsManager.toJSON());
  }

  async load(settingsManager: SettingsManager): Promise<void> {
    settingsManager.fromJSON((await window.settings.load()) ?? "[]");
  }
}
