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

import { inject, injectable, optional } from "inversify";
import type { Logger } from "../api/logger";
import type { LoggerConfig } from "../api/loggerConfig";

@injectable()
export class DefaultLogger implements Logger {
  // @ts-expect-error module is temporarily undefined during class
  // initialization
  module: string;

  constructor(
    @inject("LoggerConfig")
    @optional()
    readonly loggerConfig?: LoggerConfig | undefined,
  ) {}

  setModule(module: string): void {
    this.module = module;
  }

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  debug(...data: any[]): void {
    if (!this.loggerConfig || this.loggerConfig.hasLevelId(this.module, 4)) {
      data.unshift(this.module + ":");
    }
    console.debug.apply(null, data);
  }

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  info(...data: any[]): void {
    data.unshift(this.module + ":");
    console.info.apply(null, data);
  }

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  warn(...data: any[]): void {
    data.unshift(this.module + ":");
    console.warn.apply(null, data);
  }

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  error(...data: any[]): void {
    data.unshift(this.module + ":");
    console.error.apply(null, data);
  }
}
