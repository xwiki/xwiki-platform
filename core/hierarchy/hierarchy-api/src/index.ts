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
 * Description of a hierarchy item for a given page.
 * @since 0.9
 */
type PageHierarchyItem = {
  label: string;
  url: string;
};

/**
 * A PageHierarchyResolver computes and returns the hierarchy for a given page.
 *
 * @since 0.9
 **/
interface PageHierarchyResolver {
  /**
   * Returns the page hierarchy for a given page.
   *
   * @param pageData the page for which to compute the hierarchy
   * @returns the page hierarchy
   */
  getPageHierarchy(pageData: PageData): Promise<Array<PageHierarchyItem>>;
}

/**
 * A PageHierarchyResolverProvider returns the instance of PageHierarchyResolver
 * matching the current wiki configuration.
 *
 * @since 0.9
 **/
interface PageHierarchyResolverProvider {
  /**
   * Returns the instance of PageHierarchyResolver matching the current wiki
   * configuration.
   *
   * @returns the instance of PageHierarchyResolver
   */
  get(): PageHierarchyResolver;
}

/**
 * The component id of PageHierarchyResolver.
 * @since 0.9
 */
const name = "PageHierarchyResolver";

export {
  PageHierarchyItem,
  PageHierarchyResolver,
  PageHierarchyResolverProvider,
  name,
};
