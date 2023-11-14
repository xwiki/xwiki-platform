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

import { Logger, LoggerConfig, CristalApp as CristalApp, WikiConfig, SkinManager } from "@cristal/api";
import { Container, injectable } from "inversify";
import { App, Component, Ref } from "vue";
import { Router } from "vue-router";

@injectable()
export class WorkerCristalApp implements CristalApp {

    private wikiConfig : WikiConfig;
    private container : Container;
    private availableConfigurations : Map<string, WikiConfig>;

    public constructor() {
        this.availableConfigurations = new Map<string, WikiConfig>();
    }

    getApp(): App<any> {
        throw new Error("Method not implemented.");
    }
    getRouter(): Router {
        throw new Error("Method not implemented.");
    }
    getContainer(): Container {
        return this.container;
    }

    setContainer(container : Container) : void {
        this.container = container;
    }

    getWikiConfig(): WikiConfig {
        return this.wikiConfig;
    }
    setWikiConfig(wikiConfig: WikiConfig): void {
        this.wikiConfig = wikiConfig;
    }

    getSkinManager() : SkinManager {
        throw new Error("Method not implemented.");
    }

    switchConfig(configName : string) : void {
    }

    setAvailableConfigurations(config : Map<string, any>) {
        console.log(config);
        config.forEach((wikiConfigObject: any, key: string) => {
            let configType = wikiConfigObject?.configType;

            if (wikiConfigObject) {
                const wikiConfig = this.container.getNamed<WikiConfig>("WikiConfig", configType);
                wikiConfig.setConfigFromObject(wikiConfigObject);
                this.availableConfigurations.set(key, wikiConfig);
            }
        });
    }

    getAvailableConfigurations() : Map<string, WikiConfig> {
        return this.availableConfigurations;
    }

    run(): Promise<void> {
        throw new Error("Method not implemented.");
    }
    getUIXTemplates(extensionPoint: string): Component[] {
        throw new Error("Method not implemented.");
    }
    getMenuEntries(): string[] {
        throw new Error("Method not implemented.");
    }
    getCurrentPage(): string {
        throw new Error("Method not implemented.");
    }
    setCurrentPage(page: string): void {
        throw new Error("Method not implemented.");
    }
    setContentRef(ref: Ref<any>): void {
        throw new Error("Method not implemented.");
    }
    loadPageFromURL(url: string): Promise<void> {
        throw new Error("Method not implemented.");
    }
    loadPage(): Promise<void> {
        throw new Error("Method not implemented.");
    }
    getLogger(module: string): Logger {
        throw new Error("Method not implemented.");
    }
    getLoggerConfig(): LoggerConfig {
        throw new Error("Method not implemented.");
    }

    renderContent(source : string, sourceSyntax : string, targetSyntax : string, wikiConfig : WikiConfig) : string {
        throw new Error("Method not implemented.");
    }
}
