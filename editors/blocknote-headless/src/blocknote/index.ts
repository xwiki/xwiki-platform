/*
 * See the LICENSE file distributed with this work for additional
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

import { BlockQuote } from "./blocks/BlockQuote";
import { Heading4, Heading5, Heading6 } from "./blocks/Headings";
import {
  Block,
  BlockNoteEditor,
  BlockNoteSchema,
  Dictionary,
  Link,
  StyledText,
  combineByGroup,
  defaultBlockSpecs,
  filterSuggestionItems,
  locales,
} from "@blocknote/core";
import {
  DefaultReactSuggestionItem,
  getDefaultReactSlashMenuItems,
} from "@blocknote/react";
import {
  ColumnBlock,
  ColumnListBlock,
  getMultiColumnSlashMenuItems,
  locales as multiColumnLocales,
  withMultiColumn,
} from "@blocknote/xl-multi-column";

function createBlockNoteSchema() {
  // Get rid of some block types
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { audio, video, file, ...remainingBlockSpecs } = defaultBlockSpecs;

  const blockNoteSchema = BlockNoteSchema.create({
    blockSpecs: {
      ...remainingBlockSpecs,

      // First-party extension blocks
      column: ColumnBlock,
      columnList: ColumnListBlock,

      // Custom blocks
      Heading4: Heading4.block,
      Heading5: Heading5.block,
      Heading6: Heading6.block,
      BlockQuote: BlockQuote.block,
    },
  });

  return withMultiColumn(blockNoteSchema);
}

function createDictionary(): Dictionary & Record<string, unknown> {
  return {
    ...locales.en,

    // // First-party extensions
    multi_column: multiColumnLocales.en,
  };
}

function querySuggestionsMenuItems(
  editor: EditorType,
  query: string,
): DefaultReactSuggestionItem[] {
  return filterSuggestionItems(
    combineByGroup(
      getDefaultReactSlashMenuItems(editor),

      // // First-party extension blocks
      getMultiColumnSlashMenuItems(editor),

      // Custom blocks
      [Heading4, Heading5, Heading6, BlockQuote].map((custom) =>
        custom.slashMenuEntry(editor),
      ),
    ),
    query,
  );
}

type EditorSchema = ReturnType<typeof createBlockNoteSchema>;

type EditorBlockSchema =
  EditorSchema extends BlockNoteSchema<
    infer BlockSchema,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    infer _,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    infer __
  >
    ? BlockSchema
    : never;

type EditorInlineContentSchema =
  EditorSchema extends BlockNoteSchema<
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    infer _,
    infer InlineContentSchema,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    infer __
  >
    ? InlineContentSchema
    : never;

type EditorStyleSchema =
  EditorSchema extends BlockNoteSchema<
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    infer _,
    // eslint-disable-next-line @typescript-eslint/no-unused-vars
    infer __,
    infer StyleSchema
  >
    ? StyleSchema
    : never;

type EditorType = BlockNoteEditor<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

type EditorStyledText = StyledText<EditorStyleSchema>;
type EditorLink = Link<EditorStyleSchema>;

type BlockType = Block<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

type BlockOfType<B extends BlockType["type"]> = Extract<BlockType, { type: B }>;

export type {
  BlockOfType,
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorLink,
  EditorSchema,
  EditorStyleSchema,
  EditorStyledText,
  EditorType,
};

export { createBlockNoteSchema, createDictionary, querySuggestionsMenuItems };
