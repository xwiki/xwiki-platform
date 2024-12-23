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

import { PageAttachment, PageData } from "@xwiki/cristal-api";
import { LinkType } from "@xwiki/cristal-link-suggest-api";
import { EntityType } from "@xwiki/cristal-model-api";

export interface APITypes {
  resolvePath(page: string): Promise<string>;

  resolveAttachmentsPath(page: string): Promise<string>;

  resolveAttachmentPath(page: string, filename: string): Promise<string>;

  readPage(path: string): Promise<PageData>;

  readAttachments(path: string): Promise<PageAttachment[]>;

  readAttachment(path: string): Promise<PageAttachment>;

  savePage(path: string, content: string, title: string): Promise<PageData>;

  saveAttachment(path: string, file: File): Promise<PageData>;

  listChildren(page: string): Promise<Array<string>>;

  deletePage(path: string): Promise<void>;

  /**
   *
   * @param query - search query
   * @param type - the type of attach
   * @param mimetype - the expected mimetype, only applied for attachments
   * @since 0.13
   */
  search(
    query: string,
    type?: LinkType,
    mimetype?: string,
  ): Promise<
    (
      | { type: EntityType.ATTACHMENT; value: PageAttachment }
      | { type: EntityType.DOCUMENT; value: PageData }
    )[]
  >;
}
