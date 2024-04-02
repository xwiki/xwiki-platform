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
import { Logger, PageData, Storage, WikiConfig } from "@cristal/api";
import { injectable, unmanaged } from "inversify";

@injectable()
export abstract class AbstractStorage implements Storage {
  protected logger: Logger;
  protected wikiConfig: WikiConfig;

  constructor(@unmanaged() logger: Logger, module: string) {
    this.logger = logger;
    this.logger.setModule(module);
  }

  setWikiConfig(wikiConfig: WikiConfig) {
    this.logger.debug("Setting wiki Config: ", wikiConfig);
    this.wikiConfig = wikiConfig;
  }

  getWikiConfig() {
    return this.wikiConfig;
  }

  abstract getEditField(
    jsonArticle: object,
    fieldName: string,
  ): Promise<string>;

  abstract getImageURL(page: string, image: string): string;

  abstract getPageContent(page: string, syntax: string): Promise<PageData>;

  abstract getPageFromViewURL(url: string): string | null;

  abstract getPageRestURL(page: string, syntax: string): string;

  abstract getPanelContent(
    panel: string,
    contextPage: string,
    syntax: string,
  ): Promise<PageData>;

  abstract isStorageReady(): Promise<boolean>;

  /**
   * Save a page and its content to the give syntax.
   *
   * @param page the page to save
   * @param content the content of the page
   * @param syntax the syntax of the content
   *
   * @since 0.7
   */
  abstract save(
    page: string,
    content: string,
    syntax: string,
  ): Promise<unknown>;
}
