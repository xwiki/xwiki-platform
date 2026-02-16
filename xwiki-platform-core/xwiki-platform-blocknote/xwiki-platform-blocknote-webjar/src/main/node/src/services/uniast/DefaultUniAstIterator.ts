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
import { UniAstIterator, UniAstNode, UniAstVisitor } from "./UniAstIterator";
import { Container, injectable } from "inversify";
import type { Block, UniAst } from "@xwiki/platform-uniast-api";

type List = Extract<Block, { type: "list" }>;
type Table = Extract<Block, { type: "table" }>;

/**
 * Default implementation of a UniAstIterator.
 *
 * @beta
 */
@injectable("Singleton")
export class DefaultUniAstIterator implements UniAstIterator {
  public static bind(container: Container): void {
    container
      .bind("UniAstIterator")
      .to(DefaultUniAstIterator)
      .inSingletonScope();
  }

  public iterate(uniAst: UniAst, visitor: UniAstVisitor): void {
    this.visitChildren(uniAst.blocks, visitor);
  }

  private visitChildren(children: UniAstNode[], visitor: UniAstVisitor): void {
    for (const child of children) {
      this.visitNode(child, visitor);
    }
  }

  private visitNode(node: UniAstNode, visitor: UniAstVisitor): void {
    if (visitor.visit(node)) {
      return;
    }

    switch (node.type) {
      case "paragraph":
      case "heading":
      case "quote":
      case "listItem":
      case "tableCell":
      case "link":
        this.visitChildren(node.content, visitor);
        break;

      case "list":
        this.visitList(node, visitor);
        break;

      case "table":
        this.visitTable(node, visitor);
        break;
    }
  }

  private visitList(list: List, visitor: UniAstVisitor): void {
    for (const listItem of list.items) {
      this.visitNode({ type: "listItem", ...listItem }, visitor);
    }
  }

  private visitTable(table: Table, visitor: UniAstVisitor): void {
    for (const column of table.columns) {
      this.visitNode({ type: "tableColumn", ...column }, visitor);
      if (column.headerCell) {
        this.visitNode({ type: "tableCell", ...column.headerCell }, visitor);
      }
    }
    for (const row of table.rows) {
      for (const cell of row) {
        this.visitNode({ type: "tableCell", ...cell }, visitor);
      }
    }
  }
}
