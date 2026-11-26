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
import { propsToAttributes } from "@blocknote/core";
import type { BlockType, EditorType } from "../blocknote";
import type { BlockMetadata, BlockMetadataKey } from "./blockMetadata";
import type { BlockSpecs } from "@blocknote/core";

// The part of a Tiptap node used to add an attribute to it (the type of the Tiptap nodes isn't exported by BlockNote).
type TiptapNode = {
  extend(config: {
    addAttributes(this: { parent?: () => object }): object;
  }): TiptapNode;
};

// The metadata of the edited content that must follow the content (be shared with the other users editing in realtime,
// be restored by undo / redo, be copied with the content) is kept in the content itself:
// - in the "metadata" property that the editor adds to all the block types, as a JSON object
// - in the URL of the links, as a query parameter holding the JSON object encoded in base64url, because the links are
//   inline content, which BlockNote doesn't let us extend with properties
// Each editing feature reads, changes and writes back its own entries of the JSON object, keeping the others (even the
// ones it doesn't know about), so that the editing features don't need to extend the schema, and all the users editing
// in realtime have the same schema.

/**
 * The name of the block property holding the metadata of the block, as a JSON object.
 *
 * @since 18.10.0
 * @beta
 */
const METADATA_PROP = "metadata";

/**
 * The name of the query parameter holding the metadata of a link, in its URL.
 *
 * @since 18.10.0
 * @beta
 */
const LINK_METADATA_PARAM = "__xwikiMetadata";

/**
 * Parses the metadata kept in the edited content.
 *
 * @param value - the JSON serialization of some metadata
 * @returns the metadata, or an empty object when the given value is not a JSON object
 *
 * @since 18.10.0
 * @beta
 */
function parseMetadata(value: unknown): BlockMetadata {
  if (typeof value === "string" && value) {
    try {
      const metadata = JSON.parse(value);
      if (
        metadata &&
        typeof metadata === "object" &&
        !Array.isArray(metadata)
      ) {
        return metadata;
      }
    } catch {
      // Not metadata.
    }
  }
  return {};
}

/**
 * Serializes some metadata, to keep it in the edited content.
 *
 * @param metadata - some metadata
 * @returns the JSON serialization of the given metadata, or the empty string when it has no entries (which is the
 *   default value of the metadata property, so that the blocks without metadata don't hold an empty object)
 *
 * @since 18.10.0
 * @beta
 */
function stringifyMetadata(metadata: BlockMetadata): string {
  const entries = Object.entries(metadata).filter(
    ([, value]) => value !== undefined,
  );
  return entries.length ? JSON.stringify(Object.fromEntries(entries)) : "";
}

/**
 * Gets the metadata kept in a block.
 *
 * @param block - a block of the edited content
 * @returns the metadata kept in the given block, or an empty object if it has none; changing it doesn't change the
 *   block, use {@link setBlockMetadata} instead
 *
 * @since 18.10.0
 * @beta
 */
function getBlockMetadata(block: Pick<BlockType, "props">): BlockMetadata {
  return parseMetadata((block.props as Record<string, unknown>)[METADATA_PROP]);
}

/**
 * Sets an entry of the metadata kept in a block. It is a change of the edited content: it is shared with the other users
 * editing in realtime, and it is reverted by undo. Call it in the same transaction as the related content change (see
 * `editor.transact`), so that both are undone together.
 *
 * @param editor - the editor holding the block
 * @param block - the block, or its id
 * @param key - identifies the metadata entry
 * @param value - the value of the metadata entry, which must be serializable to JSON, or undefined to remove the entry
 *
 * @since 18.10.0
 * @beta
 */
function setBlockMetadata(
  editor: EditorType,
  block: BlockType | string,
  key: BlockMetadataKey,
  value: unknown,
): void {
  // Read the block from the editor, so that the entries set since the given block was read aren't lost.
  const current = editor.getBlock(typeof block === "string" ? block : block.id);
  if (!current) {
    return;
  }
  const props = current.props as Record<string, unknown>;
  const metadata = {
    ...parseMetadata(props[METADATA_PROP]),
    [key.name]: value,
  };
  const serialized = stringifyMetadata(metadata);
  if (serialized !== (props[METADATA_PROP] ?? "")) {
    editor.updateBlock(current, {
      props: { [METADATA_PROP]: serialized },
    } as Parameters<EditorType["updateBlock"]>[1]);
  }
}

/**
 * Adds the metadata property to all the given block types, so that any block can hold metadata. The property is added
 * by the editor, rather than by the editing features using it, because all the users editing the same content in
 * realtime must have the same schema: when a block is changed, the properties that the schema of the user changing it
 * doesn't have are removed.
 *
 * @param blockSpecs - the block types of the schema
 * @returns the given block types, with the metadata property
 */
function addMetadataProp<T extends BlockSpecs>(blockSpecs: T): T {
  const metadataPropSchema = {
    [METADATA_PROP]: { type: "string" as const, default: "" },
  };
  return Object.fromEntries(
    Object.entries(blockSpecs).map(([type, spec]) => {
      const implementation =
        spec.implementation as typeof spec.implementation & {
          node?: TiptapNode;
        };
      return [
        type,
        {
          ...spec,
          config: {
            ...spec.config,
            propSchema: { ...spec.config.propSchema, ...metadataPropSchema },
          },
          // The block types implemented with their own ProseMirror node (e.g. the table) don't get the attributes of
          // their properties from the property schema, so the metadata attribute is added to their node.
          implementation: implementation.node
            ? {
                ...implementation,
                node: implementation.node.extend({
                  addAttributes() {
                    return {
                      ...this.parent?.(),
                      ...propsToAttributes(metadataPropSchema),
                    };
                  },
                }),
              }
            : implementation,
        },
      ];
    }),
  ) as unknown as T;
}

// Sentinel base used to parse relative URLs with the URL constructor, as if they were found in a document at this URL.
// It's stripped back off before the result is returned, so that a relative URL stays relative to the same location:
// the document (e.g. "#section", "?key=value"), its directory (e.g. "page") or the server root (e.g. "/path").
const RELATIVE_BASE = "http://relative.base/__directory__/__document__";

function parseURL(href: string): { url: URL; relative: boolean } | undefined {
  try {
    return { url: new URL(href), relative: false };
  } catch {
    try {
      return { url: new URL(href, RELATIVE_BASE), relative: true };
    } catch {
      return undefined;
    }
  }
}

function serializeURL(url: URL, relative: boolean): string {
  if (!relative) {
    return url.toString();
  }
  const base = new URL(RELATIVE_BASE);
  const suffix = url.search + url.hash;
  if (url.pathname === base.pathname) {
    // Relative to the document itself, e.g. "#section" or "?key=value".
    return suffix;
  }
  const directory = base.pathname.substring(
    0,
    base.pathname.lastIndexOf("/") + 1,
  );
  if (url.pathname.startsWith(directory)) {
    // Relative to the directory of the document, e.g. "page".
    return url.pathname.substring(directory.length) + suffix;
  }
  // Relative to the server root, e.g. "/path".
  return url.pathname + suffix;
}

function encodeBase64URL(text: string): string {
  let binary = "";
  new TextEncoder()
    .encode(text)
    .forEach((byte) => (binary += String.fromCharCode(byte)));
  return btoa(binary)
    .replace(/\+/g, "-")
    .replace(/\//g, "_")
    .replace(/=+$/, "");
}

function decodeBase64URL(encoded: string): string {
  const binary = atob(encoded.replace(/-/g, "+").replace(/_/g, "/"));
  return new TextDecoder().decode(
    Uint8Array.from(binary, (char) => char.charCodeAt(0)),
  );
}

/**
 * Gets the metadata kept in the URL of a link.
 *
 * @param href - the URL of a link
 * @returns the metadata kept in the given link URL, or an empty object if it has none
 *
 * @since 18.10.0
 * @beta
 */
function getLinkMetadata(href: string): BlockMetadata {
  const encoded = parseURL(href)?.url.searchParams.get(LINK_METADATA_PARAM);
  if (encoded) {
    try {
      return parseMetadata(decodeBase64URL(encoded));
    } catch {
      // Not metadata.
    }
  }
  return {};
}

/**
 * Sets the metadata kept in the URL of a link.
 *
 * @param href - the URL of a link
 * @param metadata - the metadata to keep in the link URL, replacing the metadata it holds
 * @returns the given link URL, holding the given metadata; the URL is left untouched when it has no metadata to hold or
 *   remove, because handling the metadata normalizes the URL (e.g. adding a trailing slash)
 *
 * @since 18.10.0
 * @beta
 */
function setLinkMetadata(href: string, metadata: BlockMetadata): string {
  const serialized = stringifyMetadata(metadata);
  const parsed = parseURL(href);
  if (
    !parsed ||
    (!serialized && !parsed.url.searchParams.has(LINK_METADATA_PARAM))
  ) {
    return href;
  }
  if (serialized) {
    parsed.url.searchParams.set(
      LINK_METADATA_PARAM,
      encodeBase64URL(serialized),
    );
  } else {
    parsed.url.searchParams.delete(LINK_METADATA_PARAM);
  }
  return serializeURL(parsed.url, parsed.relative);
}

/**
 * Removes the metadata kept in the URL of a link.
 *
 * @param href - the URL of a link
 * @returns the given link URL without the metadata it holds, i.e. the URL that the link points to
 *
 * @since 18.10.0
 * @beta
 */
function stripLinkMetadata(href: string): string {
  return setLinkMetadata(href, {});
}

export {
  LINK_METADATA_PARAM,
  METADATA_PROP,
  addMetadataProp,
  getBlockMetadata,
  getLinkMetadata,
  parseMetadata,
  setBlockMetadata,
  setLinkMetadata,
  stringifyMetadata,
  stripLinkMetadata,
};
