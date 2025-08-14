/**
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

import { Heading4, Heading5, Heading6 } from "./blocks/Headings";
import { MACRO_NAME_PREFIX, Macro } from "./utils";
import translations from "../translations";
import {
  Block,
  BlockNoteEditor,
  BlockNoteSchema,
  Link,
  StyledText,
  combineByGroup,
  defaultBlockSpecs,
  defaultInlineContentSpecs,
  filterSuggestionItems,
} from "@blocknote/core";
import * as locales from "@blocknote/core/locales";
import {
  DefaultReactSuggestionItem,
  getDefaultReactSlashMenuItems,
} from "@blocknote/react";
import { filterMap } from "@xwiki/cristal-fn-utils";

/**
 * Create the BlockNote editor's schema
 *
 * Contains all the blocks usable inside the editor
 *
 * @returns The created schema
 */
function createBlockNoteSchema(macros: Macro[]) {
  // Get rid of some block types
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { audio, video, file, toggleListItem, ...remainingBlockSpecs } =
    defaultBlockSpecs;

  const blockNoteSchema = BlockNoteSchema.create({
    blockSpecs: {
      ...remainingBlockSpecs,

      // Custom blocks
      Heading4: Heading4.block,
      Heading5: Heading5.block,
      Heading6: Heading6.block,

      // Macros
      ...Object.fromEntries(
        filterMap(macros, (macro) =>
          macro.blockNote.type === "block"
            ? [`${MACRO_NAME_PREFIX}${macro.name}`, macro.blockNote.block.block]
            : null,
        ),
      ),
    },

    inlineContentSpecs: {
      ...defaultInlineContentSpecs,

      // Macros
      ...Object.fromEntries(
        filterMap(macros, (macro) =>
          macro.blockNote.type === "inline"
            ? [
                `${MACRO_NAME_PREFIX}${macro.name}`,
                macro.blockNote.inlineContent.inlineContent,
              ]
            : null,
        ),
      ),
    },
  });

  return blockNoteSchema;
}

/**
 * Create a translated dictionary for the BlockNote editor
 *
 * @param lang - The dictionary's language
 *
 * @returns The dictionary in the requested language
 */
function createDictionary(lang: EditorLanguage) {
  // eslint-disable-next-line import/namespace
  return locales[lang];
}

type EditorLanguage = keyof typeof locales & keyof typeof translations;

function querySuggestionsMenuItems(
  editor: EditorType,
  query: string,
  macros: Macro[],
): DefaultReactSuggestionItem[] {
  return filterSuggestionItems(
    combineByGroup(
      getDefaultReactSlashMenuItems(editor),

      // Custom blocks
      filterMap([Heading4, Heading5, Heading6], (custom) =>
        custom.slashMenuEntry ? custom.slashMenuEntry(editor) : null,
      ),

      // Block macros
      filterMap(macros, (macro) =>
        macro.blockNote.type === "block" && macro.blockNote.block.slashMenuEntry
          ? macro.blockNote.block.slashMenuEntry(editor)
          : null,
      ),

      // Inline macros
      filterMap(macros, (macro) =>
        macro.blockNote.type === "inline" &&
        macro.blockNote.inlineContent.slashMenuEntry
          ? macro.blockNote.inlineContent.slashMenuEntry(editor)
          : null,
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
  EditorLanguage,
  EditorLink,
  EditorSchema,
  EditorStyleSchema,
  EditorStyledText,
  EditorType,
};

export { createBlockNoteSchema, createDictionary, querySuggestionsMenuItems };
