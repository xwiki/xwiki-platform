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
import { PASSWORD, USERNAME } from "@xwiki/cristal-authentication-nextcloud";
import { AbstractStorage } from "@xwiki/cristal-backend-api";
import { inject, injectable } from "inversify";
import type { Logger } from "@xwiki/cristal-api";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

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
        const { lastModificationDate, lastAuthor } =
          await this.getLastEditDetails(page);

        const json = await response.json();

        return {
          ...json,
          id: page,
          headline: json.name,
          headlineRaw: json.name,
          lastAuthor: lastAuthor,
          lastModificationDate: lastModificationDate,
          canEdit: true,
        };
      } else {
        return undefined;
      }
    } catch {
      return undefined;
    }
  }

  private async getLastEditDetails(
    page: string,
  ): Promise<{ lastModificationDate?: Date; lastAuthor?: UserDetails }> {
    let lastModificationDate: Date | undefined;
    let lastAuthor: UserDetails | undefined;
    const response = await fetch(
      `${this.getWikiConfig().baseRestURL}/${USERNAME}/.cristal/${page}/page.json`,
      {
        body: `<?xml version="1.0" encoding="UTF-8"?>
          <d:propfind xmlns:d="DAV:">
            <d:prop xmlns:oc="http://owncloud.org/ns">
              <d:getlastmodified />
              <oc:owner-display-name />
            </d:prop>
          </d:propfind>`,
        method: "PROPFIND",
        headers: {
          ...this.getBaseHeaders(),
          Accept: "application/json",
        },
      },
    );
    if (response.status >= 200 && response.status < 300) {
      const data = new window.DOMParser().parseFromString(
        await response.text(),
        "text/xml",
      );

      const modified =
        data.getElementsByTagName("d:getlastmodified")[0]?.innerHTML;
      if (modified) {
        lastModificationDate = new Date(Date.parse(modified));
      }
      lastAuthor = {
        name: data.getElementsByTagName("oc:owner-display-name")[0]?.innerHTML,
      };
    }

    return { lastModificationDate, lastAuthor };
  }

  // TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
  // eslint-disable-next-line max-statements
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
          attachments.push(this.parseAttachment(dresponse));
        }
      }

      return { attachments };
    } else {
      return undefined;
    }
  }

  async getAttachment(
    page: string,
    name: string,
  ): Promise<PageAttachment | undefined> {
    const propfindResponse = await fetch(
      this.getAttachmentBasePath(name, page),
      {
        method: "PROPFIND",
        headers: {
          ...this.getBaseHeaders(),
          Depth: "1",
          Accept: "application/json",
        },
      },
    );

    if (propfindResponse.status >= 200 && propfindResponse.status < 300) {
      const text = await propfindResponse.text();
      const data = new window.DOMParser().parseFromString(text, "text/xml");
      const response = data.getElementsByTagName("d:response")[0];
      return this.parseAttachment(response);
    } else {
      return undefined;
    }
  }

  private getAttachmentsBasePath(page: string) {
    return `${this.getWikiConfig().baseRestURL}/${USERNAME}/.cristal/${page}/${this.ATTACHMENTS}`;
  }

  private getAttachmentBasePath(name: string, page: string) {
    return `${this.getAttachmentsBasePath(page)}/${name}`;
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
    await fetch(fileURL, {
      method: "PUT",
      headers: this.getBaseHeaders(),
      body: file,
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

  async delete(page: string): Promise<{ success: boolean; error?: string }> {
    const rootURL = `${this.getWikiConfig().baseRestURL}/${USERNAME}/.cristal`;
    const success = await fetch(`${rootURL}/${page}`, {
      method: "DELETE",
      headers: this.getBaseHeaders(),
    }).then(async (response) => {
      if (response.ok) {
        return { success: true };
      } else {
        return { success: false, error: await response.text() };
      }
    });
    return success;
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

  private parseAttachment(element: Element): PageAttachment {
    const id = element.getElementsByTagName("d:href")[0].textContent!;
    const mimetype =
      element.getElementsByTagName("d:getcontenttype")[0].textContent!;
    const size = parseInt(
      element.getElementsByTagName("d:getcontentlength")[0].textContent!,
    );
    const date = Date.parse(
      element.getElementsByTagName("d:getlastmodified")[0].textContent!,
    );
    const segments = id.split("/");
    const href = `${this.getWikiConfig().baseRestURL}/${USERNAME}/${segments.slice(5).join("/")}`;
    const reference = segments[segments.length - 1];

    return {
      mimetype,
      reference,
      id,
      href,
      size,
      date: new Date(date),
      // For now nextcloud does not allow for shared Cristal storage, so all files are owned by the current user
      author: undefined,
    };
  }
}
