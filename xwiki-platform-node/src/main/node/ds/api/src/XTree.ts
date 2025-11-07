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

import type { TreeNode } from "@xwiki/platform-fn-utils";

/**
 * Represents a TreeNode that can be displayed in a Tree component.
 * @since 0.23
 * @beta
 */
type DisplayableTreeNode = TreeNode<{
  id: string;
  label: string;
  url?: string;
  activatable?: boolean;
}>;

/**
 * Props of the Tree component.
 * @since 0.23
 * @beta
 */
type TreeProps = {
  /**
   * Node that contains the nodes to display.
   */
  rootNode: DisplayableTreeNode;
  /**
   * Whether to display the root node itself (default: false).
   */
  showRootNode?: boolean;
  /**
   * Model value that contains the id of the current activated node.
   */
  activated?: string;
  /**
   * Model value that contains the ids of the current opened nodes.
   */
  opened?: string[];
};

export type { DisplayableTreeNode, TreeProps };
