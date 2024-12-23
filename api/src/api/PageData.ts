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

import type { Document } from "./document";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

export interface PageData {
  id: string;
  name: string;
  source: string;
  syntax: string;
  html: string;
  headline: string;
  headlineRaw: string;
  document: Document;
  css: Array<string>;
  js: Array<string>;
  version: string | undefined;
  /**
   * Date of the last modification of this page.
   * @since 0.13
   */
  lastModificationDate: Date | undefined;
  /**
   * Name of the last user to edit this page.
   * @since 0.13
   */
  lastAuthor: UserDetails | undefined;
  /**
   * Indicate if the current user can edit this page.
   * @since 0.13
   */
  canEdit: boolean;

  // TODO: remove any
  toObject(): any; // eslint-disable-line

  // TODO: remove any
  fromObject(object: any): void; // eslint-disable-line
}
