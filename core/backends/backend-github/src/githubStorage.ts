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
  DefaultPageData,
  PageAttachment,
  PageData,
} from "@xwiki/cristal-api";
import { AbstractStorage } from "@xwiki/cristal-backend-api";
import { inject, injectable } from "inversify";
import type { Logger } from "@xwiki/cristal-api";

@injectable()
export class GitHubStorage extends AbstractStorage {
  constructor(@inject<Logger>("Logger") logger: Logger) {
    super(logger, "storage.components.githubStorage");
  }

  public async isStorageReady(): Promise<boolean> {
    return true;
  }

  getPageRestURL(page: string, _syntax: string, revision?: string): string {
    this.logger?.debug("GitHub Loading page", page);
    let baseRestURL = this.wikiConfig.baseRestURL;
    if (revision) {
      baseRestURL = baseRestURL.replace(/\/[^/]*\/$/, `/${revision}/`);
    }
    return baseRestURL + page;
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
    const directory = page.replace(/[^/]*$/, "");
    return `${this.wikiConfig.baseRestURL}${directory}${image}`;
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

  async getPageContent(
    page: string,
    syntax: string,
    revision?: string,
  ): Promise<PageData> {
    this.logger?.debug("GitHub Loading page", page);
    const url = this.getPageRestURL(page, syntax, revision);
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
    pageContentData.id = page;
    pageContentData.name = page.split("/").pop()!;
    pageContentData.source = content;
    pageContentData.syntax = "md";
    pageContentData.css = [];
    pageContentData.version = this.hashCode(content);
    return pageContentData;
  }

  /**
   * @since 0.9
   */
  getAttachments(): Promise<AttachmentsData | undefined> {
    // TODO: to be implemented.
    throw new Error("unsupported");
  }

  getAttachment(): Promise<PageAttachment | undefined> {
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

  async saveAttachments(): Promise<unknown> {
    // TODO: to be implemented
    throw new Error("unsupported");
  }

  async delete(): Promise<{ success: boolean; error?: string }> {
    // TODO: to be implemented
    throw new Error("Delete not supported");
  }
}
