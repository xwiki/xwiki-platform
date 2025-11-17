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
import type { AttachmentsData } from "./attachmentsData";
import type { PageAttachment } from "./pageAttachment";

/**
 * @since 0.1
 * @beta
 */
export interface Storage {
  setWikiConfig(config: WikiConfig): void;

  getWikiConfig(): WikiConfig;

  getPageRestURL(page: string, syntax: string, revision?: string): string;

  getPageFromViewURL(url: string): string | null;

  getImageURL(page: string, image: string): string;

  /**
   *
   * @param page - the id of the request page
   * @param syntax - the syntax of the request page
   * @param revision - the revision requested, undefined will default to latest
   * @param requeue - optional param informing whether an asynchronous update of
   *  the page content is allowed (default is true)
   * @returns a promise wrapping a page data, or undefined in case of page not
   *  found
   *  @since 0.8
   */
  getPageContent(
    page: string,
    syntax: string,
    revision?: string,
    requeue?: boolean,
  ): Promise<PageData | undefined>;

  /**
   * @param page - the page to get the attachments from
   * @returns a promise wrapping an array of attachments and an optional count, or undefined if the
   *  requested page is not found
   *
   * @since 0.9
   * @beta
   */
  getAttachments(page: string): Promise<AttachmentsData | undefined>;

  /**
   * @param page - the attachment page name
   * @param name - the attachment name
   * @returns a promise wrapping the attachment data, or undefined if the requested attachment is not found
   * @since 0.12
   * @beta
   */
  getAttachment(
    page: string,
    name: string,
  ): Promise<PageAttachment | undefined>;

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
   * @param page - the page to save
   * @param content - the content of the page
   * @param title - the page title
   * @param syntax - the syntax of the page
   * @returns a promise when the save is done
   *
   * @since 0.8
   * @beta
   */
  save(
    page: string,
    title: string,
    content: string,
    syntax: string,
  ): Promise<unknown>;

  /**
   * @param page - the serialized reference of the page
   * @param files - the list of files to upload
   * @returns (since 0.20) an optional list of resolved attachments URL (in the same order as the provided files). This
   *   is useful in the case where the url cannot be resolved from the name of the file and its document reference
   *   alone.
   * @since 0.9
   * @beta
   */
  saveAttachments(
    page: string,
    files: File[],
  ): Promise<undefined | (undefined | string)[]>;

  /**
   * Delete a page.
   *
   * @param page - the page to delete
   * @returns true if the delete was successful, false with the reason otherwise
   *
   * @since 0.11
   * @beta
   */
  delete(page: string): Promise<{ success: boolean; error?: string }>;

  /**
   * Move a page.
   *
   * @param page - the page to move
   * @param newPage - the new location for the page
   * @param preserveChildren - whether to move children
   * @returns true if the move was successful, false with the reason otherwise
   *
   * @since 0.14
   * @beta
   */
  move(
    page: string,
    newPage: string,
    preserveChildren: boolean,
  ): Promise<{ success: boolean; error?: string }>;
}
