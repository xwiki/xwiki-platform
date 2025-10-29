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

import { EntityType } from "@xwiki/cristal-model-api";
import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { PageHierarchyItem } from "@xwiki/cristal-hierarchy-api";
import type {
  DocumentReference,
  SpaceReference,
} from "@xwiki/cristal-model-api";

/**
 * Returns the page hierarchy for a path-like page id.
 * This does not include a Home segment.
 *
 * @param page - the reference to the page
 * @param cristalApp - the current app
 * @returns the page hierarchy
 * @since 0.15
 * @beta
 **/
// TODO: reduce the number of statements in the following method and reactivate the disabled eslint rule.
// eslint-disable-next-line max-statements
export async function getPageHierarchyFromPath(
  page: DocumentReference | SpaceReference,
  cristalApp: CristalApp,
): Promise<Array<PageHierarchyItem>> {
  const hierarchy: Array<PageHierarchyItem> = [];
  const fileHierarchy = [];
  if (page.type == EntityType.SPACE) {
    fileHierarchy.push(...page.names);
  } else if (page.type == EntityType.DOCUMENT) {
    fileHierarchy.push(...(page.space?.names ?? []), page.name);
  }
  let currentFile = "";

  for (let i = 0; i < fileHierarchy.length; i++) {
    const file = fileHierarchy[i];
    currentFile += `${i == 0 ? "" : "/"}${file}`;
    const currentPageData: PageData | undefined =
      await cristalApp.getPage(currentFile);
    hierarchy.push({
      label: currentPageData?.name ? currentPageData.name : file,
      pageId: currentFile,
      url: cristalApp.getRouter().resolve({
        name: "view",
        params: { page: currentFile },
      }).href,
    });
  }

  return hierarchy;
}
