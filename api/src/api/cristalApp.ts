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

import type { Container } from "inversify";
import type { App, Component, Ref } from "vue";
import type { WikiConfig } from "./WikiConfig";
import type { Router } from "vue-router";
import type { Logger } from "./logger";
import type { LoggerConfig } from "./loggerConfig";
import type { SkinManager } from "./skinManager";
import { PageData } from "./PageData";

export interface CristalApp {
  getApp(): App;

  getRouter(): Router;

  getContainer(): Container;

  setContainer(container: Container): void;

  getWikiConfig(): WikiConfig;

  setWikiConfig(wikiConfig: WikiConfig): void;

  getSkinManager(): SkinManager;

  switchConfig(configName: string): void;

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  setAvailableConfigurations(config: Map<string, any>): void;

  getAvailableConfigurations(): Map<string, WikiConfig>;

  run(): Promise<void>;

  getUIXTemplates(extensionPoint: string): Array<Component>;

  getMenuEntries(): Array<string>;

  getCurrentPage(): string;

  getCurrentContent(): string;

  getCurrentSource(): string;

  /**
   * Return the syntax of the current page.
   * @since 0.7
   */
  getCurrentSyntax(): string;

  setCurrentPage(page: string, mode: string): void;

  setContentRef(ref: Ref): void;

  loadPageFromURL(url: string): Promise<void>;

  loadPage(): Promise<void>;

  /**
   * Return the requested page
   * @param page a page identifier (e.g., a document reference for the XWiki
   * backend, or a filename for the filesystem backend)
   *
   * @since 0.7
   */
  getPage(page: string): Promise<PageData>;

  getLogger(module: string): Logger;

  getLoggerConfig(): LoggerConfig;

  renderContent(
    source: string,
    sourceSyntax: string,
    targetSyntax: string,
    wikiConfig: WikiConfig,
  ): Promise<string>;
}
