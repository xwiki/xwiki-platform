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
import { ResourceType } from "@xwiki/platform-rendering-api";
import type {
  BlockMetadataKey,
  PastedMetadataContext,
} from "../../misc/blockMetadata";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

// The metadata of the blocks and links produced by the XWiki rendering that the BlockNote schema can't hold. It is kept
// in the edited content (see setBlockMetadata and setLinkMetadata), so that it is shared with the other users editing
// the content in realtime (any of them can save it), and restored by undo / redo.

const keep = (value: unknown) => value;

/**
 * The parameters of a block, a link or a text style (e.g. the class of a paragraph). They can hold any attribute, so
 * they are dropped from the pasted content.
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_PARAMETERS: BlockMetadataKey = {
  name: "xwikiParameters",
};

// The resources of the wiki: the user can only reach them with their own rights.
const WIKI_RESOURCE_TYPES = new Set([
  ResourceType.DOCUMENT,
  ResourceType.PAGE,
  ResourceType.SPACE,
  ResourceType.ATTACHMENT,
  ResourceType.PAGE_ATTACHMENT,
  ResourceType.ICON,
  ResourceType.USER,
]);

function isResourceReference(value: unknown): value is ResourceReference {
  const reference = value as ResourceReference | null;
  return (
    typeof reference === "object" &&
    reference !== null &&
    typeof reference.type === "string" &&
    typeof reference.reference === "string"
  );
}

function filterPastedReference(
  value: unknown,
  { url }: PastedMetadataContext,
): unknown {
  if (!isResourceReference(value)) {
    return undefined;
  }
  if (WIKI_RESOURCE_TYPES.has(value.type)) {
    return value;
  }
  // The other references (e.g. to a URL, a file path, a UNC path) are kept only when they point to what the pasted
  // content displays, otherwise the saved content would point to something else than what the user saw.
  const displayed = [value.reference, `${value.type}:${value.reference}`];
  return url !== undefined && displayed.includes(url) ? value : undefined;
}

/**
 * The resource reference of a link or an image. The references to wiki resources are kept in the pasted content; the
 * others only when they point to the URL displayed by the pasted content.
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_REFERENCE: BlockMetadataKey = {
  name: "xwikiReference",
  filterPasted: filterPastedReference,
};

/**
 * Whether a link is free-standing (i.e. a URL without a label, nor brackets).
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_FREESTANDING: BlockMetadataKey = {
  name: "xwikiFreestanding",
  filterPasted: keep,
};

/**
 * The label generated for a link without a label.
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_GENERATED_LABEL: BlockMetadataKey = {
  name: "xwikiGeneratedLabel",
  filterPasted: keep,
};

/**
 * Whether the verbatim content of a code block starts with a new line that is hidden in the code block.
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_LEADING_NEW_LINE: BlockMetadataKey = {
  name: "xwikiLeadingNewLine",
  filterPasted: keep,
};

/**
 * Whether the verbatim content of a code block ends with a new line that is hidden in the code block.
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_TRAILING_NEW_LINE: BlockMetadataKey = {
  name: "xwikiTrailingNewLine",
  filterPasted: keep,
};

/**
 * Whether a code block was produced from a verbatim block (otherwise it is saved as a code macro call).
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_VERBATIM: BlockMetadataKey = {
  name: "xwikiVerbatim",
  filterPasted: keep,
};

/**
 * All the metadata entries of the blocks and links produced by the XWiki rendering. The integrations binding the
 * `xwikiRenderingFeature` bind them in the service container as well, under `blockMetadataKeyName`, so that their
 * values are filtered rather than dropped when pasted.
 *
 * @since 18.10.0
 * @beta
 */
const XWIKI_METADATA_KEYS: readonly BlockMetadataKey[] = [
  XWIKI_PARAMETERS,
  XWIKI_REFERENCE,
  XWIKI_FREESTANDING,
  XWIKI_GENERATED_LABEL,
  XWIKI_LEADING_NEW_LINE,
  XWIKI_TRAILING_NEW_LINE,
  XWIKI_VERBATIM,
];

export {
  XWIKI_FREESTANDING,
  XWIKI_GENERATED_LABEL,
  XWIKI_LEADING_NEW_LINE,
  XWIKI_METADATA_KEYS,
  XWIKI_PARAMETERS,
  XWIKI_REFERENCE,
  XWIKI_TRAILING_NEW_LINE,
  XWIKI_VERBATIM,
};
