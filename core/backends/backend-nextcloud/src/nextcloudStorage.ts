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
import {
  AttachmentsData,
  DefaultPageData,
  type Logger,
  PageAttachment,
  PageData,
} from "@xwiki/cristal-api";
import { AbstractStorage } from "@xwiki/cristal-backend-api";

// TODO: To be replaced by an actual authentication with CRISTAL-267
const USERNAME = "test";
const PASSWORD = "test";

/**
 * Access Nextcloud storage through http.
 * Read and write files to a ~/.cristal directory where all persistent data is
 * stored.
 *
 * @since 0.9
 */
@injectable()
export class NextcloudStorage extends AbstractStorage {
  private readonly ATTACHMENTS = "attachments";

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

    try {
      const response = await fetch(
        `${baseRestURL}/${USERNAME}/.cristal/${page}/page.json`,
        {
          method: "GET",
          headers,
        },
      );

      if (response.status >= 200 && response.status < 300) {
        const json = await response.json();

        return {
          ...json,
          id: page,
          headline: json.name,
          headlineRaw: json.name,
        };
      } else {
        return undefined;
      }
    } catch {
      return undefined;
    }
  }

  async getAttachments(page: string): Promise<AttachmentsData | undefined> {
    const response = await fetch(this.getAttachmentsBasePath(page), {
      method: "PROPFIND",
      headers: {
        ...this.getBaseHeaders(),
        Depth: "1",
        Accept: "application/json",
      },
    });

    if (response.status >= 200 && response.status < 300) {
      const text = await response.text();
      const data = new window.DOMParser().parseFromString(text, "text/xml");

      const responses = data.getElementsByTagName("d:response");
      const attachments: PageAttachment[] = [];
      for (let i = 0; i < responses.length; i++) {
        const dresponse = responses[i];
        if (dresponse.getElementsByTagName("d:getcontenttype").length > 0) {
          const id = dresponse.getElementsByTagName("d:href")[0].textContent!;
          const mimetype =
            dresponse.getElementsByTagName("d:getcontenttype")[0].textContent!;
          const segments = id.split("/");
          const href = `${this.getWikiConfig().baseRestURL}/${USERNAME}/${segments.slice(5).join("/")}`;
          const reference = segments[segments.length - 1];

          attachments.push({
            mimetype,
            reference,
            id,
            href,
          });
        }
      }

      return { attachments };
    } else {
      return undefined;
    }
  }

  private getAttachmentsBasePath(page: string) {
    return `${this.getWikiConfig().baseRestURL}/${USERNAME}/.cristal/${page}/${this.ATTACHMENTS}`;
  }

  async save(page: string, content: string, title: string): Promise<unknown> {
    // Splits the page reference along the / and create intermediate directories
    // for each segment, expect the last one where the content and title are
    // persisted.
    const directories = page.split("/");

    // Create the root directory. We also need to create all intermediate directories.
    const rootURL = `${this.getWikiConfig().baseRestURL}/${USERNAME}/.cristal`;
    await this.createIntermediateDirectories(rootURL, directories);

    await fetch(`${rootURL}/${directories.join("/")}/page.json`, {
      method: "PUT",
      headers: this.getBaseHeaders(),
      body: JSON.stringify({
        source: content,
        name: title,
        syntax: "markdown/1.2",
      }),
    });

    return;
  }

  private async createIntermediateDirectories(
    rootURL: string,
    directories: string[],
  ) {
    await this.createDirectory(rootURL);

    for (let i = 0; i < directories.length; i++) {
      // The intermediate directories must exist for the target file to be
      // accepted by webdav.
      await this.createDirectory(
        `${rootURL}/${directories.slice(0, i + 1).join("/")}`,
      );
    }
  }

  async saveAttachments(page: string, files: File[]): Promise<unknown> {
    return Promise.all(files.map((file) => this.saveAttachment(page, file)));
  }

  private async saveAttachment(page: string, file: File): Promise<unknown> {
    const directories = page.split("/");

    // Create the root directory. We also need to create all intermediate directories.
    const rootURL = `${this.getWikiConfig().baseRestURL}/${USERNAME}/.cristal`;
    await this.createIntermediateDirectories(rootURL, [
      ...directories,
      this.ATTACHMENTS,
    ]);

    const fileURL = this.getAttachmentsBasePath(page) + "/" + file.name;
    const formData = new FormData();
    formData.append(file.name, file);
    await fetch(fileURL, {
      method: "PUT",
      headers: this.getBaseHeaders(),
      body: formData,
    });
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

  private async createDirectory(currentTarget: string) {
    await fetch(currentTarget, {
      method: "MKCOL",
      headers: this.getBaseHeaders(),
    });
  }

  private getBaseHeaders() {
    // TODO: the authentication is currently hardcoded.
    return {
      Authorization: `Basic ${btoa(`${USERNAME}:${PASSWORD}`)}`,
    };
  }
}
