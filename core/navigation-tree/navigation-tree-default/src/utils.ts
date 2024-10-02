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

import type { PageData } from "@xwiki/cristal-api";

/**
 * Returns the ids of the parents nodes for a path-like page id.
 *
 * @param pageData the data of the page
 * @returns the parents nodes ids
 * @since 0.10
 **/
export function getParentNodesIdFromPath(page?: PageData): Array<string> {
  const result: Array<string> = [];
  if (page) {
    // TODO: Use a page resolver instead when CRISTAL-234 is fixed.
    const parents = page.id.split("/");
    let currentParent = "";
    let i;
    for (i = 0; i < parents.length - 1; i++) {
      currentParent += parents[i];
      result.push(currentParent);
      currentParent += "/";
    }
    result.push(page.id);
  }
  return result;
}
