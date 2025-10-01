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
import { AttachmentsData, Document, PageAttachment, PageData, type Logger } from "@xwiki/cristal-api";
import { AbstractStorage } from "@xwiki/cristal-backend-api";
import { Container, inject, injectable } from "inversify";

@injectable("Singleton")
export class XWikiStorage extends AbstractStorage {
  public static override bind(container: Container): void {
    container.bind("Storage").to(XWikiStorage).inSingletonScope().whenNamed("XWiki");
  }

  constructor(@inject("Logger") logger: Logger) {
    super(logger, "storage.components.xwikiStorage");
  }

  public async isStorageReady(): Promise<boolean> {
    return true;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public getPageRestURL(page: string, syntax: string, revision?: string): string {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public getPageFromViewURL(url: string): string | null {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public getImageURL(page: string, image: string): string {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async getPageContent(page: string, syntax: string, revision?: string): Promise<PageData | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async getAttachments(page: string): Promise<AttachmentsData | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async getAttachment(page: string, name: string): Promise<PageAttachment | undefined> {
    const attachments = await this.getAttachments(page);
    if (attachments) {
      return attachments.attachments.filter((a) => a.reference == name)[0];
    }
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async getPanelContent(panel: string, contextPage: string): Promise<PageData> {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async getEditField(document: Document, fieldName: string): Promise<string> {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async save(page: string, title: string, content: string): Promise<unknown> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async saveAttachments(page: string, files: File[]): Promise<unknown> {
    return Promise.all(files.map((file) => this.saveAttachment(page, file)));
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async saveAttachment(page: string, file: File): Promise<unknown> {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async delete(page: string): Promise<{ success: boolean; error?: string }> {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async move(): Promise<{ success: boolean; error?: string }> {
    // TODO
    throw new Error("Method not implemented.");
  }
}
