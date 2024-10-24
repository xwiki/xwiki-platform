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

import type { CristalApp, PageData } from "@xwiki/cristal-api";
import type { PageHierarchyItem } from "@xwiki/cristal-hierarchy-api";

/**
 * Returns the page hierarchy for a path-like page id.
 * This does not include a Home segment.
 *
 * @param pageData - the data of the page
 * @param cristalApp - the current app
 * @returns the page hierarchy
 * @since 0.10
 **/
export async function getPageHierarchyFromPath(
  pageData: PageData,
  cristalApp: CristalApp,
): Promise<Array<PageHierarchyItem>> {
  const hierarchy: Array<PageHierarchyItem> = [];
  const fileHierarchy = pageData.id.split("/");
  let currentFile = "";

  for (let i = 0; i < fileHierarchy.length; i++) {
    const file = fileHierarchy[i];
    currentFile += `${i == 0 ? "" : "/"}${file}`;
    let currentPageData: PageData | undefined;
    if (i == fileHierarchy.length - 1) {
      currentPageData = pageData;
    } else {
      currentPageData = await cristalApp.getPage(currentFile);
    }
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
