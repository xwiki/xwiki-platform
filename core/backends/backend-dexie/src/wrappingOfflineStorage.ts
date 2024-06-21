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

import { inject, injectable } from "inversify";
import "reflect-metadata";
import type {
  Document,
  Logger,
  PageAttachment,
  PageData,
  Storage,
  WikiConfig,
  WrappingStorage,
} from "@xwiki/cristal-api";
import type { QueueWorker } from "@xwiki/cristal-sharedworker-api";
import { type OfflineStorage } from "@xwiki/cristal-backend-api";

@injectable()
export class WrappingOfflineStorage implements WrappingStorage {
  public logger: Logger;
  public storage: Storage;
  public offlineStorage: OfflineStorage;
  public queueWorker: QueueWorker;

  constructor(
    @inject<Logger>("Logger") logger: Logger,
    @inject<OfflineStorage>("OfflineStorage") offlineStorage: OfflineStorage,
    @inject<QueueWorker>("QueueWorker") queueWorker: QueueWorker,
  ) {
    this.logger = logger;
    this.logger.setModule("storage.components.wrappingOfflineStorage");
    this.offlineStorage = offlineStorage;
    this.queueWorker = queueWorker;
  }

  public setStorage(storage: Storage): void {
    this.storage = storage;
  }

  public getStorage(): Storage {
    return this.storage;
  }

  public setWikiConfig(config: WikiConfig): void {
    return this.storage.setWikiConfig(config);
  }

  public getWikiConfig(): WikiConfig {
    return this.storage.getWikiConfig();
  }

  public async isStorageReady(): Promise<boolean> {
    return this.storage.isStorageReady();
  }

  public getPageRestURL(page: string, syntax: string): string {
    return this.storage.getPageRestURL(page, syntax);
  }

  public getPageFromViewURL(url: string): string | null {
    return this.storage.getPageFromViewURL(url);
  }

  public getImageURL(page: string, image: string): string {
    return this.storage.getImageURL(page, image);
  }

  public async getPageContent(
    page: string,
    syntax: string,
    requeue?: boolean,
  ): Promise<PageData | undefined> {
    this.logger.debug("Trying to get data for page ", page);
    if (this.offlineStorage) {
      this.logger.debug("Asking offline storage for ", page);
      const pageData = await this.offlineStorage.getPage(
        this.getWikiConfig().name,
        page + "_" + syntax,
      );
      if (pageData != null && pageData != undefined) {
        this.logger.debug("Loading data from local storage for page", page);
        // Adding page to refresh queue in shared worker
        if (requeue == undefined || requeue) {
          this.queueWorker.addToQueue(
            this.getWikiConfig().name + ":" + page + "_" + syntax,
          );
        }
        return pageData;
      } else {
        this.logger.debug(
          "Could not find data in local storage for page",
          page,
        );
        return this.savePageContent(page, syntax);
      }
    } else {
      this.logger.debug("No offline local storage available");
      return this.storage.getPageContent(page, syntax);
    }
  }

  public getAttachments(page: string): Promise<PageAttachment[] | undefined> {
    // TODO: add support for offline storage of attachments.
    return this.storage.getAttachments(page);
  }

  private async savePageContent(
    page: string,
    syntax: string,
  ): Promise<PageData> {
    const pageData = await this.storage.getPageContent(page, syntax);
    if (pageData) {
      pageData.id = page + "_" + syntax;
      this.logger.debug("Saving page to offline storage", page);
      this.offlineStorage.savePage(
        this.getWikiConfig().name,
        page + "_" + syntax,
        pageData,
      );

      return pageData;
    } else {
      throw new Error("Can't save missing page.");
    }
  }

  public async updatePageContent(
    page: string,
    syntax: string,
  ): Promise<boolean> {
    const currentPageData = await this.offlineStorage.getPage(
      this.getWikiConfig().name,
      page + "_" + syntax,
    );
    const pageData = await this.storage.getPageContent(page, syntax);
    if (currentPageData && pageData) {
      pageData.id = page + "_" + syntax;
      this.logger.debug("Saving page to offline storage", page);
      this.offlineStorage.savePage(
        this.getWikiConfig().name,
        page + "_" + syntax,
        pageData,
      );
      return true;
    } else if (currentPageData?.version != pageData?.version && pageData) {
      pageData.id = page + "_" + syntax;
      this.logger.debug("Updating page to offline storage", page);
      this.offlineStorage.updatePage(
        this.getWikiConfig().name,
        page + "_" + syntax,
        pageData,
      );
      return true;
    } else {
      this.logger.debug(
        "Local storage not updated, because version are identical",
      );
      return false;
    }
  }

  public async getPanelContent(
    panel: string,
    contextPage: string,
    syntax: string,
  ): Promise<PageData> {
    return this.storage.getPanelContent(panel, contextPage, syntax);
  }

  public async getEditField(
    document: Document,
    fieldName: string,
  ): Promise<string> {
    return await this.storage.getEditField(document, fieldName);
  }

  save(
    page: string,
    title: string,
    content: string,
    syntax: string,
  ): Promise<unknown> {
    return this.storage.save(page, title, content, syntax);
  }
}
