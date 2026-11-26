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
import { CustomInsertMacroButton } from "./CustomInsertMacroButton";
import { CustomMacroEditButton } from "./CustomMacroEditButton";
import { getMacroEditing } from "./macroEditing";
import { MacroOutputCache } from "./macroOutputCache";
import { createXWikiInlineMacro, createXWikiMacroBlock } from "./macroSpec";
import {
  MACRO_NAME_PREFIX,
  adaptMacroForBlockNote,
  insertMacroInvocation,
} from "./utils";
import { filterMap } from "@xwiki/platform-fn-utils";
import { UniAstToReactJsxConverter } from "@xwiki/platform-uniast-react-jsx";
import { createElement } from "react";
import { RiFileList3Fill } from "react-icons/ri";
import type { EditorInstance } from "../../editorInstance";
import type {
  EditorFeature,
  FeatureContext,
  FeatureContribution,
  SchemaContribution,
  SlashMenuItem,
} from "../editorFeature";
import type { BlockNoteConcreteMacro } from "./utils";
import type { MacroWithUnknownParamsType } from "@xwiki/platform-macros-api";
import type { RemoteURLSerializerProvider } from "@xwiki/platform-model-remote-url-api";
import type { SyntaxConfig } from "@xwiki/platform-syntaxes-config";
import type { Container } from "inversify";

/**
 * @param serviceContainer - the service container of the editor instance
 * @param list - the client-rendered macros
 * @returns the client-rendered macros adapted to BlockNote, sorted by name
 */
function adaptMacros(
  serviceContainer: Container,
  list: MacroWithUnknownParamsType[],
): BlockNoteConcreteMacro[] {
  if (!list.length) {
    return [];
  }
  const uniAstToReactJsxConverter = new UniAstToReactJsxConverter(
    serviceContainer
      .get<RemoteURLSerializerProvider>("RemoteURLSerializerProvider")
      .get()!,
  );
  return list
    .map((macro) => adaptMacroForBlockNote(macro, uniAstToReactJsxConverter))
    .sort((a, b) => a.macro.infos.name.localeCompare(b.macro.infos.name));
}

/**
 * @param macros - the client-rendered macros
 * @param outputs - the output of the server-rendered macro calls of the editor instance
 * @returns the block and inline content types of the macro calls: the server-rendered ones, and those of the
 *   client-rendered macros
 */
function macroSchema(
  macros: BlockNoteConcreteMacro[],
  outputs: MacroOutputCache,
): SchemaContribution {
  return {
    blockSpecs: {
      xwikiMacroBlock: createXWikiMacroBlock(outputs)(),
      ...Object.fromEntries(
        filterMap(macros, ({ macro, bnRendering }) =>
          bnRendering.type === "block"
            ? [
                `${MACRO_NAME_PREFIX}${macro.infos.id}`,
                bnRendering.block.block(),
              ]
            : null,
        ),
      ),
    },
    inlineContentSpecs: {
      xwikiInlineMacro: createXWikiInlineMacro(outputs),
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
  };
}

/**
 * @param instance - the editor instance where the macro is inserted
 * @param macros - the client-rendered macros
 * @param syntax - the configuration of the syntax of the edited content
 * @param t - translates the given key, in the language of the editor UI
 * @returns the slash menu items inserting the client-rendered macros that the syntax can save, and the "Macro" item
 *   opening the macro editor (used to insert the server-rendered macros), when a `MacroEditor` is bound
 */
function macroSlashMenuItems(
  instance: EditorInstance,
  macros: BlockNoteConcreteMacro[],
  syntax: SyntaxConfig,
  t: (key: string) => string,
): SlashMenuItem[] {
  const { editor } = instance;
  const { blocks, inlineContents } = syntax.features;
  const macroEditing = getMacroEditing(instance);
  // The icon of a client-rendered macro is any React node (e.g. a letter), which BlockNote renders as well.
  return [
    ...(blocks.macros
      ? filterMap(macros, ({ bnRendering }) =>
          bnRendering.type === "block" && bnRendering.block.slashMenuEntry
            ? (bnRendering.block.slashMenuEntry(editor) as SlashMenuItem)
            : null,
        )
      : []),
    ...(inlineContents.macros
      ? filterMap(macros, ({ bnRendering }) =>
          bnRendering.type === "inline" &&
          bnRendering.inlineContent.slashMenuEntry
            ? (bnRendering.inlineContent.slashMenuEntry(
                editor,
              ) as SlashMenuItem)
            : null,
        )
      : []),
    ...(macroEditing
      ? [
          {
            title: t("blocknote.slashMenu.macro.title"),
            subtext: t("blocknote.slashMenu.macro.subtext"),
            aliases: ["macro"],
            group: t("blocknote.slashMenu.macro.group"),
            icon: createElement(RiFileList3Fill),
            onItemClick: () =>
              macroEditing.insert(
                {
                  kind: "block",
                  id: null,
                  params: null,
                  body: { type: "none" },
                },
                (invocation) => insertMacroInvocation(editor, invocation),
              ),
          },
        ]
      : []),
  ];
}

/**
 * Displays, inserts and edits macro calls: the server-rendered macro calls, displayed with their output
 * (`features.macros.outputs`), and the client-rendered macros (`features.macros.list`), added to the schema of the editor, the slash menu items and block type select items of the
 * client-rendered macros, and the actions to insert and edit macro calls with the `MacroEditor` bound in the service
 * container (the "Macro" slash menu item and the toolbar buttons), hidden without one. Nothing is contributed when the
 * syntax of the edited content doesn't support macros.
 *
 * @since 18.10.0RC1
 * @beta
 */
const macrosFeature: EditorFeature = {
  id: "macros",
  contribute({
    options,
    serviceContainer,
    syntax,
    t,
    getInstance,
  }: FeatureContext): FeatureContribution | undefined {
    const { blocks, inlineContents } = syntax.features;
    if (!blocks.macros && !inlineContents.macros) {
      return undefined;
    }
    // Not called when the macros are disabled.
    const macroOptions = options.features?.macros || undefined;
    const macros = adaptMacros(serviceContainer, macroOptions?.list ?? []);
    // The output of the server-rendered macro calls is kept with the macro calls rendering it.
    const outputs = new MacroOutputCache(macroOptions?.outputs);
    return {
      schema: macroSchema(macros, outputs),
      slashMenuItems: () =>
        macroSlashMenuItems(getInstance(), macros, syntax, t),
      formattingToolbar: {
        // Hidden when no macro editor is available.
        items: [
          <CustomMacroEditButton key="macroEditButton" />,
          <CustomInsertMacroButton key="insertMacroButton" />,
        ],
        blockTypeSelectItems: filterMap(
          macros,
          (macro) => macro.dropdownTransformItem,
        ),
      },
    };
  },
};

export { macrosFeature };
