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
    if (typeof node === "object" && node.type === "xwikiMacroBlock") {
      this.loadMacro(node as BlockType);
      return true;
    }
    return false;
  }

  /**
   * By default, BlockNote supports only primitive values for block properties. To overcome this limitation, we need to
   * pre-process the macro block properties to convert the call and output objects into strings (JSON serialization).
   *
   * @param macroNode - the macro node to pre-process
   */
  private loadMacro(macroNode: BlockType): void {
    const props = macroNode.props as Record<string, unknown>;
    props.call = JSON.stringify(props.call);
    if (props.output) {
      props.output = JSON.stringify(props.output);
    }
  }

  public save(blockNoteContent: BlockType[]): string {
    const blockNoteContentCopy = structuredClone(blockNoteContent);
    this.iterator.iterate(blockNoteContentCopy, {
      visit: (node) => this.saveNode(node),
    });
    return JSON.stringify(blockNoteContentCopy);
  }

  private saveNode(node: NodeType): boolean {
    if (typeof node === "object" && node.type === "xwikiMacroBlock") {
      this.saveMacro(node as BlockType);
      return true;
    }
    return false;
  }

  /**
   * Reverses the changes made by the loadMacro method.
   *
   * @param macroNode - the macro node to post-process
   */
  private saveMacro(macroNode: BlockType): void {
    const props = macroNode.props as Record<string, unknown>;
    if (typeof props.call === "string") {
      props.call = JSON.parse(props.call);
    }
    if (typeof props.output === "string") {
      props.output = JSON.parse(props.output);
    }
  }
}
