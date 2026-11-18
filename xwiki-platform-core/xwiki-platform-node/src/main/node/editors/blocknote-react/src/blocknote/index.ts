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

import { DefinitionListItemBlock } from "./definitionList";
import { XWikiGroupBlock } from "./group";
import { XWikiRawBlock } from "./raw";
import {
  SubscriptStyle,
  SuperscriptStyle,
  VerbatimStyle,
  XWikiParametersStyle,
} from "./styles";
import {
  XWikiInlineMacro,
  XWikiMacroBlock,
} from "../features/macros/macroSpec";
import translations from "../translations";
import {
  BlockNoteEditor,
  BlockNoteSchema,
  combineByGroup,
  defaultBlockSpecs,
  defaultInlineContentSpecs,
  defaultStyleSpecs,
} from "@blocknote/core";
import { filterSuggestionItems } from "@blocknote/core/extensions";
import * as locales from "@blocknote/core/locales";
import { getDefaultReactSlashMenuItems } from "@blocknote/react";
import type {
  SchemaContribution,
  SlashMenuItem,
} from "../features/editorFeature";
import type {
  Block,
  InlineContent,
  Link,
  PartialLink,
  StyledText,
} from "@blocknote/core";
import type { DefaultReactSuggestionItem } from "@blocknote/react";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";

/**
 * Create the BlockNote editor's schema
 *
 * Contains all the blocks usable inside the editor
 *
 * @param contributed - the block and inline content types added by the editing features, whose types are erased; they
 *   replace the types of the same name (e.g. BlockNote's code block), whose types the typed API keeps
 * @returns The created schema
 */
function createBlockNoteSchema(contributed?: SchemaContribution) {
  const blockNoteSchema = BlockNoteSchema.create({
    blockSpecs: {
      ...defaultBlockSpecs,
      xwikiDefinitionListItem: DefinitionListItemBlock(),
      xwikiGroup: XWikiGroupBlock(),
      xwikiMacroBlock: XWikiMacroBlock(),
      xwikiRaw: XWikiRawBlock(),

      // The block types added by the editing features, whose types are erased under an index signature. They replace
      // the types of the same name (e.g. BlockNote's code block and image), the typed API keeping the types of these.
      ...Object.fromEntries(Object.entries(contributed?.blockSpecs ?? {})),
    },

    inlineContentSpecs: {
      ...defaultInlineContentSpecs,

      // The generic inline macro (server-rendered inline macros without a dedicated client-side rendering). It is
      // registered through Object.fromEntries so that its type stays erased under an index signature, like the types
      // added by the editing features below: a statically-typed custom inline content entry would tighten the inline
      // content schema and break the call sites that expect the default inline content schema.
      ...Object.fromEntries([["xwikiInlineMacro", XWikiInlineMacro]]),

      // The inline content types added by the editing features.
      ...Object.fromEntries(
        Object.entries(contributed?.inlineContentSpecs ?? {}),
      ),
    },

    styleSpecs: {
      ...defaultStyleSpecs,
      subscript: SubscriptStyle,
      superscript: SuperscriptStyle,
      xwikiVerbatim: VerbatimStyle,
      xwikiParameters: XWikiParametersStyle,
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
  // eslint-disable-next-line import-x/namespace
  return locales[lang];
}

/**
 * @since 18.0.0RC1
 * @beta
 */
type EditorLanguage = keyof typeof locales & keyof typeof translations;

/**
 * Parameters used for generating the slash menu
 */
type SuggestionMenuItemsParams = {
  /** The query to filter the suggestions by */
  query: string;
  /** Instance of the BlockNote editor */
  editor: EditorType;
  /** Syntax of the current document */
  syntax: SyntaxConfig;
  /** The items contributed by the editing features */
  featureItems: SlashMenuItem[];
};

/**
 * @param blocks - the blocks supported by the syntax of the edited content
 * @returns the keys of BlockNote's default slash menu items inserting blocks that the syntax can't save
 */
function getUnsupportedDefaultItemKeys(
  blocks: SyntaxConfig["features"]["blocks"],
): Set<string> {
  const unsupported: [boolean, string[]][] = [
    [
      blocks.headings.levels1To3,
      [
        "heading",
        "heading_2",
        "heading_3",
        "toggle_heading",
        "toggle_heading_2",
        "toggle_heading_3",
      ],
    ],
    [blocks.headings.levels4To6, ["heading_4", "heading_5", "heading_6"]],
    [blocks.code.basicCodeBlocks, ["code_block"]],
    [blocks.quotes, ["quote"]],
    [blocks.lists.bulletLists, ["bullet_list", "toggle_list"]],
    [blocks.lists.contiguousNumberedLists, ["numbered_list"]],
    [blocks.lists.checkableLists, ["check_list"]],
    [blocks.tables.basicTables, ["table"]],
    [blocks.images.basicImages, ["image"]],
    [blocks.dividers, ["divider"]],
  ];
  return new Set(
    unsupported.flatMap(([supported, keys]) => (supported ? [] : keys)),
  );
}

/**
 * The editing features replace a default slash menu item by contributing an item with the same key, which takes the
 * place of the default one.
 *
 * @param defaultItems - BlockNote's default slash menu items
 * @param featureItems - the slash menu items contributed by the editing features
 * @returns the default items, some of them replaced, and the contributed items that don't replace any default item
 */
function replaceDefaultItems(
  defaultItems: SlashMenuItem[],
  featureItems: SlashMenuItem[],
): { items: SlashMenuItem[]; additionalItems: SlashMenuItem[] } {
  const defaultKeys = new Set(defaultItems.map((item) => item.key));
  const replacements = new Map(
    featureItems
      .filter((item) => item.key && defaultKeys.has(item.key))
      .map((item) => [item.key, item]),
  );
  return {
    items: defaultItems.map((item) => replacements.get(item.key) ?? item),
    additionalItems: featureItems.filter(
      (item) => !item.key || !defaultKeys.has(item.key),
    ),
  };
}

/**
 * Suggests a set of suggestion from the menu items.
 *
 * @param params - see {@link SuggestionMenuItemsParams}
 */
function querySuggestionsMenuItems({
  editor,
  query,
  syntax,
  featureItems,
}: SuggestionMenuItemsParams): DefaultReactSuggestionItem[] {
  // The default items keep the key of their BlockNote dictionary entry, even if their type doesn't declare it.
  const unsupportedKeys = getUnsupportedDefaultItemKeys(syntax.features.blocks);
  const { items, additionalItems } = replaceDefaultItems(
    (getDefaultReactSlashMenuItems(editor) as SlashMenuItem[]).filter(
      (item) => !unsupportedKeys.has(item.key!),
    ),
    featureItems,
  );

  return filterSuggestionItems(combineByGroup(items, additionalItems), query);
}

/**
 * Schema of the BlockNote editor
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorSchema = ReturnType<typeof createBlockNoteSchema>;

/**
 * Block schema for BlockNote
 *
 * @since 18.0.0RC1
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
 * Inline content schema for BlockNote
 *
 * @since 18.0.0RC1
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
 * Style schema for BlockNote
 *
 * @since 18.0.0RC1
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
 * Type of a BlockNote editor instance
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorType = BlockNoteEditor<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * Typesafe BlockNote type
 *
 * @since 18.0.0RC1
 * @beta
 */
type BlockType = Block<
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * Typesafe BlockNote type of the a given kind
 *
 * @since 18.0.0RC1
 * @beta
 */
type BlockOfType<B extends BlockType["type"]> = Extract<BlockType, { type: B }>;

/**
 * Typesafe BlockNote inline content
 *
 * @since 18.0.0RC1
 * @beta
 */
type InlineContentType = InlineContent<
  EditorInlineContentSchema,
  EditorStyleSchema
>;

/**
 * Typesafe BlockNote styled text
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorStyledText = StyledText<EditorStyleSchema>;

/**
 * Typesafe BlockNote link
 *
 * @since 18.0.0RC1
 * @beta
 */
type EditorLink = Link<EditorStyleSchema>;

/**
 * Typesafe BlockNote link to insert, whose content is the label rather than styled text
 *
 * @since 18.10.0RC1
 * @beta
 */
type EditorPartialLink = PartialLink<EditorStyleSchema>;

export type {
  BlockOfType,
  BlockType,
  EditorBlockSchema,
  EditorInlineContentSchema,
  EditorLanguage,
  EditorLink,
  EditorPartialLink,
  EditorSchema,
  EditorStyleSchema,
  EditorStyledText,
  EditorType,
  InlineContentType,
};

export { createBlockNoteSchema, createDictionary, querySuggestionsMenuItems };
