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

import { BlockNoteEditor, insertOrUpdateBlock } from "@blocknote/core";
import {
  createReactBlockSpec,
  createReactInlineContentSpec,
} from "@blocknote/react";
import { assertUnreachable, objectEntries } from "@manuelleducorg/fn-utils";
import type {
  CustomBlockConfig,
  CustomInlineContentConfig,
  InlineContentSchema,
  PartialBlock,
  PartialInlineContent,
  PropSchema,
  PropSpec,
  Props,
  StyleSchema,
} from "@blocknote/core";
import type {
  ReactCustomBlockImplementation,
  ReactInlineContentImplementation,
} from "@blocknote/react";
import type {
  MacroWithUnknownParamsType,
  UnknownMacroParamsType,
} from "@manuelleducorg/macros-api";
import type { MacrosAstToReactJsxConverter } from "@manuelleducorg/macros-ast-react-jsx";
import type { ReactNode } from "react";

/**
 * Create a custom block to use in the BlockNote editor
 *
 * @param block - The block specification
 *
 * @returns A block definition
 *
 * @since 0.18
 * @internal
 */
function createCustomBlockSpec<
  const B extends CustomBlockConfig,
  const I extends InlineContentSchema,
  const S extends StyleSchema,
>({
  config,
  implementation,
  slashMenu,
  customToolbar,
}: {
  config: B;
  implementation: ReactCustomBlockImplementation<B, I, S>;
  slashMenu:
    | false
    | {
        title: string;
        aliases?: string[];
        group: string;
        icon: ReactNode;
        default: () => PartialBlock<Record<B["type"], B>>;
      };
  customToolbar: (() => ReactNode) | null;
}) {
  return {
    block: createReactBlockSpec(config, implementation),

    slashMenuEntry: !slashMenu
      ? (false as const)
      : // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (editor: BlockNoteEditor<any>) => ({
          title: slashMenu.title,
          aliases: slashMenu.aliases,
          group: slashMenu.group,
          icon: slashMenu.icon,
          onItemClick: () => {
            insertOrUpdateBlock(editor, slashMenu.default());
          },
        }),

    customToolbar,
  };
}

/**
 * Create a custom inilne content to use in the BlockNote editor
 *
 * @param inlineContent - The inline content specification
 *
 * @returns An inline content definition
 *
 * @since 0.20
 * @beta
 */

/**
 * @since 0.23
 * @internal
 */
function createCustomInlineContentSpec<
  const I extends CustomInlineContentConfig,
  const S extends StyleSchema,
>({
  config,
  implementation,
  slashMenu,
  customToolbar,
}: {
  config: I;
  implementation: ReactInlineContentImplementation<I, S>;
  slashMenu:
    | false
    | {
        title: string;
        aliases?: string[];
        group: string;
        icon: ReactNode;
        default: () => PartialInlineContent<Record<I["type"], I>, S>;
      };
  customToolbar: (() => ReactNode) | null;
}) {
  return {
    inlineContent: createReactInlineContentSpec(config, implementation),

    slashMenuEntry: !slashMenu
      ? (false as const)
      : // eslint-disable-next-line @typescript-eslint/no-explicit-any
        (editor: BlockNoteEditor<any>) => ({
          title: slashMenu.title,
          aliases: slashMenu.aliases,
          group: slashMenu.group,
          icon: slashMenu.icon,
          onItemClick: () => {
            editor.insertInlineContent([
              // @ts-expect-error: the AST is dynamically-typed with macros, so the types are incorrect here
              slashMenu.default(),
            ]);
          },
        }),

    customToolbar,
  };
}

/**
 * Name prefix for macro blocks and inline contents in BlockNote
 *
 * @since 0.23
 * @beta
 */
const MACRO_NAME_PREFIX = "Macro_";

/**
 * Description of a macro adapted by `adaptMacroForBlockNote`
 *
 * @since 0.20
 * @internal
 */
type BlockNoteConcreteMacro = {
  /** Type-erased macro */
  macro: MacroWithUnknownParamsType;

  /** Rendering part */
  bnRendering: // Block macro
  | {
        type: "block";
        block: ReturnType<typeof createCustomBlockSpec>;
      }
    // Inline macro
    | {
        type: "inline";
        inlineContent: ReturnType<typeof createCustomInlineContentSpec>;
      };
};

/**
 * Internal context required for macros execution
 *
 * @since 0.23
 * @beta
 */
type ContextForMacros = {
  /**
   * Request the opening of an UI to edit the macro's parameters (e.g. a modal)
   *
   * @param macro - Description of the macro being edited
   * @param params - Current parameters of the macro
   * @param update - Calling this function will replace the existing macro's parameters with the provided ones
   */
  openParamsEditor(
    macro: MacroWithUnknownParamsType,
    params: UnknownMacroParamsType,
    update: (newProps: UnknownMacroParamsType) => void,
  ): void;
};

/**
 * Adapt a macro to be used inside BlockNote.
 *
 * @param macro - The macro to adapt
 * @param ctx - The context used to handle the macro inside BlockNote
 * @param jsxConverter - A converter that transforms macro ASTs to React JSX
 *
 * @returns - The BlockNote-compatible macro
 *
 * @since 0.20
 * @beta
 */
function adaptMacroForBlockNote(
  macro: MacroWithUnknownParamsType,
  ctx: ContextForMacros,
  jsxConverter: MacrosAstToReactJsxConverter,
): BlockNoteConcreteMacro {
  const { id, name, params, defaultParameters } = macro.infos;

  // Compute the macro name
  const blockNoteName = `${MACRO_NAME_PREFIX}${id}`;

  const propSchema: Record<
    string,
    PropSpec<boolean | number | string> & { optional?: true }
  > = {};

  for (const [name, { type, optional }] of objectEntries(params)) {
    propSchema[name] = {
      type:
        type === "string"
          ? "string"
          : type === "float"
            ? "number"
            : type === "boolean"
              ? "boolean"
              : assertUnreachable(type),
      optional,
      default: undefined,
    };
  }

  const getSlashMenu = <T,>(opts: (defaultValue: () => unknown) => T) =>
    defaultParameters
      ? {
          title: name,
          group: "Macros",
          icon: "M",
          aliases: [],
          ...opts(() => ({
            // TODO: statically type parameters so that the `type` name cannot be used,
            //       as it would be shadowed here otherwise
            type: `${MACRO_NAME_PREFIX}${id}`,
            props: defaultParameters,
          })),
        }
      : false;

  // The rendering function
  const renderMacro = (
    contentRef: (node: HTMLElement | null) => void,
    props: Props<PropSchema>,
    update: (newParams: Props<PropSchema>) => void,
  ) => {
    const openParamsEditor = () =>
      // eslint-disable-next-line @typescript-eslint/no-explicit-any
      ctx.openParamsEditor(macro, props, update as any);

    const renderedJsx =
      macro.renderAs === "block"
        ? jsxConverter.blocksToReactJSX(macro.render(props), {
            type: "block",
            ref: contentRef,
          })
        : jsxConverter.inlineContentsToReactJSX(macro.render(props), {
            type: "inline",
            ref: contentRef,
          });

    if (renderedJsx instanceof Error) {
      // TODO: how to display properly an error?
      return <strong>Failed to render macro: {renderedJsx.message}</strong>;
    }

    return macro.renderAs === "block" ? (
      <div style={{ userSelect: "none" }} onDoubleClick={openParamsEditor}>
        {renderedJsx}
      </div>
    ) : (
      <span style={{ userSelect: "none" }} onDoubleClick={openParamsEditor}>
        {renderedJsx}
      </span>
    );
  };

  // Block and inline macros are defined pretty differently, so a bit of logic was computed ahead of time
  // to share it between the two definitions here.
  const bnRendering: BlockNoteConcreteMacro["bnRendering"] =
    macro.renderAs === "block"
      ? {
          type: "block",
          block: createCustomBlockSpec({
            config: {
              type: blockNoteName,
              // TODO: when BlockNote supports internal content in custom blocks, set this to "inline" if the macro can have children
              // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
              content: "none",
              propSchema,
            },
            implementation: {
              render: ({ contentRef, block, editor }) =>
                renderMacro(contentRef, block.props, (newProps) => {
                  editor.updateBlock(block.id, { props: newProps });
                }),
            },
            slashMenu: getSlashMenu((getDefaultValue) => ({
              // eslint-disable-next-line @typescript-eslint/no-explicit-any
              default: () => getDefaultValue() as any,
            })),
            // TODO: allow macros to define their own toolbar, using a set of provided UI components (buttons, ...)
            // Tracking issue: https://jira.xwiki.org/browse/CRISTAL-708
            customToolbar: null,
          }),
        }
      : {
          type: "inline",
          inlineContent: createCustomInlineContentSpec({
            config: {
              type: blockNoteName,
              // TODO: when BlockNote supports internal content in custom inline contents themselves, set this to "styled" if the macro can have children
              // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
              content: "none",
              propSchema,
            },
            implementation: {
              render: ({ contentRef, inlineContent, updateInlineContent }) =>
                renderMacro(contentRef, inlineContent.props, (newProps) => {
                  updateInlineContent({
                    type: inlineContent.type,
                    props: newProps,
                    // TODO: make it editable!
                    content: inlineContent.content,
                  });
                }),
            },
            slashMenu: getSlashMenu((getDefaultValue) => ({
              default: () => [
                // eslint-disable-next-line @typescript-eslint/no-explicit-any
                getDefaultValue() as any,
              ],
            })),
            // TODO: allow macros to define their own toolbar, using a set of provided UI components (buttons, ...)
            // Tracking issue: https://jira.xwiki.org/browse/CRISTAL-708
            customToolbar: null,
          }),
        };

  return { macro, bnRendering };
}

export {
  MACRO_NAME_PREFIX,
  adaptMacroForBlockNote,
  createCustomBlockSpec,
  createCustomInlineContentSpec,
};
export type { BlockNoteConcreteMacro, ContextForMacros };
