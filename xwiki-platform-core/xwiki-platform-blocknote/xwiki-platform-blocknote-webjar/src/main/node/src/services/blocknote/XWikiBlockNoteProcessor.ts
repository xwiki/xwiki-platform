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
import {
  METADATA_PROP,
  XWIKI_GENERATED_LABEL,
  XWIKI_METADATA_KEYS,
  getLinkMetadata,
  parseMetadata,
  setLinkMetadata,
  setMacroOutput,
  stringifyMetadata,
  stripLinkMetadata,
} from "@xwiki/platform-editors-blocknote-react";
import { Container, inject, injectable } from "inversify";
import { uuidv4 } from "lib0/random";
import type { BlockNoteIterator, NodeType } from "./BlockNoteIterator";
import type { BlockNoteProcessor } from "./BlockNoteProcessor";
import type {
  BlockMetadata,
  BlockType,
} from "@xwiki/platform-editors-blocknote-react";

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

    // Image
    caption: "",

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
      if (this.isMacroNode(node)) {
        this.loadMacro(node as Record<string, unknown>, blockNoteDocument);
        return true;
      } else if (node.type === "link") {
        this.normalizeLinkContent(node);
        this.loadLinkMetadata(node);
      } else if (
        "props" in node &&
        node.props !== null &&
        typeof node.props === "object"
      ) {
        this.loadBlockMetadata(node);
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
   * @param node - the node to test
   * @returns `true` if the given node is a macro node (block or inline macro). The inline macro type is compared as a
   *   string because it is not part of the (statically-typed) inline content schema: it is registered through an index
   *   signature on the editor side so that it doesn't tighten the schema, so its literal type is not available here.
   */
  private isMacroNode(node: object): boolean {
    const type = (node as { type?: unknown }).type;
    return type === "xwikiMacroBlock" || type === "xwikiInlineMacro";
  }

  /**
   * Pre-processes a macro node (block or inline macro):
   * <ul>
   * <li>By default, BlockNote supports only primitive values for block and inline content properties, so the macro call
   * object is converted into a string (JSON serialization).</li>
   * <li>The macro output is moved to the document metadata. The macro output can contain information that only the
   * current user is allowed to view (macros are rendered server-side with the rights of the current user) so it must
   * not be part of the edited content, which is shared with the other users when editing in realtime. The same goes
   * for the output of the macros nested in the macro parameters or content (i.e. the parameters or the content that
   * are editable in-place).</li>
   * </ul>
   *
   * @param macroNode - the macro node to pre-process
   * @param blockNoteDocument - the BlockNote document where to backup the macro output
   * @param nested - whether the macro is nested in the parameters or content of another macro call
   */
  private loadMacro(
    macroNode: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
    nested = false,
  ): void {
    const props = macroNode.props as Record<string, unknown>;
    this.visitNestedMacros(props.call, (nestedMacro) =>
      this.loadMacro(nestedMacro, blockNoteDocument, true),
    );
    const call = JSON.stringify(props.call);
    if (!nested) {
      props.call = call;
    }

    const output = props.output;
    delete props.output;
    if (Array.isArray(output) && output.length) {
      setMacroOutput(
        blockNoteDocument.metadata,
        this.getMacroId(macroNode, nested),
        call,
        output,
      );
    }
  }

  /**
   * @param macroNode - a macro node
   * @param nested - whether the macro is nested in the parameters or content of another macro call
   * @returns the id used to associate the macro output with the given macro node, generated if needed
   */
  private getMacroId(
    macroNode: Record<string, unknown>,
    nested: boolean,
  ): string {
    if (!nested && macroNode.type === "xwikiMacroBlock") {
      // BlockNote preserves the block id.
      macroNode.id = macroNode.id ?? uuidv4();
      return macroNode.id as string;
    }
    // Inline content and nested macros don't have an id that BlockNote preserves so we use a dedicated property.
    const props = macroNode.props as Record<string, unknown>;
    props.id = uuidv4();
    return props.id as string;
  }

  /**
   * Calls the given function for each macro node found in the parameters or the content of the given macro call (the
   * parameters and the content that are editable in-place are BlockNote fragments that can contain macro calls). The
   * macros nested in the found macros are not visited.
   *
   * @param call - the macro call object
   * @param visit - the function to call for each nested macro node
   */
  private visitNestedMacros(
    call: unknown,
    visit: (macroNode: Record<string, unknown>) => void,
  ): void {
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
        const nodes = Array.isArray(fragment) ? fragment : [fragment];
        // A fragment can also hold inline content, which the iterator handles as well.
        this.iterator.iterate(nodes as BlockType[], {
          visit: (node) => {
            if (typeof node === "object" && this.isMacroNode(node)) {
              visit(node as Record<string, unknown>);
              return true;
            }
            return false;
          },
        });
      });
  }

  /**
   * Moves the XWiki metadata (e.g. the parameters, the resource reference) out of the given properties, which the
   * BlockNote schema doesn't know about.
   *
   * @param props - the properties of a node of the loaded content (mutable)
   * @returns the XWiki metadata found in the given properties
   */
  private extractMetadata(props: Record<string, unknown>): BlockMetadata {
    const metadata: BlockMetadata = {};
    XWIKI_METADATA_KEYS.filter((key) => key.name in props).forEach((key) => {
      metadata[key.name] = props[key.name];
      delete props[key.name];
    });
    return metadata;
  }

  /**
   * Moves the XWiki metadata of a block (e.g. its parameters) into the metadata property of the block, where the editor
   * keeps it, as part of the edited content (so that it is shared with the other users editing in realtime, and
   * restored by undo / redo). The metadata is put back in the block properties on save.
   *
   * @param block - the raw block node (mutable)
   */
  private loadBlockMetadata(block: Record<string, unknown>): void {
    const metadata = this.extractMetadata(
      block.props as Record<string, unknown>,
    );
    // The properties of the table cells are fixed by BlockNote, so they can't hold metadata: their metadata is lost.
    if (block.type !== "tableCell" && Object.keys(metadata).length) {
      (block.props as Record<string, unknown>)[METADATA_PROP] =
        stringifyMetadata(metadata);
    }
  }

  /**
   * Moves the XWiki metadata of a link (e.g. its resource reference, its parameters) into its URL, where the editor
   * keeps it (the links are inline content, which BlockNote doesn't let us extend with properties), as part of the
   * edited content. The metadata is put back in the link properties on save.
   *
   * @param link - the raw link node (mutable)
   */
  private loadLinkMetadata(link: Record<string, unknown>): void {
    const metadata = this.extractMetadata(
      (link.props ?? {}) as Record<string, unknown>,
    );
    link.href = setLinkMetadata(link.href as string, metadata);
    // The BlockNote link schema doesn't support properties.
    delete link.props;

    this.showGeneratedLabel(link, metadata[XWIKI_GENERATED_LABEL.name]);
  }

  /**
   * BlockNote's link inline content, unlike block or table-cell content, doesn't accept bare strings inside its
   * content array: each array element must be a styled-text object (`type: "text"` with `text` and `styles`). A bare
   * string element makes BlockNote throw when it builds the ProseMirror node (it reads `.text` on the string). Wrap
   * any bare string element into a styled-text object so such link content can still be loaded. A whole-string content
   * is left untouched, since BlockNote accepts it (the link content type is `string | StyledText[]`).
   *
   * @param link - the raw link node (mutable)
   */
  private normalizeLinkContent(link: Record<string, unknown>): void {
    if (Array.isArray(link.content)) {
      link.content = link.content.map((child) =>
        typeof child === "string"
          ? { type: "text", text: child, styles: {} }
          : child,
      );
    }
  }

  /**
   * A link without a label is rendered with an empty content and its generated label stored as metadata. Show the
   * generated label as the link content so that the link is visible and editable. If the user changes it, the label is
   * no longer generated and the marker is dropped on save (see {@link saveLinkMetadata}).
   *
   * @param link - the raw link node (mutable)
   * @param generatedLabel - the generated label backed up from the link metadata, if any
   */
  private showGeneratedLabel(
    link: Record<string, unknown>,
    generatedLabel: unknown,
  ): void {
    if (typeof generatedLabel === "string" && this.getLinkText(link) === "") {
      link.content = [{ type: "text", text: generatedLabel, styles: {} }];
    }
  }

  /**
   * @param link - the raw link node
   * @returns the plain text of the link content (the link label); returns undefined if the link has styled content
   */
  private getLinkText(link: Record<string, unknown>): string | undefined {
    let content = link.content;
    if (!Array.isArray(content)) {
      content = [content];
    }
    let text = "";
    for (const child of content as (
      | string
      | { type: "text"; text: string; styles?: Record<string, unknown> }
    )[]) {
      if (typeof child === "string") {
        text += child;
      } else if (
        child?.type === "text" &&
        (!child.styles || Object.keys(child.styles).length === 0)
      ) {
        text += child.text;
      } else {
        return;
      }
    }
    return text;
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
      visit: (node) => this.saveNode(node),
    });
    return JSON.stringify(blockNoteContentCopy);
  }

  private saveNode(node: NodeType): boolean {
    if (typeof node === "object") {
      if (this.isMacroNode(node)) {
        this.saveMacro(node as { props?: unknown });
        return true;
      } else if (node.type === "link") {
        this.saveLinkMetadata(node);
      } else if (
        "props" in node &&
        node.props !== null &&
        typeof node.props === "object"
      ) {
        this.removeDefaults(node.props);
        this.saveBlockMetadata(node.props);
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
   * Reverses the changes made by the loadMacro method. The macro output is not restored because it isn't needed
   * server-side (it is ignored when parsing the BlockNote content).
   *
   * @param macroNode - the macro node to post-process
   */
  private saveMacro(macroNode: { props?: unknown }): void {
    const props = macroNode.props as Record<string, unknown>;
    if (typeof props.call === "string") {
      props.call = JSON.parse(props.call);
    }
    // The id used to look up the macro output.
    delete props.id;
    // The macro calls don't have XWiki metadata.
    delete props[METADATA_PROP];
    this.visitNestedMacros(props.call, (nestedMacro) =>
      this.saveMacro(nestedMacro),
    );
  }

  /**
   * Reverses {@link loadBlockMetadata}: moves the metadata kept in the metadata property of a block back to the block
   * properties.
   *
   * @param props - the properties of the raw block node (mutable)
   */
  private saveBlockMetadata(props: Record<string, unknown>): void {
    Object.assign(props, parseMetadata(props[METADATA_PROP]));
    delete props[METADATA_PROP];
  }

  /**
   * Reverses {@link loadLinkMetadata}: moves the metadata kept in the link URL back to the link properties.
   *
   * @param link - the raw link node (mutable)
   */
  private saveLinkMetadata(link: Record<string, unknown>): void {
    if (typeof link.href !== "string") {
      return;
    }
    const metadata = getLinkMetadata(link.href);
    if (Object.keys(metadata).length) {
      link.href = stripLinkMetadata(link.href);
      // Drop the generated label marker if the user changed the (originally generated) label, so that the modified
      // label is preserved on save instead of being discarded as a generated label.
      if (metadata[XWIKI_GENERATED_LABEL.name] !== this.getLinkText(link)) {
        delete metadata[XWIKI_GENERATED_LABEL.name];
      }
      link.props = metadata;
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
