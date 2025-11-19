/**
 * See the NOTICE file distributed with this work for additional
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

import type { PageData } from "./PageData";
import type { WikiConfig } from "./WikiConfig";
import type { Logger } from "./logger";
import type { LoggerConfig } from "./loggerConfig";
import type { SkinManager } from "./skinManager";
import type { Configurations } from "@manuelleducorg/configuration-api";
import type { Container } from "inversify";
import type { App, Component, Ref } from "vue";
import type { Router } from "vue-router";

/**
 * @since 0.1
 * @beta
 */
export interface CristalApp {
  getApp(): App;

  getRouter(): Router;

  getContainer(): Container;

  setContainer(container: Container): void;

  getWikiConfig(): WikiConfig;

  setWikiConfig(wikiConfig: WikiConfig): void;

  getSkinManager(): SkinManager;

  switchConfig(configName: string): void;

  setAvailableConfigurations(config: Configurations): void;

  getAvailableConfigurations(): Map<string, WikiConfig>;

  /**
   * Delete a configuration from the set of available configurations.
   *
   * @param configName - the name of the configuration to delete
   * @since 0.18
   * @beta
   */
  deleteAvailableConfiguration(configName: string): void;

  run(): Promise<void>;

  /**
   * The method existed before 0.15, but wasn't allowing for components to be
   * resolved asynchronously.
   * This is useful to avoid loading all the components of a UIX during the first page load, and instead wait for the
   * extension point to be actually required first.
   *
   * @param extensionPoint - id of the extension point to resolve
   * @since 0.15
   * @beta
   */
  getUIXTemplates(extensionPoint: string): Promise<Array<Component>>;

  getMenuEntries(): Array<string>;

  getCurrentPage(): string;

  getCurrentContent(): string;

  getCurrentSource(): string;

  /**
   * Return the syntax of the current page.
   * @since 0.7
   * @beta
   */
  getCurrentSyntax(): string;

  /** @since 0.18
   * @beta
   */
  setCurrentPage(page: string, mode?: string): Promise<void>;

  /**
   * @deprecated use the document-api instead
   * @param ref - the reactive reference holding the reference to a page data.
   */
  setContentRef(ref: Ref): void;

  /**
   * @deprecated since 0.12, use ClickListener instead
   */
  loadPageFromURL(url: string): Promise<void>;

  loadPage(action?: string, options?: { requeue: boolean }): Promise<void>;

  /**
   * Return the requested page
   * @param page - a page identifier (e.g., a document reference for the XWiki
   *  backend, or a filename for the filesystem backend)
   * @param revision - the revision requested, undefined will default to latest
   * @returns the page data, or undefined if the page is not found
   *
   * @since 0.7
   * @beta
   */
  getPage(
    page: string,
    options?: { requeue?: boolean; revision?: string },
  ): Promise<PageData | undefined>;

  getLogger(module: string): Logger;

  getLoggerConfig(): LoggerConfig;
}
