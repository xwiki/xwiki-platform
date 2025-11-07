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
import { MACRO_NAME_PREFIX } from "./utils";
import translations from "../translations";
import {
  BlockNoteEditor,
  BlockNoteSchema,
  combineByGroup,
  defaultBlockSpecs,
  defaultInlineContentSpecs,
  filterSuggestionItems,
} from "@blocknote/core";
import * as locales from "@blocknote/core/locales";
import { getDefaultReactSlashMenuItems } from "@blocknote/react";
import { filterMap } from "@xwiki/platform-fn-utils";
import type { BlockNoteConcreteMacro } from "./utils";
import type { Block, Link, StyledText } from "@blocknote/core";
import type { DefaultReactSuggestionItem } from "@blocknote/react";

/**
 * Create the BlockNote editor's schema
 *
 * Contains all the blocks usable inside the editor
 *
 * @returns The created schema
 * @since 0.20
 * @beta
 */
function createBlockNoteSchema(macros: BlockNoteConcreteMacro[]) {
  // Get rid of some block types
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const { audio, video, file, toggleListItem, ...remainingBlockSpecs } =
    defaultBlockSpecs;

  macros = [
    ...macros.sort((a, b) =>
      a.macro.infos.name.localeCompare(b.macro.infos.name),
    ),
  ];

  const blockNoteSchema = BlockNoteSchema.create({
    blockSpecs: {
      ...remainingBlockSpecs,

      // Custom blocks
      Heading4: Heading4.block,
      Heading5: Heading5.block,
      Heading6: Heading6.block,

      // Macros
      ...Object.fromEntries(
        filterMap(macros, ({ macro, bnRendering }) =>
          bnRendering.type === "block"
            ? [`${MACRO_NAME_PREFIX}${macro.infos.id}`, bnRendering.block.block]
            : null,
        ),
      ),
    },

    inlineContentSpecs: {
      ...defaultInlineContentSpecs,

      // Macros
      ...Object.fromEntries(
        filterMap(macros, ({ macro, bnRendering }) =>
          bnRendering.type === "inline"
            ? [
                `${MACRO_NAME_PREFIX}${macro.infos.id}`,
                bnRendering.inlineContent.inlineContent,
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
 * @since 0.19
 * @beta
 */
function createDictionary(lang: EditorLanguage) {
  // eslint-disable-next-line import/namespace
  return locales[lang];
}

/**
 * @since 0.20
 * @beta
 */
type EditorLanguage = keyof typeof locales & keyof typeof translations;

/**
 * Suggests a set of suggestion from the menu items.
 *
 * @param editor - the editor type
 * @param query - the query to filter the suggestions by
 * @param macros - the available macros
 * @since 0.16
 * @beta
 */
function querySuggestionsMenuItems(
  editor: EditorType,
  query: string,
  macros: BlockNoteConcreteMacro[],
): DefaultReactSuggestionItem[] {
  return filterSuggestionItems(
    combineByGroup(
      getDefaultReactSlashMenuItems(editor),

      // Custom blocks
      filterMap([Heading4, Heading5, Heading6], (custom) =>
        custom.slashMenuEntry ? custom.slashMenuEntry(editor) : null,
      ),

      // Block macros
      filterMap(macros, ({ bnRendering }) =>
        bnRendering.type === "block" && bnRendering.block.slashMenuEntry
          ? bnRendering.block.slashMenuEntry(editor)
          : null,
      ),

      // Inline macros
      filterMap(macros, ({ bnRendering }) =>
        bnRendering.type === "inline" &&
        bnRendering.inlineContent.slashMenuEntry
          ? bnRendering.inlineContent.slashMenuEntry(editor)
          : null,
      ),
    ),
    query,
  );
}

/**
 * @since 0.16
 * @beta
 */
type EditorSchema = ReturnType<typeof createBlockNoteSchema>;

/**
 * @since 0.16
 * @beta
 */
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

/**
 * @since 0.16
 * @beta
 */
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

/**
 * @since 0.16
 * @beta
 */
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

/**
 * @since 0.16
 * @beta
 */
type EditorType = BlockNoteEditor<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * @since 0.16
 * @beta
 */
type EditorStyledText = StyledText<EditorStyleSchema>;

/**
 * @since 0.16
 * @beta
 */
type EditorLink = Link<EditorStyleSchema>;

/**
 * @since 0.16
 * @beta
 */
type BlockType = Block<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * @since 0.16
 * @beta
 */
type BlockOfType<B extends BlockType["type"]> = Extract<BlockType, { type: B }>;

export type {
  BlockNoteConcreteMacro,
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
