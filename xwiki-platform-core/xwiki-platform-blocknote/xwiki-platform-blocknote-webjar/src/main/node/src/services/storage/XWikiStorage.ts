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
import { AttachmentsData, PageAttachment, PageData } from "@xwiki/platform-api";
import { AbstractStorage } from "@xwiki/platform-backend-api";
import { Container, inject, injectable } from "inversify";
import type { Logger } from "@xwiki/platform-api";

@injectable("Singleton")
export class XWikiStorage extends AbstractStorage {
  public static override bind(container: Container): void {
    container
      .bind("Storage")
      .to(XWikiStorage)
      .inSingletonScope()
      .whenNamed("XWiki");
  }

  constructor(@inject("Logger") logger: Logger) {
    super(logger, "storage.components.xwikiStorage");
  }

  public async isStorageReady(): Promise<boolean> {
    return true;
  }

  public getPageRestURL(): string {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getPageFromViewURL(): string | null {
    // TODO
    throw new Error("Method not implemented.");
  }

  public getImageURL(): string {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async getPageContent(): Promise<PageData | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  async getAttachments(page: string): Promise<AttachmentsData | undefined> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async getAttachment(
    page: string,
    name: string,
  ): Promise<PageAttachment | undefined> {
    const attachments = await this.getAttachments(page);
    if (attachments) {
      return attachments.attachments.filter((a) => a.reference == name)[0];
    }
  }

  public async getPanelContent(): Promise<PageData> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async getEditField(): Promise<string> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async save(): Promise<unknown> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async saveAttachments(
    page: string,
    files: File[],
  ): Promise<undefined | (undefined | string)[]> {
    await Promise.all(files.map((file) => this.saveAttachment(page, file)));
    return undefined;
  }

  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  public async saveAttachment(page: string, file: File): Promise<unknown> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async delete(): Promise<{ success: boolean; error?: string }> {
    // TODO
    throw new Error("Method not implemented.");
  }

  public async move(): Promise<{ success: boolean; error?: string }> {
    // TODO
    throw new Error("Method not implemented.");
  }
}
