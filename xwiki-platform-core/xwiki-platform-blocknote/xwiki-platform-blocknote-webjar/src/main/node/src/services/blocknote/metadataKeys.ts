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
import type { BlockMetadataKey } from "@xwiki/platform-editors-blocknote-react";

// The metadata of the blocks and links that the BlockNote schema can't hold, extracted from the loaded content and put
// back on save. It comes from the edited content, so it's shared with the other users editing the content in realtime:
// any of them can save the content.

/**
 * The parameters of a block, a link or a text style (e.g. the class of a paragraph).
 */
const XWIKI_PARAMETERS: BlockMetadataKey = {
  name: "xwikiParameters",
  shared: true,
};

/**
 * The resource reference of a link or an image.
 */
const XWIKI_REFERENCE: BlockMetadataKey = {
  name: "xwikiReference",
  shared: true,
};

/**
 * Whether a link is free-standing (i.e. a URL without a label, nor brackets).
 */
const XWIKI_FREESTANDING: BlockMetadataKey = {
  name: "xwikiFreestanding",
  shared: true,
};

/**
 * The label generated for a link without a label.
 */
const XWIKI_GENERATED_LABEL: BlockMetadataKey = {
  name: "xwikiGeneratedLabel",
  shared: true,
};

/**
 * Whether the verbatim content of a code block starts with a new line that is hidden in the code block.
 */
const XWIKI_LEADING_NEW_LINE: BlockMetadataKey = {
  name: "xwikiLeadingNewLine",
  shared: true,
};

/**
 * Whether the verbatim content of a code block ends with a new line that is hidden in the code block.
 */
const XWIKI_TRAILING_NEW_LINE: BlockMetadataKey = {
  name: "xwikiTrailingNewLine",
  shared: true,
};

/**
 * Whether a code block was produced from a verbatim block (otherwise it is saved as a code macro call).
 */
const XWIKI_VERBATIM: BlockMetadataKey = {
  name: "xwikiVerbatim",
  shared: true,
};

export {
  XWIKI_FREESTANDING,
  XWIKI_GENERATED_LABEL,
  XWIKI_LEADING_NEW_LINE,
  XWIKI_PARAMETERS,
  XWIKI_REFERENCE,
  XWIKI_TRAILING_NEW_LINE,
  XWIKI_VERBATIM,
};
