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

import type { PageData } from "./PageData";
import type { WikiConfig } from "./WikiConfig";
import { PageAttachment } from "./pageAttachment";

export interface Storage {
  setWikiConfig(config: WikiConfig): void;

  getWikiConfig(): WikiConfig;

  getPageRestURL(page: string, syntax: string): string;

  getPageFromViewURL(url: string): string | null;

  getImageURL(page: string, image: string): string;

  /**
   *
   * @param page the id of the request page
   * @param syntax the syntax of the request page
   * @param requeue optional param informing whether an asynchronous update of
   *  the page content is allowed (default is true)
   * @return a promise wrapping a page data, or undefined in case of page not
   *  found
   *  @since 0.8
   */
  getPageContent(
    page: string,
    syntax: string,
    requeue?: boolean,
  ): Promise<PageData | undefined>;

  /**
   * @param page the page to get the attachments from
   * @return a promise wrapping an array of attachments, or undefined if the
   *  requested page is not found
   *
   * @since 0.9
   */
  getAttachments(page: string): Promise<PageAttachment[] | undefined>;

  getPanelContent(
    panel: string,
    contextPage: string,
    syntax: string,
  ): Promise<PageData>;

  getEditField(jsonArticle: object, fieldName: string): Promise<string>;

  isStorageReady(): Promise<boolean>;

  /**
   * Update the content of a given page with the provided content.
   *
   * @param page the page to save
   * @param content the content of the page
   * @param title the page title
   * @param syntax the syntax of the page
   * @return a promise on the save is done
   *
   * @since 0.8
   */
  save(
    page: string,
    title: string,
    content: string,
    syntax: string,
  ): Promise<unknown>;
}
