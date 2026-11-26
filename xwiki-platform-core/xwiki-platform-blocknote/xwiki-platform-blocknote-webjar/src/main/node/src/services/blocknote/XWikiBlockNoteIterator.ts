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
import { isMacroNode } from "./macroNodes";
import {
  DefaultBlockNoteIterator,
  blockNoteIteratorName,
} from "@xwiki/platform-editors-blocknote-react";
import { injectable } from "inversify";
import type {
  BlockNoteIterator,
  BlockNoteVisitor,
  BlockType,
} from "@xwiki/platform-editors-blocknote-react";
import type { Container } from "inversify";

/**
 * Iterates the BlockNote content produced by the XWiki rendering, entering the macro calls: the parameters and the
 * content of a macro call that are editable in-place are BlockNote fragments, which can hold other macro calls. The
 * nodes of these fragments are visited after the macro node that holds them, which the visitor receives with them. A
 * macro call held as a JSON string (like the top-level macro calls of the edited content) is parsed to be entered, so
 * the changes the visitor makes to its nodes are not kept, unless the visitor replaces the JSON string with the macro
 * call object when visiting the macro node.
 *
 * @since 18.10.0
 * @beta
 */
@injectable("Singleton")
export class XWikiBlockNoteIterator
  extends DefaultBlockNoteIterator
  implements BlockNoteIterator
{
  public static override bind(container: Container): void {
    // Replaces the default iterator, which doesn't enter the macro calls.
    if (container.isBound(blockNoteIteratorName)) {
      container.unbind(blockNoteIteratorName);
    }
    container
      .bind<BlockNoteIterator>(blockNoteIteratorName)
      .to(XWikiBlockNoteIterator)
      .inSingletonScope();
  }

  public override iterate(
    blocks: BlockType[],
    visitor: BlockNoteVisitor,
  ): void {
    this.iterateIn(blocks, visitor);
  }

  /**
   * @param nodes - the nodes to iterate
   * @param visitor - the visitor to call for each node
   * @param macro - the macro node whose call holds the given nodes, if any
   */
  private iterateIn(
    nodes: BlockType[],
    visitor: BlockNoteVisitor,
    macro?: object,
  ): void {
    super.iterate(nodes, {
      visit: (node) => {
        if (visitor.visit(node, macro)) {
          return true;
        }
        if (typeof node === "object" && isMacroNode(node)) {
          this.iterateNestedMacros(node, visitor);
          // The macro nodes don't have children.
          return true;
        }
        return false;
      },
    });
  }

  /**
   * Visits the nodes of the parameters and the content of the given macro call that are editable in-place.
   *
   * @param macroNode - the macro node to enter
   * @param visitor - the visitor to call for each nested node
   */
  private iterateNestedMacros(
    macroNode: object,
    visitor: BlockNoteVisitor,
  ): void {
    const call = this.getMacroCall(macroNode);
    if (call === null || typeof call !== "object") {
      return;
    }
    const { parameters, content } = call as {
      parameters?: Record<string, unknown>;
      content?: unknown;
    };
    [...Object.values(parameters ?? {}), content]
      .filter((value) => value !== null && typeof value === "object")
      .forEach((fragment) => {
        // A fragment can also hold inline content, which the iterator handles as well.
        const nodes = Array.isArray(fragment) ? fragment : [fragment];
        this.iterateIn(nodes as BlockType[], visitor, macroNode);
      });
  }

  /**
   * @param macroNode - a macro node
   * @returns the macro call object of the given macro node, parsed if needed, or undefined if it can't be parsed
   */
  private getMacroCall(macroNode: object): unknown {
    const call = (macroNode as { props?: { call?: unknown } }).props?.call;
    if (typeof call !== "string") {
      return call;
    }
    try {
      return JSON.parse(call);
    } catch {
      return undefined;
    }
  }
}
