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

import {
  AttachmentsData,
  PageAttachment,
  PageData,
  Storage,
  WikiConfig,
} from "@xwiki/cristal-api";
import { injectable, unmanaged } from "inversify";
import type { Logger } from "@xwiki/cristal-api";

@injectable()
export abstract class AbstractStorage implements Storage {
  protected logger: Logger;
  // @ts-expect-error wikiConfig is temporarily undefined during class
  // initialization
  protected wikiConfig: WikiConfig;

  constructor(@unmanaged() logger: Logger, module: string) {
    this.logger = logger;
    this.logger.setModule(module);
  }

  setWikiConfig(wikiConfig: WikiConfig): void {
    this.logger.debug("Setting wiki Config: ", wikiConfig);
    this.wikiConfig = wikiConfig;
  }

  getWikiConfig(): WikiConfig {
    return this.wikiConfig;
  }

  abstract getEditField(
    jsonArticle: object,
    fieldName: string,
  ): Promise<string>;

  abstract getImageURL(page: string, image: string): string;

  abstract getPageContent(
    page: string,
    syntax: string,
    revision?: string,
  ): Promise<PageData | undefined>;

  /**
   * Returns the list of attachments of a given page.
   * TODO: this API is missing pagination.
   * @since 0.9
   */
  abstract getAttachments(page: string): Promise<AttachmentsData | undefined>;

  /**
   * @since 0.12
   */
  abstract getAttachment(
    page: string,
    name: string,
  ): Promise<PageAttachment | undefined>;

  abstract getPageFromViewURL(url: string): string | null;

  abstract getPageRestURL(
    page: string,
    syntax: string,
    revision?: string,
  ): string;

  abstract getPanelContent(
    panel: string,
    contextPage: string,
    syntax: string,
  ): Promise<PageData>;

  abstract isStorageReady(): Promise<boolean>;

  /**
   * Save a page and its content to the give syntax.
   *
   * @param page - the page to save
   * @param title - the raw page title
   * @param content - the content of the page
   * @param syntax - the syntax of the content
   *
   * @since 0.8
   */
  abstract save(
    page: string,
    title: string,
    content: string,
    syntax: string,
  ): Promise<unknown>;

  /**
   * @since 0.9
   */
  abstract saveAttachments(page: string, files: File[]): Promise<unknown>;

  /**
   * Delete a page.
   *
   * @param page - the page to delete
   * @returns true if the delete was successful, false with the reason otherwise
   *
   * @since 0.11
   */
  abstract delete(page: string): Promise<{ success: boolean; error?: string }>;
}
