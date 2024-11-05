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

import Worker from "./worker?sharedworker";
import * as Comlink from "comlink";
import { inject, injectable } from "inversify";
import { type CristalApp, type Logger } from "@xwiki/cristal-api";
import {
  type DocumentService,
  name as documentServiceName,
} from "@xwiki/cristal-document-api";
import type { MyWorker, QueueWorker } from "@xwiki/cristal-sharedworker-api";

@injectable()
export default class DefaultQueueWorker implements QueueWorker {
  // TODO remove use of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  private workerInstance: any;
  private cristalApp: CristalApp;
  private logger: Logger;

  public constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<CristalApp>("CristalApp") cristalApp: CristalApp,
  ) {
    this.initialize();
    this.logger = logger;
    this.cristalApp = cristalApp;
  }

  public pageLoaded(page: string): void {
    try {
      this.logger.debug(
        "Received callback that new document has been loaded",
        page,
      );
      // We need to access the DocumentService lasily as otherwise the
      // internal pinia instance is initialized too early.
      const documentService = this.cristalApp
        .getContainer()
        .get<DocumentService>(documentServiceName);
      documentService.refreshCurrentDocument();

      this.logger.debug(
        "Done callback that new document has been loaded",
        page,
      );
    } catch (e) {
      this.logger.error("Error calling loadPage", e);
    }
  }

  public initialize(): void {
    try {
      if (this.workerInstance == null) {
        // this.workerInstance = new ComlinkWorker<typeof import('./worker')>(new URL('./worker',
        // import.meta.url), {/* normal Worker options*/})
        const sworker = new Worker({
          name: "cristal-sharedworker",
        });
        this.workerInstance = Comlink.wrap<MyWorker>(sworker.port);
        // TODO get rid of aliasing
        // eslint-disable-next-line @typescript-eslint/no-this-alias
        const that = this;
        this.workerInstance.setPageLoadedCallback(
          Comlink.proxy(function (page: string) {
            that.pageLoaded(page);
          }),
        );

        console.debug("Worker initialized", this.workerInstance);
      } else {
        console.debug("Already initialized");
      }
    } catch (e) {
      console.error("Failed to initialize shared worker", e);
    }
  }

  public getStatus(): boolean {
    if (this.workerInstance != null) {
      console.log("workerInstance is ready");
      return true;
    } else {
      console.log("workerInstance is null. Cannot call");
      return false;
    }
  }

  public async increment(): Promise<number> {
    if (this.workerInstance == null) {
      console.log("workerInstance is null. Cannot call");
      return -1;
    } else {
      const result = await this.workerInstance.add(1);
      console.log("Results from shared worker: ", result);
      return result;
    }
  }

  public async addToQueue(page: string): Promise<void> {
    console.log("worker Calling addToQueue", page);
    if (this.workerInstance != null) {
      await this.workerInstance.addToQueue(page);
    } else {
      console.log("workerInstance is null. Cannot call");
    }
  }

  public async getQueueSize(): Promise<number> {
    console.log("worker Calling getQueueSize");
    if (this.workerInstance == null) return -1;
    else return await this.workerInstance.getQueueSize();
  }
}
