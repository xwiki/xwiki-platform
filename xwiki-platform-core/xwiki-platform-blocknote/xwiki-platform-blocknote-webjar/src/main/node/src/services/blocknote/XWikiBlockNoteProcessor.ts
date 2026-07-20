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
import { extractLinkId, injectLinkId, stripLinkId } from "./linkId";
import { Container, inject, injectable } from "inversify";
import { uuidv4 } from "lib0/random";
import type { BlockNoteIterator, NodeType } from "./BlockNoteIterator";
import type { BlockNoteProcessor } from "./BlockNoteProcessor";

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

  // The block metadata that can't be stored in the BlockNote schema and is backed up (mapped to the block id) so that
  // it survives the editing round-trip.
  private static readonly KNOWN_METADATA: readonly string[] = [
    "xwikiParameters",
    "xwikiReference",
    "xwikiFreestanding",
    "xwikiGeneratedLabel",
  ];

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
        this.loadMacro(node as { props?: unknown });
        return true;
      } else if (node.type === "link") {
        this.normalizeLinkContent(node);
        this.backupLinkMetadata(node, blockNoteDocument);
      } else if (
        "props" in node &&
        node.props !== null &&
        typeof node.props === "object"
      ) {
        this.backupBlockMetadata(node, blockNoteDocument);
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
   * By default, BlockNote supports only primitive values for block and inline content properties. To overcome this
   * limitation, we need to pre-process the macro properties (both block and inline macros) to convert the call and
   * output objects into strings (JSON serialization).
   *
   * @param macroNode - the macro node to pre-process
   */
  private loadMacro(macroNode: { props?: unknown }): void {
    const props = macroNode.props as Record<string, unknown>;
    props.call = JSON.stringify(props.call);
    if (props.output) {
      props.output = JSON.stringify(props.output);
    }
  }

  /**
   * Backup known block metada (like `xwikiParameters`) from a block's props so that it doesn't get lost during edit.
   * The metadata is mapped to the block id, which is generated if missing (BlockNote preserves the generated id). The
   * metadata is restored on save.
   *
   * Modifying propSchema of standard BlockNote blocks to carry the metadata is not possible: the render and
   * toExternalHTML closures inside each block's implementation capture the original propSchema at construction time,
   * so they would receive the metadata in block.props but not find it in their captured propSchema, causing a
   * TypeError in wrapInBlockStructure. The ID-injection side-channel avoids that entirely.
   *
   * @param block - the raw block node (mutable)
   * @param blockNoteDocument - the BlockNote document where to backup the metadata
   */
  private backupBlockMetadata(
    block: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
  ): void {
    const props = block.props as Record<string, unknown>;
    XWikiBlockNoteProcessor.KNOWN_METADATA.filter(
      (key) => key in props,
    ).forEach((key) => {
      block.id = block.id ?? uuidv4();
      blockNoteDocument.getMetadata(block.id as string, true)![key] =
        props[key];
      delete props[key];
    });
  }

  /**
   * Backup the metadata of a link (resource reference, parameters, freestanding flag) so that it
   * survives the editing round-trip. Unlike a block, a link is inline content and has no id that
   * BlockNote preserves, so the metadata is mapped to a synthetic id that is stored in the link href
   * (the only part of a link that BlockNote preserves verbatim). The BlockNote link schema has no
   * props, so the props (holding the metadata) are removed after being backed up.
   *
   * @param link - the raw link node (mutable)
   * @param blockNoteDocument - the BlockNote document where to backup the metadata
   */
  private backupLinkMetadata(
    link: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
  ): void {
    const props = (link.props ?? {}) as Record<string, unknown>;
    const metadata: Record<string, unknown> = {};
    XWikiBlockNoteProcessor.KNOWN_METADATA.filter(
      (key) => key in props,
    ).forEach((key) => {
      metadata[key] = props[key];
    });
    if (Object.keys(metadata).length > 0) {
      const id = uuidv4();
      Object.assign(blockNoteDocument.getMetadata(id, true)!, metadata);
      link.href = injectLinkId(link.href as string, id);
    }
    // The BlockNote link schema doesn't support props so we drop them (the metadata they held is
    // backed up above and restored on save).
    delete link.props;

    this.showGeneratedLabel(link, metadata.xwikiGeneratedLabel);
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
   * no longer generated and the marker is dropped on save (see {@link restoreLinkMetadata}).
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
      visit: (node) => this.saveNode(node, blockNoteDocument),
    });
    return JSON.stringify(blockNoteContentCopy);
  }

  private saveNode(
    node: NodeType,
    blockNoteDocument: BlockNoteDocument,
  ): boolean {
    if (typeof node === "object") {
      if (this.isMacroNode(node)) {
        this.saveMacro(node as { props?: unknown });
        return true;
      } else if (node.type === "link") {
        this.restoreLinkMetadata(node, blockNoteDocument);
      } else if (
        "props" in node &&
        node.props !== null &&
        typeof node.props === "object"
      ) {
        this.removeDefaults(node.props);
        this.restoreBlockMetadata(node, blockNoteDocument);
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
  private saveMacro(macroNode: { props?: unknown }): void {
    const props = macroNode.props as Record<string, unknown>;
    if (typeof props.call === "string") {
      props.call = JSON.parse(props.call);
    }
    if (typeof props.output === "string") {
      props.output = JSON.parse(props.output);
    }
  }

  /**
   * Re-injects the block metadata (like `xwikiParameters`) that was extracted during load and mapped to the block ID.
   *
   * @param block - the raw block node
   * @param blockNoteDocument - the BlockNote document to retrieve the metadata from
   */
  private restoreBlockMetadata(
    block: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
  ): void {
    if (typeof block.id === "string") {
      const metadata = blockNoteDocument.getMetadata(block.id);
      const props = block.props as Record<string, unknown>;
      Object.entries(metadata ?? {}).forEach(([key, value]) => {
        props[key] = value;
      });
    }
  }

  /**
   * Reverses {@link backupLinkMetadata}: reads the synthetic id from the link href, restores the
   * backed up metadata into the link props (recreating them, since the BlockNote link schema has no
   * props) and removes the synthetic id from the href.
   *
   * @param link - the raw link node (mutable)
   * @param blockNoteDocument - the BlockNote document to retrieve the metadata from
   */
  private restoreLinkMetadata(
    link: Record<string, unknown>,
    blockNoteDocument: BlockNoteDocument,
  ): void {
    const id =
      typeof link.href === "string" ? extractLinkId(link.href) : undefined;
    if (id) {
      link.href = stripLinkId(link.href as string);
      const metadata = blockNoteDocument.getMetadata(id);
      if (metadata) {
        const props = { ...metadata };
        // Drop the generated label marker if the user changed the (originally generated) label, so that the modified
        // label is preserved on save instead of being discarded as a generated label.
        if (props.xwikiGeneratedLabel !== this.getLinkText(link)) {
          delete props.xwikiGeneratedLabel;
        }
        link.props = props;
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
