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
import { Container, inject, injectable } from "inversify";
import type { BlockNoteIterator, NodeType } from "./BlockNoteIterator";
import type { BlockNoteProcessor } from "./BlockNoteProcessor";
import type { BlockType } from "@xwiki/platform-editors-blocknote-react";

/**
 * XWik specific implementation of a BlockNoteProcessor.
 *
 * @beta
 */
@injectable("Singleton")
export class XWikiBlockNoteProcessor implements BlockNoteProcessor {
  public static bind(container: Container): void {
    container
      .bind("BlockNoteProcessor")
      .to(XWikiBlockNoteProcessor)
      .inSingletonScope()
      .whenNamed("XWiki");
  }

  constructor(
    @inject("BlockNoteIterator") private readonly iterator: BlockNoteIterator,
  ) {}

  public load(blockNoteJSON: string): BlockType[] {
    const blockNoteContent = blockNoteJSON ? JSON.parse(blockNoteJSON) : [];
    this.iterator.iterate(blockNoteContent, {
      visit: (node) => this.loadNode(node),
    });
    return blockNoteContent;
  }

  private loadNode(node: NodeType): boolean {
    console.log("Loading node:", node);
    return false;
  }

  public save(blockNoteContent: BlockType[]): string {
    this.iterator.iterate(blockNoteContent, {
      visit: (node) => this.saveNode(node),
    });
    return JSON.stringify(blockNoteContent);
  }

  private saveNode(node: NodeType): boolean {
    console.log("Saving node:", node);
    return false;
  }
}
