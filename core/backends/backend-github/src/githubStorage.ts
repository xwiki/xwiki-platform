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

import { inject, injectable } from "inversify";
import "reflect-metadata";
import {
  DefaultPageData,
  Logger,
  PageAttachment,
  PageData,
  WikiConfig,
} from "@xwiki/cristal-api";
import { AbstractStorage } from "@xwiki/cristal-backend-api";

@injectable()
export class GitHubStorage extends AbstractStorage {
  public wikiConfig: WikiConfig;
  public logger: Logger;

  constructor(@inject<Logger>("Logger") logger: Logger) {
    super(logger, "storage.components.githubStorage");
  }

  public async isStorageReady(): Promise<boolean> {
    return true;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  getPageRestURL(page: string, syntax: string): string {
    this.logger?.debug("GitHub Loading page", page);
    return this.wikiConfig.baseRestURL + page;
  }

  getPageFromViewURL(url: string): string | null {
    let page = null;
    if (url.startsWith(this.wikiConfig.baseURL)) {
      const uri = url.replace(this.wikiConfig.baseURL, "");
      page = uri;
    }
    return page;
  }

  getImageURL(page: string, image: string): string {
    return this.wikiConfig.baseRestURL + image;
  }

  hashCode = function (str: string): string {
    let hash = 0,
      i,
      chr;
    if (str.length === 0) {
      return "" + hash;
    }
    for (i = 0; i < str.length; i++) {
      chr = str.charCodeAt(i);
      hash = (hash << 5) - hash + chr;
      hash |= 0; // Convert to 32bit integer
    }
    return "" + hash;
  };

  async getPageContent(page: string, syntax: string): Promise<PageData> {
    this.logger?.debug("GitHub Loading page", page);
    const url = this.getPageRestURL(page, syntax);
    const response = await fetch(url, { cache: "no-store" });
    const text = await response.text();
    let content = "";
    if (syntax == "json") {
      content = "";
    } else if (syntax == "md") {
      content = text;
    } else if (syntax == "html") {
      content = text;
    } else {
      content = "";
    }

    const pageContentData = new DefaultPageData();
    pageContentData.source = content;
    pageContentData.syntax = "md";
    pageContentData.css = [];
    pageContentData.version = this.hashCode(content);
    return pageContentData;
  }

  /**
   * @since 0.9
   */
  getAttachments(): Promise<PageAttachment[] | undefined> {
    // TODO: to be implemented.
    throw new Error("unsupported");
  }

  async getPanelContent(): Promise<PageData> {
    return new DefaultPageData();
  }

  async getEditField(): Promise<string> {
    return "";
  }

  save(): Promise<unknown> {
    throw new Error("Save not supported");
  }
}
