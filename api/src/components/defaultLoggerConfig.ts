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

import { injectable } from "inversify";
import type { LoggerConfig } from "../api/loggerConfig";

@injectable()
export class DefaultLoggerConfig implements LoggerConfig {
  protected config: Map<string, string>;
  protected computedConfig: Map<string, number>;
  protected defaultLevel: string;
  protected defaultLevelId: number;
  protected levels: Map<string, number> = new Map<string, number>();

  constructor(defaultLevel = "error") {
    this.config = new Map<string, string>();
    this.defaultLevel = defaultLevel;
    this.computedConfig = new Map<string, number>();
    this.levels.set("error", 1);
    this.levels.set("warn", 2);
    this.levels.set("info", 3);
    this.levels.set("debug", 4);
  }

  addLevel(module: string, level: string): void {
    const nbLevel = this.levels.get(level);
    if (nbLevel != undefined) {
      this.config.forEach((key) => {
        if (key.startsWith(module)) {
          const currentLevel = this.computedConfig.get(key);
          if (currentLevel == null || (nbLevel && nbLevel > currentLevel)) {
            if (nbLevel) this.computedConfig.set(key, nbLevel);
          }
        }
      });
    }
  }
  getLevels(): Map<string, string> {
    return this.config;
  }

  getLevel(module: string): string {
    let level = this.config.get(module);
    if (!level) level = this.defaultLevel;
    return level;
  }

  getLevelId(level: string): number {
    const nbLevel = this.levels.get(level);
    if (!nbLevel) return 10;
    else return nbLevel;
  }

  setDefaultLevel(level: string): void {
    this.defaultLevel = level;
    this.defaultLevelId = this.getLevelId(this.defaultLevel);
  }

  getDefaultLevel(): string {
    return this.defaultLevel;
  }

  getDefaultLevelId(): number {
    if (this.defaultLevelId) return this.defaultLevelId;
    else {
      this.defaultLevelId = this.getLevelId(this.defaultLevel);
      return this.defaultLevelId;
    }
  }

  hasLevel(module: string, level: string): boolean {
    let nbLevel = this.computedConfig.get(module);
    if (!nbLevel) nbLevel = this.getDefaultLevelId();
    const demandedLevel = this.getLevelId(level);

    if (demandedLevel) {
      if (nbLevel >= demandedLevel) return true;
    }
    return false;
  }

  hasLevelId(module: string, levelId: number): boolean {
    let nbLevel = this.computedConfig.get(module);
    if (!nbLevel) nbLevel = this.getDefaultLevelId();
    const demandedLevel = levelId;

    if (demandedLevel) {
      if (nbLevel >= demandedLevel) return true;
    }
    return false;
  }
}
