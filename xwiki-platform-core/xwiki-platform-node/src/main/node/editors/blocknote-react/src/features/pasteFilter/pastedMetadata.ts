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
import { blockMetadataKeyName } from "../../misc/blockMetadata";
import {
  METADATA_PROP,
  getLinkMetadata,
  parseMetadata,
  setLinkMetadata,
  stringifyMetadata,
  stripLinkMetadata,
} from "../../misc/contentMetadata";
import { createExtension } from "@blocknote/core";
import { Fragment, Slice } from "prosemirror-model";
import { Plugin, PluginKey } from "prosemirror-state";
import type {
  BlockMetadata,
  BlockMetadataKey,
  PastedMetadataContext,
} from "../../misc/blockMetadata";
import type { ExtensionOptions } from "@blocknote/core";
import type { Container } from "inversify";
import type { Mark, Node } from "prosemirror-model";

type MetadataKeys = Map<string, BlockMetadataKey>;

/**
 * @param serviceContainer - the service container of the editor instance
 * @returns the metadata keys bound in the given service container, by name: the ones bound for the editor instance,
 *   then the ones bound for all the editor instances (the first key bound with a given name is used)
 */
function getMetadataKeys(serviceContainer: Container): MetadataKeys {
  const keys: MetadataKeys = new Map();
  if (serviceContainer.isBound(blockMetadataKeyName)) {
    for (const key of serviceContainer.getAll<BlockMetadataKey>(
      blockMetadataKeyName,
      { chained: true },
    )) {
      if (!keys.has(key.name)) {
        keys.set(key.name, key);
      }
    }
  }
  return keys;
}

/**
 * Keeps only the entries of the given pasted metadata that their key accepts.
 */
function filterMetadata(
  metadata: BlockMetadata,
  keys: MetadataKeys,
  context: PastedMetadataContext,
): BlockMetadata {
  const filtered: BlockMetadata = {};
  for (const [name, value] of Object.entries(metadata)) {
    const kept = keys.get(name)?.filterPasted?.(value, context);
    if (kept !== undefined) {
      filtered[name] = kept;
    }
  }
  return filtered;
}

/**
 * A text style named after a metadata key holds a value of that metadata entry (e.g. the parameters of a text).
 */
function filterStyle(mark: Mark, key: BlockMetadataKey): Mark | undefined {
  // BlockNote keeps the value of a text style in the "stringValue" attribute of its mark (boolean styles have none).
  const value = mark.attrs.stringValue ?? true;
  const kept = key.filterPasted?.(value, {});
  if (kept === undefined) {
    return undefined;
  }
  return kept === value
    ? mark
    : mark.type.create({ ...mark.attrs, stringValue: kept });
}

/**
 * The metadata of a link is kept in its URL.
 */
function filterLink(link: Mark, href: string, keys: MetadataKeys): Mark {
  const metadata = getLinkMetadata(href);
  if (!Object.keys(metadata).length) {
    return link;
  }
  const url = stripLinkMetadata(href);
  return link.type.create({
    ...link.attrs,
    href: setLinkMetadata(url, filterMetadata(metadata, keys, { url })),
  });
}

function filterMark(mark: Mark, keys: MetadataKeys): Mark | undefined {
  const key = keys.get(mark.type.name);
  const href = mark.attrs.href;
  if (key) {
    return filterStyle(mark, key);
  } else if (mark.type.name === "link" && typeof href === "string") {
    return filterLink(mark, href, keys);
  }
  return mark;
}

function filterAttributes(
  attrs: Node["attrs"],
  keys: MetadataKeys,
): Node["attrs"] {
  const metadata = attrs[METADATA_PROP];
  if (typeof metadata !== "string" || !metadata) {
    return attrs;
  }
  // The source of an image, or of another file block.
  const url = typeof attrs.url === "string" ? attrs.url : undefined;
  return {
    ...attrs,
    [METADATA_PROP]: stringifyMetadata(
      filterMetadata(parseMetadata(metadata), keys, { url }),
    ),
  };
}

function filterNode(node: Node, keys: MetadataKeys): Node {
  const marks = node.marks
    .map((mark) => filterMark(mark, keys))
    .filter((mark) => mark !== undefined);
  if (node.isText) {
    return node.mark(marks);
  }
  return node.type.create(
    filterAttributes(node.attrs, keys),
    filterFragment(node.content, keys),
    marks,
  );
}

function filterFragment(fragment: Fragment, keys: MetadataKeys): Fragment {
  const nodes: Node[] = [];
  fragment.forEach((node) => nodes.push(filterNode(node, keys)));
  return Fragment.fromArray(nodes);
}

/**
 * Filters the metadata held by the pasted (or dropped) content, which can come from anywhere (another page, another
 * application, a malicious web page): only the entries that their key, bound in the given service container, accepts
 * are kept (see `BlockMetadataKey.filterPasted`). The format of the pasted content doesn't tell where it comes from
 * (any web page can put BlockNote's own clipboard format on the clipboard), so all the pasted content is filtered,
 * except the content dragged within the editor, which comes from the edited content.
 */
const PastedMetadataFilterExtension = createExtension(
  ({
    options: { serviceContainer },
  }: ExtensionOptions<{ serviceContainer: Container }>) => {
    return {
      key: "pastedMetadataFilter",
      prosemirrorPlugins: [
        new Plugin({
          key: new PluginKey("pastedMetadataFilter"),
          props: {
            transformPasted: (slice, view) =>
              view.dragging
                ? slice
                : new Slice(
                    filterFragment(
                      slice.content,
                      getMetadataKeys(serviceContainer),
                    ),
                    slice.openStart,
                    slice.openEnd,
                  ),
          },
        }),
      ],
    };
  },
);

export { PastedMetadataFilterExtension, filterMetadata };
