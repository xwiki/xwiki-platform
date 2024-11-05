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

// eslint-disable-next-line import/no-named-as-default
import Dexie from "dexie";
import type { Table } from "dexie";

export default class DexiePageStorage extends Dexie {
  pages!: Table<object>;

  constructor(wikiName: string) {
    super("pages_" + wikiName);
    this.version(2).stores({
      pages:
        // Primary key and indexed props
        "id, name, content, document, css, js, document.name, document.headline, document.creator",
    });
  }
}
