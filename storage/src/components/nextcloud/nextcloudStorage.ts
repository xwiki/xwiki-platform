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

import { injectable, inject } from "inversify";
import { DefaultPageData, Logger, PageData } from "@xwiki/cristal-api";
import { AbstractStorage } from "../abstractStorage";

/**
 * Access Nextcloud storage through http.
 * Read and write files to a ~/.cristal directory where all persistent data is
 * stored.
 *
 * @since 0.9
 */
@injectable()
export class NextcloudStorage extends AbstractStorage {
  constructor(@inject<Logger>("Logger") logger: Logger) {
    super(logger, "storage.components.nextcloudStorage");
  }

  async getEditField(): Promise<string> {
    // TODO: unsupported
    return "";
  }

  getImageURL(): string {
    // TODO: unsupported
    return "";
  }

  async getPageContent(page: string): Promise<PageData | undefined> {
    const baseRestURL = this.getWikiConfig().baseRestURL;
    const headers = this.getBaseHeaders();

    const response = await fetch(`${baseRestURL}/.cristal/${page}.json`, {
      method: "GET",
      headers,
    });

    if (response.status >= 200 && response.status < 300) {
      const json = await response.json();

      return {
        ...json,
        headline: json.name,
        headlineRaw: json.name,
      };
    } else {
      return undefined;
    }
  }

  async save(page: string, content: string, title: string): Promise<unknown> {
    // Splits the page reference along the / and create intermediate directories
    // for each segment, expect the last one where the content and title are
    // persisted.
    const directories = page.split("/");

    const headers = this.getBaseHeaders();
    const baseRestURL = this.getWikiConfig().baseRestURL;
    // Create the root directory. We also need to create all intermediate directories.
    let currentTarget = `${baseRestURL}/.cristal`;
    await this.createDirectory(currentTarget, headers);

    for (let i = 0; i < directories.length; i++) {
      currentTarget = `${currentTarget}/${directories[i]}`;
      if (i < directories.length - 1) {
        // The intermediate directories must exist for a the target file to be
        // accepted by webdav.
        await this.createDirectory(currentTarget, headers);
      } else {
        await fetch(`${currentTarget}.json`, {
          method: "PUT",
          headers: headers,
          body: JSON.stringify({
            source: content,
            name: title,
            syntax: "markdown/1.2",
          }),
        });
      }
    }

    return;
  }

  getPageFromViewURL(): string | null {
    return null;
  }

  getPageRestURL(): string {
    return "";
  }

  async getPanelContent(): Promise<PageData> {
    // TODO: unsupported
    return new DefaultPageData();
  }

  async isStorageReady(): Promise<boolean> {
    return true;
  }

  private async createDirectory(
    currentTarget: string,
    headers: { Authorization: string },
  ) {
    await fetch(currentTarget, {
      method: "MKCOL",
      headers: headers,
    });
  }

  private getBaseHeaders() {
    // TODO: the authentication is currently hardcoded.
    return {
      Authorization: `Basic ${btoa("test:test")}`,
    };
  }
}
