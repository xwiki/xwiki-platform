/**
 * See the LICENSE file distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 *
 * This file is part of the Cristal Wiki software prototype
 * @copyright  Copyright (c) 2023 XWiki SAS
 * @license    http://opensource.org/licenses/AGPL-3.0 AGPL-3.0
 *
 **/

import { injectable } from "inversify";
import type { QueueWorker } from "@xwiki/cristal-sharedworker-api";

@injectable()
export default class WorkerQueueWorker implements QueueWorker {
  public constructor() {
    this.initialize();
  }

  public initialize() {}

  public getStatus(): boolean {
    return true;
  }

  public async increment(): Promise<number> {
    return 0;
  }

  public async addToQueue(): Promise<void> {}

  public async getQueueSize(): Promise<number> {
    return 0;
  }
}
