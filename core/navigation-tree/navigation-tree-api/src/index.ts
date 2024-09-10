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

/**
 * Description of a navigation tree node.
 * @since 0.10
 */
type NavigationTreeNode = {
  id: string;
  label: string;
  url: string;
  has_children: boolean;
};

/**
 * A NavigationTreeSource computes and returns a wiki's navigation tree.
 *
 * @since 0.10
 **/
interface NavigationTreeSource {
  /**
   * Returns the direct child nodes for a given page id in the navigation tree.
   * If the page id is omitted, returns the root nodes instead.
   *
   * @param id the page id
   * @returns the descendants in the navigation tree
   */
  getChildNodes(id?: string): Promise<Array<NavigationTreeNode>>;
}

/**
 * A NavigationTreeSourceProvider returns the instance of NavigationTreeSource
 * matching the current wiki configuration.
 *
 * @since 0.10
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
 */
const name = "NavigationTreeSource";

export {
  type NavigationTreeNode,
  type NavigationTreeSource,
  type NavigationTreeSourceProvider,
  name,
};
