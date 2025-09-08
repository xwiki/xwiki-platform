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

import type { PageData } from "@xwiki/cristal-api";

/**
 * A PageRenameManager can handle page rename operations.
 *
 * @since 0.14
 * @beta
 **/
interface PageRenameManager {
  /**
   * Change the reference of a given page.
   *
   * @param pageData - the page for which to get the revisions
   * @param newReference - the new reference for the page
   * @param preserveChildren - whether to also affect children
   * @returns true if this was successful, false with the reason otherwise
   */
  updateReference(
    page: PageData,
    newReference: string,
    preserveChildren: boolean,
  ): Promise<{ success: boolean; error?: string }>;

  /* TODO: Fix CRISTAL-84 and add operations to update backlinks and set-up
     automatic redirects. */
}

/**
 * A PageRenameManagerProvider returns the instance of {@link PageRenameManager}
 * matching the current wiki configuration.
 *
 * @since 0.14
 * @beta
 **/
interface PageRenameManagerProvider {
  /**
   * Check whether an instance of PageRenameManager matching the current wiki
   * configuration exists.
   *
   * @returns whether or not an instance exists
   */
  has(): boolean;

  /**
   * Return the instance of PageRenameManager matching the current wiki
   * configuration.
   *
   * @returns the instance of PageRenameManager
   */
  get(): PageRenameManager;
}

/**
 * The component id of PageRenameManager.
 * @since 0.14
 * @beta
 */
const name = "PageRenameManager";

export { type PageRenameManager, type PageRenameManagerProvider, name };
