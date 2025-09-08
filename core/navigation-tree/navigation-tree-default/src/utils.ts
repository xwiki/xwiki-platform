/**
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

import type { DocumentReference } from "@xwiki/cristal-model-api";

/**
 * Returns the ids of the parents nodes for a path-like page id.
 *
 * @param page - the page reference
 * @param includeRootNode - whether to include a root node with empty id (default: false)
 * @returns the parents nodes ids
 * @since 0.20
 * @beta
 **/
export function getParentNodesIdFromPath(
  page?: DocumentReference,
  includeRootNode?: boolean,
): Array<string> {
  const result: Array<string> = includeRootNode ? [""] : [];
  if (page) {
    const parents = [...(page.space?.names ?? []), page.name];
    let currentParent = "";
    for (let i = 0; i < parents.length; i++) {
      currentParent += parents[i];
      result.push(currentParent);
      currentParent += "/";
    }
  }
  return result;
}
