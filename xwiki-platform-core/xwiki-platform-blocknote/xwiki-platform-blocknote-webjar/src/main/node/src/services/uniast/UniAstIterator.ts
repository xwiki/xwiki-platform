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
import {
  Block,
  InlineContent,
  ListItem,
  TableCell,
  TableColumn,
  UniAst,
} from "@xwiki/platform-uniast-api";

type UniAstNode =
  | Block
  | InlineContent
  | ({ type: "listItem" } & ListItem)
  | ({ type: "tableColumn" } & TableColumn)
  | ({ type: "tableCell" } & TableCell);

/**
 * Used to visit UniAst nodes.
 *
 * @beta
 */
interface UniAstVisitor {
  /**
   * Visit a UniAst node.
   *
   * @param node - the node to visit
   * @returns true if the iterator should not visit the children of this node, false otherwise
   */
  visit(node: UniAstNode): boolean;
}

/**
 * Used to iterate UniAst nodes.
 *
 * @beta
 */
interface UniAstIterator {
  /**
   * Iterates the nodes of the given UniAst, calling the provided visitor for each node.
   *
   * @param uniAst - the UniAst to iterate
   * @param visitor - the visitor to call for each node of the UniAst
   */
  iterate(uniAst: UniAst, visitor: UniAstVisitor): void;
}

export { type UniAstIterator, type UniAstNode, type UniAstVisitor };
