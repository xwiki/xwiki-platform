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
import { BlockNoteDocument } from "./BlockNoteProcessor";
import { Container, inject, injectable } from "inversify";
import { uuidv4 } from "lib0/random";
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
  private static readonly DEFAULTS = {
    // Generic styles
    backgroundColor: "default",
    textColor: "default",

    // FIXME: Left text alignment is the default but at the same time the user might have explicitly set it in order to
    // overwrite the inherited alignment. It's not easy to distinguish between the two cases. Given that we discourage
    // the users from using non-semantic text styles in general (they should focus on the content and leave the style to
    // the theme and skin), I think it's acceptable to lose the explicit left alignment for now, instead of polluting
    // the saved source syntax with a lot of unnecessary styles.
    textAlignment: "left",

    // Table cells
    colspan: 1,
    rowspan: 1,

    // Code block
    language: "text",
  };

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

  public load(blockNoteJSON: string): BlockNoteDocument {
    const blockNoteContent = blockNoteJSON ? JSON.parse(blockNoteJSON) : [];
    const blockNoteDocument = new BlockNoteDocument(blockNoteContent);
    this.iterator.iterate(blockNoteContent, {
      visit: (node) => this.loadNode(node, blockNoteDocument),
    });
    return blockNoteDocument;
  }

  private loadNode(
    node: NodeType,
    blockNoteDocument: BlockNoteDocument,
  ): boolean {
    if (typeof node === "object") {
      if (node.type === "xwikiMacroBlock") {
        this.loadMacro(node as BlockType);
        return true;
      } else if (
        "props" in node &&
        node.props !== null &&
        typeof node.props === "object"
      ) {
        this.storeBlockXWikiParameters(node, blockNoteDocument);
      } else if (
        node.type === "text" &&
        "styles" in node &&
        node.styles !== null &&
        typeof node.styles === "object"
      ) {
        this.serializeXWikiParameters(node.styles as Record<string, unknown>);
      }
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

  /**
   * Extracts `xwikiParameters` from a block's props, assigns the block a stable UUID (so BlockNote preserves it),
   * and stores the parameters in the metadata for re-injection during save.
   *
   * Modifying propSchema of standard BlockNote blocks to carry `xwikiParameters` is not possible: the render and
   * toExternalHTML closures inside each block's implementation capture the original propSchema at construction time,
   * so they would receive `xwikiParameters` in block.props but not find it in their captured propSchema, causing a
   * TypeError in wrapInBlockStructure. The ID-injection side-channel avoids that entirely.
   *
   * @param block - the raw block node (mutable)
   * @param blockNoteDocument - the BlockNote document to store the parameters in
   */
  private storeBlockXWikiParameters(
    block: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
  ): void {
    const props = block.props as Record<string, unknown>;
    if (
      props.xwikiParameters !== null &&
      typeof props.xwikiParameters === "object"
    ) {
      block.id = block.id ?? uuidv4();
      blockNoteDocument.getMetadata(block.id as string, true)!.parameters =
        props.xwikiParameters;
      delete props.xwikiParameters;
    }
  }

  /**
   * Converts the `xwikiParameters` value in a text style object from an object to a JSON string so that BlockNote
   * can store it as a primitive style value.
   *
   * @param obj - the text styles object to update in place
   */
  private serializeXWikiParameters(obj: Record<string, unknown>): void {
    if (
      obj.xwikiParameters !== null &&
      typeof obj.xwikiParameters === "object"
    ) {
      obj.xwikiParameters = JSON.stringify(obj.xwikiParameters);
    }
  }

  public save(blockNoteDocument: BlockNoteDocument): string {
    const blockNoteContentCopy = structuredClone(blockNoteDocument.content);
    this.iterator.iterate(blockNoteContentCopy, {
      visit: (node) => this.saveNode(node, blockNoteDocument),
    });
    return JSON.stringify(blockNoteContentCopy);
  }

  private saveNode(
    node: NodeType,
    blockNoteDocument: BlockNoteDocument,
  ): boolean {
    if (typeof node === "object") {
      if (node.type === "xwikiMacroBlock") {
        this.saveMacro(node as BlockType);
        return true;
      } else if (
        "props" in node &&
        node.props !== null &&
        typeof node.props === "object"
      ) {
        this.removeDefaults(node.props);
        this.restoreBlockXWikiParameters(node, blockNoteDocument);
      } else if (
        node.type === "text" &&
        "styles" in node &&
        node.styles !== null &&
        typeof node.styles === "object"
      ) {
        this.removeDefaults(node.styles);
        this.restoreXWikiParameters(node.styles as Record<string, unknown>);
      }
    }
    return false;
  }

  private removeDefaults(options: Record<string, unknown>): void {
    for (const [key, defaultValue] of Object.entries(
      XWikiBlockNoteProcessor.DEFAULTS,
    )) {
      if (options[key] === defaultValue) {
        delete options[key];
      }
    }
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

  /**
   * Re-injects the `xwikiParameters` object that was extracted during load, identified by the block's stable ID.
   *
   * @param block - the raw block node
   * @param blockNoteDocument - the BlockNote document to retrieve the parameters from
   */
  private restoreBlockXWikiParameters(
    block: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
  ): void {
    if (typeof block.id === "string") {
      const metadata = blockNoteDocument.getMetadata(block.id);
      if (metadata?.parameters) {
        const props = block.props as Record<string, unknown>;
        props.xwikiParameters = metadata.parameters;
      }
    }
  }

  /**
   * Reverses the serialization done by {@link serializeXWikiParameters}: converts the `xwikiParameters` JSON string
   * back to an object, or removes the key entirely when it is empty (so the Java backend does not receive an empty
   * entry).
   *
   * @param obj - the text styles object to update in place
   */
  private restoreXWikiParameters(obj: Record<string, unknown>): void {
    if (typeof obj.xwikiParameters === "string") {
      if (obj.xwikiParameters === "") {
        delete obj.xwikiParameters;
      } else {
        obj.xwikiParameters = JSON.parse(obj.xwikiParameters);
      }
    }
  }
}
