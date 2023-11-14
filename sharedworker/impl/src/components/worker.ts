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

import * as Comlink from 'comlink';
import { MyWorker } from '@cristal/sharedworker-api';
import { QueueWorker } from '@cristal/sharedworker-api';
import { CristalApp, WrappingStorage, type WikiConfig } from '@cristal/api';
import { CristalLoader } from "@cristal/extension-manager";
import { ComponentInit as StorageComponentInit } from "@cristal/storage";
import { Container } from 'inversify';
import { WorkerCristalApp } from './workerCristalApp';
import WorkerQueueWorker from './workerQueueWorker';

export class Worker implements MyWorker { 
    private currentNumber : number = 0;
    private queue : Array<string> = new Array();
    private container : Container;
    private cristal : CristalApp;
    private configMap : Map<string, Object>;
    private initialized : boolean = false;
    private fct : Function;
    /*
     Start worker thread 
    */
    public async start() {
        console.log("Starting worker thread");
        this.initialize();
        while (true) {
            await this.sleep(1000);
            this.checkQueue();
        }
    }

    public setPageLoadedCallback(fct: Function) : void {
        this.fct = fct;
    }

    public sleep(ms : number) {
        return new Promise(resolve => setTimeout(resolve, ms));
    }

    public async checkQueue() : Promise<void> {
        // console.log("Checking queue");
        let size = this.getQueueSize();
        // console.log("Current queue size is ", size, this.queue);
        if (size>0) {
            if (this.initialized) {
                let page = this.queue.pop();
                if (page)
                    await this.handleQueueItem(page);
                await this.checkQueue();
            } else {
              console.log("Worker cristal code not initialized yet. Cannot process queue items.")
            }
        }   
    }

    public async handleQueueItem(page : string) : Promise<void> {
        console.log("Handling queue item code ready to process", page);
        
        try {
        let configName = null;
        let pageName = null;
        let type = null;
        let index1 = page.indexOf(":");
        let index2 = page.lastIndexOf("_");
        if (index1!=-1 && index2!=-1) {
            configName = page.substring(0, index1);
            pageName = page.substring(index1+1, index2);
            type = page.substring(index2 + 1);

            let wikiConfig = this.getWikiConfig(configName);
            if (wikiConfig==undefined) {
                console.log("Could not find config for page", page);
            } else {
                console.log("Updating page ", pageName, type);
                let storage = wikiConfig.storage as WrappingStorage;
                let result = await storage.updatePageContent(pageName, type);
                if (result==true) {
                    console.log("Page updated. Calling back to main thread");
                    this.fct(page);
                }
            }

        } else {
            console.log("Could not process page", page)
        }
        } catch (e) {
            console.log("Exception while trying to load", page, e);
        }
    }

    public add(a: number) : number { 
        console.log("Worker in add");
        this.currentNumber += a; 
        return this.currentNumber 
    };

    public addToQueue(page : string) : void { 
        console.log("Worker in addToQueue", page);
        if (!this.queue.includes(page))
            this.queue.push(page); 
        return; 
    };

    public getQueueSize() : number { 
        console.log("Worker in getQueueSize");
        return this.queue.length 
    };

    public getWikiConfig(configName : string) : WikiConfig | undefined {
        let wikiConfigObject = this.configMap.get(configName);
        if (wikiConfigObject==null) {
            console.error("Failed to find wikiConfig for configName", configName);
            return undefined;
        }
        let wikiConfigMap = new Map(Object.entries(wikiConfigObject))
        let configType = wikiConfigMap.get("configType");
        console.log("Looking for wikiConfig for name", configName, "and type", configType);

        let wikiConfig;
        try {
            wikiConfig = this.container.getNamed<WikiConfig>("WikiConfig", configType);
            this.cristal.setWikiConfig(wikiConfig);
            wikiConfig.setConfigFromObject(wikiConfigObject);
 
            // Make sure we have initialized this config
            // This is necessary for offline mode
            wikiConfig.initialize();

            console.debug("Found wikiConfig for", configName, "and type", configType, ":", wikiConfig);
            return wikiConfig;
        } catch (e) {
            console.error("Failed to find wikiConfig for name", configName, "and type", configType, e);
            return undefined;
        }
}

    public async initialize() {
        console.log("Starting initialize");
        let extensionList : Array<string> = [ "storage" ];
        let response = await fetch("/config.json");
        let config = await response.json();
        this.configMap = new Map(Object.entries(config));
        console.log("Loaded json config", this.configMap);
        let cristalLoader = new CristalLoader(extensionList);
        cristalLoader.initializeContainer();
        this.container = cristalLoader.container;
        console.log("Container status", this.container);
        this.container.bind<CristalApp>("CristalApp").to(WorkerCristalApp).inSingletonScope();
        this.container.bind<QueueWorker>("QueueWorker").to(WorkerQueueWorker).inSingletonScope();
        this.cristal = this.container.get<CristalApp>("CristalApp");
        this.cristal.setContainer(this.container);
        console.log("Container status", this.container);
        new StorageComponentInit(cristalLoader.container);
        console.log("Loading storage components");
        this.initialized = true;
        console.log("Finished initialize");
    }

}

let worker = new Worker();
worker.start();

// @ts-ignore
onconnect = (e : any) => {
     Comlink.expose(worker, e.ports[0]);
}
console.log("Worker code loaded")
