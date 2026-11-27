/**
 * See the NOTICE file distributed with this work for additional
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
import type { BlockType, InlineContentType } from "../blocknote";

/**
 * A node of the BlockNote content: a block, a table cell, some inline content or a text.
 *
 * @since 18.10.0
 * @beta
 */
type NodeType =
  | BlockType
  | { type: "tableCell"; content: InlineContentType[] }
  | InlineContentType
  | string;

/**
 * Used to visit BlockNote nodes (blocks and inline content).
 *
 * @since 18.10.0
 * @beta
 */
interface BlockNoteVisitor {
  /**
   * Visit a BlockNote node (block or inline content).
   *
   * @param node - the node to visit
   * @param macro - the macro node that holds the visited node in the parameters or the content of its call, if any (the
   *   parameters and the content of a macro call that are editable in-place are BlockNote fragments, which only some
   *   iterators enter); undefined when the visited node is part of the iterated content
   * @returns true if the iterator should not visit the children of this node (nor the nested macro calls, for a macro
   *   node), false otherwise
   */
  visit(node: NodeType, macro?: object): boolean;
}

/**
 * The name under which the {@link BlockNoteIterator} is bound in the service container.
 *
 * @since 18.10.0
 * @beta
 */
const blockNoteIteratorName = "BlockNoteIterator";

/**
 * Used to iterate BlockNote nodes (blocks and inline content).
 *
 * @since 18.10.0
 * @beta
 */
interface BlockNoteIterator {
  /**
   * Iterates the nodes of the given BlockNote tree, calling the provided visitor for each node.
   *
   * @param blockNoteContent - the BlockNote content to iterate, i.e. the top level nodes (blocks) of the BlockNote tree
   * @param visitor - the visitor to call for each node of the BlockNote tree
   */
  iterate(blockNoteContent: BlockType[], visitor: BlockNoteVisitor): void;
}

export { blockNoteIteratorName };
export type { BlockNoteIterator, BlockNoteVisitor, NodeType };
