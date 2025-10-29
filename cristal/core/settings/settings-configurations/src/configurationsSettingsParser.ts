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

import { ConfigurationsSettings } from "./configurations";
import { injectable } from "inversify";
import type { Configuration } from "@xwiki/cristal-configuration-api";
import type { SettingsParser } from "@xwiki/cristal-settings-api";

/**
 * Implementation of {@link SettingsParser} for {@link ConfigurationSettings}.
 * In particular, it will handle parsing Map instances used as content.
 * @since 0.18
 * @beta
 */
@injectable()
export class ConfigurationsSettingsParser implements SettingsParser {
  parse(serializedSettings: string): ConfigurationsSettings {
    const parsed = JSON.parse(serializedSettings) as {
      [configurationName: string]: Configuration;
    };
    return new ConfigurationsSettings(new Map(Object.entries(parsed)));
  }
}
