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

import * as Comlink from "comlink";
import type { MyWorker, QueueWorker } from "@xwiki/cristal-sharedworker-api";
import type { CristalApp, WrappingStorage } from "@xwiki/cristal-api";
import { type WikiConfig } from "@xwiki/cristal-api";
import { CristalLoader } from "@xwiki/cristal-extension-manager";
import { ComponentInit as DexieBackendComponentInit } from "@xwiki/cristal-backend-dexie";
import { ComponentInit as GithubBackendComponentInit } from "@xwiki/cristal-backend-github";
import { ComponentInit as NextcloudBackendComponentInit } from "@xwiki/cristal-backend-nextcloud";
import { ComponentInit as XWikiBackendComponentInit } from "@xwiki/cristal-backend-xwiki";
import type { Container } from "inversify";
import { WorkerCristalApp } from "./workerCristalApp";
import WorkerQueueWorker from "./workerQueueWorker";

export class Worker implements MyWorker {
  private currentNumber: number = 0;
  private queue: Array<string> = [];
  private container: Container;
  private cristal: CristalApp;
  private configMap: Map<string, object>;
  private initialized: boolean = false;
  private fct: (a: string) => void;

  /*
     Start worker thread
    */
  public async start(): Promise<void> {
    console.log("Starting worker thread");
    this.initialize();
    // eslint-disable-next-line no-constant-condition
    while (true) {
      await this.sleep(1000);
      this.checkQueue();
    }
  }

  public setPageLoadedCallback(fct: (a: string) => void): void {
    this.fct = fct;
  }

  public sleep(ms: number): Promise<unknown> {
    return new Promise((resolve) => setTimeout(resolve, ms));
  }

  public async checkQueue(): Promise<void> {
    // console.log("Checking queue");
    const size = this.getQueueSize();
    // console.log("Current queue size is ", size, this.queue);
    if (size > 0) {
      if (this.initialized) {
        const page = this.queue.pop();
        if (page) {
          await this.handleQueueItem(page);
        }
        await this.checkQueue();
      } else {
        console.log(
          "Worker cristal code not initialized yet. Cannot process queue items.",
        );
      }
    }
  }

  public async handleQueueItem(page: string): Promise<void> {
    console.log("Handling queue item code ready to process", page);

    try {
      let configName = null;
      let pageName = null;
      let type = null;
      const index1 = page.indexOf(":");
      const index2 = page.lastIndexOf("_");
      if (index1 != -1 && index2 != -1) {
        configName = page.substring(0, index1);
        pageName = page.substring(index1 + 1, index2);
        type = page.substring(index2 + 1);

        const wikiConfig = this.getWikiConfig(configName);
        if (wikiConfig == undefined) {
          console.log("Could not find config for page", page);
        } else {
          console.log("Updating page ", pageName, type);
          const storage = wikiConfig.storage as WrappingStorage;
          const result = await storage.updatePageContent(pageName, type);
          if (result) {
            console.log("Page updated. Calling back to main thread");
            this.fct(page);
          }
        }
      } else {
        console.log("Could not process page", page);
      }
    } catch (e) {
      console.log("Exception while trying to load", page, e);
    }
  }

  public add(a: number): number {
    console.log("Worker in add");
    this.currentNumber += a;
    return this.currentNumber;
  }

  public addToQueue(page: string): void {
    console.log("Worker in addToQueue", page);
    if (!this.queue.includes(page)) {
      this.queue.push(page);
    }
    return;
  }

  public getQueueSize(): number {
    console.log("Worker in getQueueSize");
    return this.queue.length;
  }

  public getWikiConfig(configName: string): WikiConfig | undefined {
    const wikiConfigObject = this.configMap.get(configName);
    if (wikiConfigObject == null) {
      console.error("Failed to find wikiConfig for configName", configName);
      return undefined;
    }
    const wikiConfigMap = new Map(Object.entries(wikiConfigObject));
    const configType = wikiConfigMap.get("configType");
    console.log(
      "Looking for wikiConfig for name",
      configName,
      "and type",
      configType,
    );

    let wikiConfig;
    try {
      wikiConfig = this.container.getNamed<WikiConfig>(
        "WikiConfig",
        configType,
      );
      this.cristal.setWikiConfig(wikiConfig);
      wikiConfig.setConfigFromObject(wikiConfigObject);

      // Make sure we have initialized this config
      // This is necessary for offline mode
      wikiConfig.initialize();

      console.debug(
        "Found wikiConfig for",
        configName,
        "and type",
        configType,
        ":",
        wikiConfig,
      );
      return wikiConfig;
    } catch (e) {
      console.error(
        "Failed to find wikiConfig for name",
        configName,
        "and type",
        configType,
        e,
      );
      return undefined;
    }
  }

  public async initialize(): Promise<void> {
    console.log("Starting initialize");
    const extensionList: Array<string> = ["storage"];
    const response = await fetch("/config.json");
    const config = await response.json();
    this.configMap = new Map(Object.entries(config));
    console.log("Loaded json config", this.configMap);
    const cristalLoader = new CristalLoader(extensionList);
    cristalLoader.initializeContainer();
    this.container = cristalLoader.container;
    console.log("Container status", this.container);
    this.container
      .bind<CristalApp>("CristalApp")
      .to(WorkerCristalApp)
      .inSingletonScope();
    this.container
      .bind<QueueWorker>("QueueWorker")
      .to(WorkerQueueWorker)
      .inSingletonScope();
    this.cristal = this.container.get<CristalApp>("CristalApp");
    this.cristal.setContainer(this.container);
    console.log("Container status", this.container);
    // TODO: find a way to do this loading differently. Here we need to
    //  explicitly depend on all required storage and this is not good.
    new DexieBackendComponentInit(cristalLoader.container);
    new GithubBackendComponentInit(cristalLoader.container);
    new NextcloudBackendComponentInit(cristalLoader.container);
    new XWikiBackendComponentInit(cristalLoader.container);
    console.log("Loading storage components");
    this.initialized = true;
    console.log("Finished initialize");
  }
}

const worker = new Worker();
worker.start();

// @ts-expect-error ignore
// TODO remove use of any
// eslint-disable-next-line @typescript-eslint/no-explicit-any
onconnect = (e: any) => {
  Comlink.expose(worker, e.ports[0]);
};
console.log("Worker code loaded");
