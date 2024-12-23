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

import { JSONLDDocument } from "./JSONLDDocument";
import type { PageData } from "../api/PageData";
import type { Document } from "../api/document";
import type { UserDetails } from "@xwiki/cristal-authentication-api";

export class DefaultPageData implements PageData {
  id: string;
  name: string;
  mode: string = "";
  source: string;
  syntax: string;
  html: string;
  document: Document;
  css: Array<string>;
  js: Array<string>;
  version: string;
  headline: string = "";
  headlineRaw: string = "";
  lastModificationDate: Date | undefined;
  lastAuthor: UserDetails | undefined;
  canEdit: boolean = true;

  public constructor(
    id: string = "",
    name: string = "",
    source: string = "",
    syntax: string = "",
  ) {
    this.document = new JSONLDDocument({});
    this.source = source;
    this.syntax = syntax;
    this.html = "";
    this.name = name;
    this.id = id;
    this.css = [];
    this.js = [];
    this.version = "";
  }

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  toObject(): any {
    return {
      id: this.id,
      name: this.name,
      source: this.source,
      syntax: this.syntax,
      html: this.html,
      document: this.document.getSource(),
      css: this.css,
      js: this.js,
      version: this.version,
    };
  }

  // TODO get rid of any
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  fromObject(object: any): void {
    this.id = object.id;
    this.name = object.name;
    this.source = object.source;
    this.syntax = object.syntax;
    this.html = object.html;
    this.document = new JSONLDDocument(object.document);
    this.css = object.css;
    this.js = object.js;
    this.version = object.version;
  }
}
