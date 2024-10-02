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
  PageData,
} from "@xwiki/cristal-api";
import { AbstractStorage } from "@xwiki/cristal-backend-api";
import { APITypes } from "../electron/preload/apiTypes";

declare const fileSystemStorage: APITypes;

@injectable()
export default class FileSystemStorage extends AbstractStorage {
  constructor(@inject<Logger>("Logger") logger: Logger) {
    super(logger, "storage.components.fileSystemStorage");
  }

  getEditField(): Promise<string> {
    return Promise.resolve("");
  }

  getImageURL(): string {
    return "";
  }

  async getPageContent(page: string): Promise<PageData> {
    const decodedPage = decodeURI(page);
    const path = await fileSystemStorage.resolvePath(decodedPage);
    const pageData = await fileSystemStorage.readPage(path || "");
    if (pageData) {
      pageData.id = decodedPage;
      pageData.headline = pageData.name;
      pageData.headlineRaw = pageData.name;
    }
    return pageData;
  }

  async getAttachments(page: string): Promise<AttachmentsData | undefined> {
    const path = await fileSystemStorage.resolveAttachmentsPath(page);
    return {
      attachments: await fileSystemStorage.readAttachments(path),
    };
  }

  getPageFromViewURL(): string | null {
    return null;
  }

  getPageRestURL(): string {
    return "";
  }

  getPanelContent(): Promise<PageData> {
    return Promise.resolve(new DefaultPageData());
  }

  isStorageReady(): Promise<boolean> {
    return Promise.resolve(true);
  }

  async save(page: string, content: string, title: string): Promise<void> {
    const path = await fileSystemStorage.resolvePath(page);
    await fileSystemStorage.savePage(path, content, title);
  }

  async saveAttachments(page: string, files: File[]): Promise<unknown> {
    return Promise.all(files.map((file) => this.saveAttachment(page, file)));
  }

  private async saveAttachment(page: string, file: File): Promise<unknown> {
    const path = await fileSystemStorage.resolveAttachmentPath(page, file.name);
    await fileSystemStorage.saveAttachment(path, file);
    return;
  }
}
