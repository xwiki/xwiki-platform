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

import type {
  DocumentReference,
  SpaceReference,
} from "@xwiki/platform-model-api";

/**
 * Description of a navigation tree node.
 * @since 0.10
 * @beta
 */
type NavigationTreeNode = {
  /** the id of a node, used by the NavigationTreeSource to access children */
  id: string;
  label: string;
  /** the location of the corresponding page on Cristal */
  location: SpaceReference | DocumentReference;
  url: string;
  has_children: boolean;
  /**
   * Whether this node corresponds to a terminal page.
   * @since 0.16
   * @beta
   */
  is_terminal: boolean;
};

/**
 * A NavigationTreeSource computes and returns a wiki's navigation tree.
 *
 * @since 0.10
 * @beta
 **/
interface NavigationTreeSource {
  /**
   * Returns the direct child nodes for a given page id in the navigation tree.
   * If the page id is omitted, returns the root nodes instead.
   *
   * @param id - the page id
   * @returns the descendants in the navigation tree
   */
  getChildNodes(id?: string): Promise<Array<NavigationTreeNode>>;

  /**
   * Returns the ids of the parents nodes for a given page.
   *
   * @param page - the reference to the page
   * @param includeTerminal - whether to include the final terminal page (default: true)
   * @param includeRootNode - whether to include a root node with empty id (default: false)
   * @returns the parents nodes ids
   * @since 0.20
   * @beta
   **/
  getParentNodesId(
    page: DocumentReference,
    includeTerminal?: boolean,
    includeRootNode?: boolean,
  ): Array<string>;
}

/**
 * A NavigationTreeSourceProvider returns the instance of NavigationTreeSource
 * matching the current wiki configuration.
 *
 * @since 0.10
 * @beta
 **/
interface NavigationTreeSourceProvider {
  /**
   * Returns the instance of NavigationTreeSource matching the current wiki
   * configuration.
   *
   * @returns the instance of NavigationTreeSource
   */
  get(): NavigationTreeSource;
}

/**
 * The component id of NavigationTreeSource.
 * @since 0.10
 * @beta
 */
const name = "NavigationTreeSource";

export {
  type NavigationTreeNode,
  type NavigationTreeSource,
  type NavigationTreeSourceProvider,
  name,
};
