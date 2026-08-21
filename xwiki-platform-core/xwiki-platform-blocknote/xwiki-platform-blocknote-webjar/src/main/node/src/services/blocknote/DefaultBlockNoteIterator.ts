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
import { Container, injectable } from "inversify";
import type {
  BlockNoteIterator,
  BlockNoteVisitor,
  NodeType,
} from "./BlockNoteIterator";
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

type Table = Extract<BlockType, { type: "table" }>;

/**
 * Default implementation of a BlockNoteIterator.
 *
 * @beta
 */
@injectable("Singleton")
export class DefaultBlockNoteIterator implements BlockNoteIterator {
  public static bind(container: Container): void {
    container
      .bind("BlockNoteIterator")
      .to(DefaultBlockNoteIterator)
      .inSingletonScope();
  }

  public iterate(blocks: BlockType[], visitor: BlockNoteVisitor): void {
    this.visitChildren(blocks, visitor);
  }

  private visitChildren(children: NodeType[], visitor: BlockNoteVisitor): void {
    for (const child of children) {
      this.visitNode(child, visitor);
    }
  }

  private visitNode(node: NodeType, visitor: BlockNoteVisitor): void {
    if (visitor.visit(node) || typeof node === "string") {
      return;
    }

    if (node.type === "table") {
      this.visitTableCells(node, visitor);
    } else if ("content" in node && node.content) {
      const content = Array.isArray(node.content)
        ? node.content
        : [node.content];
      this.visitChildren(content, visitor);
    }

    if ("children" in node && node.children) {
      const children = Array.isArray(node.children)
        ? node.children
        : [node.children];
      this.visitChildren(children, visitor);
    }
  }

  private visitTableCells(table: Table, visitor: BlockNoteVisitor): void {
    table.content.rows.forEach((row) => {
      row.cells.forEach((cell) => this.visitNode(cell as NodeType, visitor));
    });
  }
}
