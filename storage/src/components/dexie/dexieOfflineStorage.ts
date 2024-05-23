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

import type { PageData } from "@xwiki/cristal-api";
import { DefaultPageData, type Logger } from "@xwiki/cristal-api";
import type OfflineStorage from "../../api/offlineStorage";
import DexiePageStorage from "./dexiePageStorage";
import { inject, injectable } from "inversify";

@injectable()
export default class DexieOfflineStorage implements OfflineStorage {
  private pageStorageMap: Map<string, DexiePageStorage> = new Map<
    string,
    DexiePageStorage
  >();
  private logger: Logger;

  constructor(@inject<Logger>("Logger") logger: Logger) {
    this.logger = logger;
    this.logger.setModule("storage.components.dexieOfflineStorage");
  }

  getPageStorage(wikiName: string): DexiePageStorage {
    let pageStorage = this.pageStorageMap.get(wikiName);
    if (pageStorage == null) {
      pageStorage = new DexiePageStorage(wikiName);
      this.pageStorageMap.set(wikiName, pageStorage);
    }
    return pageStorage;
  }

  async getPage(wikiName: string, id: string): Promise<PageData | undefined> {
    try {
      this.logger.debug("Ready to load page from local storage", id);
      const pageStorage = this.getPageStorage(wikiName);
      this.logger.debug("pageStorage is ready, asking for page");
      const pageObject = await pageStorage.pages.get(id);
      const pageData = new DefaultPageData();
      pageData.fromObject(pageObject);
      return pageData;
    } catch (e) {
      this.logger.debug("Exception while trying to read from local storage", e);
      return undefined;
    }
  }

  async savePage(wikiName: string, id: string, page: PageData): Promise<void> {
    try {
      const pageStorage = this.getPageStorage(wikiName);
      await pageStorage.pages.add(page.toObject(), id);
    } catch (e) {
      this.logger.debug(
        "Exception while trying to store in local storage",
        wikiName,
        id,
        page,
        e,
      );
    }
  }

  async updatePage(
    wikiName: string,
    id: string,
    page: PageData,
  ): Promise<void> {
    try {
      const pageStorage = this.getPageStorage(wikiName);
      this.logger.debug("Ready to store", page);
      await pageStorage.pages.update(id, page.toObject());
    } catch (e) {
      this.logger.debug(
        "Exception while trying to store in local storage",
        wikiName,
        id,
        page,
        e,
      );
    }
  }
}
