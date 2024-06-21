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

import type {
  CristalApp as CristalApp,
  Logger,
  LoggerConfig,
  PageData,
  SkinManager,
  WikiConfig,
} from "@xwiki/cristal-api";
import type { Container } from "inversify";
import { injectable } from "inversify";
import type { App, Component } from "vue";
import type { Router } from "vue-router";

@injectable()
export class WorkerCristalApp implements CristalApp {
  private wikiConfig: WikiConfig;
  private container: Container;
  private availableConfigurations: Map<string, WikiConfig>;

  public constructor() {
    this.availableConfigurations = new Map<string, WikiConfig>();
  }

  getApp(): App {
    throw new Error("Method not implemented.");
  }
  getRouter(): Router {
    throw new Error("Method not implemented.");
  }
  getContainer(): Container {
    return this.container;
  }

  setContainer(container: Container): void {
    this.container = container;
  }

  getWikiConfig(): WikiConfig {
    return this.wikiConfig;
  }
  setWikiConfig(wikiConfig: WikiConfig): void {
    this.wikiConfig = wikiConfig;
  }

  getSkinManager(): SkinManager {
    throw new Error("Method not implemented.");
  }

  switchConfig(): void {}

  // TODO remove use of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  setAvailableConfigurations(config: Map<string, any>): void {
    console.log(config);
    // TODO remove use of any
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    config.forEach((wikiConfigObject: any, key: string) => {
      const configType = wikiConfigObject?.configType;

      if (wikiConfigObject) {
        const wikiConfig = this.container.getNamed<WikiConfig>(
          "WikiConfig",
          configType,
        );
        wikiConfig.setConfigFromObject(wikiConfigObject);
        this.availableConfigurations.set(key, wikiConfig);
      }
    });
  }

  getAvailableConfigurations(): Map<string, WikiConfig> {
    return this.availableConfigurations;
  }

  run(): Promise<void> {
    throw new Error("Method not implemented.");
  }
  getUIXTemplates(): Component[] {
    throw new Error("Method not implemented.");
  }
  getMenuEntries(): string[] {
    throw new Error("Method not implemented.");
  }
  getCurrentPage(): string {
    throw new Error("Method not implemented.");
  }
  getCurrentContent(): string {
    throw new Error("Method not implemented.");
  }
  getCurrentSource(): string {
    throw new Error("Method not implemented.");
  }
  getCurrentSyntax(): string {
    throw new Error("Method not implemented.");
  }
  setCurrentPage(): void {
    throw new Error("Method not implemented.");
  }
  setContentRef(): void {
    throw new Error("Method not implemented.");
  }
  loadPageFromURL(): Promise<void> {
    throw new Error("Method not implemented.");
  }
  loadPage(): Promise<void> {
    throw new Error("Method not implemented.");
  }
  getPage(): Promise<PageData> {
    throw new Error("Method not implemented.");
  }
  getLogger(): Logger {
    throw new Error("Method not implemented.");
  }
  getLoggerConfig(): LoggerConfig {
    throw new Error("Method not implemented.");
  }

  renderContent(): Promise<string> {
    throw new Error("Method not implemented.");
  }
}
