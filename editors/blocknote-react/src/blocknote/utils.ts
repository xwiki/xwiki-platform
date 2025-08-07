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

import {
  BlockNoteEditor,
  CustomBlockConfig,
  CustomInlineContentConfig,
  InlineContentSchema,
  PartialBlock,
  PartialInlineContent,
  PropSchema,
  StyleSchema,
  insertOrUpdateBlock,
} from "@blocknote/core";
import {
  ReactCustomBlockImplementation,
  ReactInlineContentImplementation,
  createReactBlockSpec,
  createReactInlineContentSpec,
} from "@blocknote/react";
import { assertUnreachable } from "@xwiki/cristal-fn-utils";
import { ReactNode } from "react";

/**
 * Create a custom block to use in the BlockNote editor
 *
 * @param block - The block specification
 *
 * @returns A block definition
 *
 * @since 0.18
 */
function createCustomBlockSpec<
  const B extends CustomBlockConfig,
  const I extends InlineContentSchema,
  const S extends StyleSchema,
>(block: {
  config: B;
  implementation: ReactCustomBlockImplementation<B, I, S>;
  slashMenu: {
    title: string;
    aliases?: string[];
    group: string;
    icon: ReactNode;
    default: PartialBlock<Record<B["type"], B>>;
  };
  toolbar: () => ReactNode | null;
}) {
  return {
    block: createReactBlockSpec(block.config, block.implementation),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    slashMenuEntry: (editor: BlockNoteEditor<any>) => ({
      title: block.slashMenu.title,
      aliases: block.slashMenu.aliases,
      group: block.slashMenu.group,
      icon: block.slashMenu.icon,
      onItemClick: () => {
        insertOrUpdateBlock(editor, block.slashMenu.default);
      },
    }),
    toolbar,
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
 */
function createCustomInlineContentSpec<
  const I extends CustomInlineContentConfig,
  const S extends StyleSchema,
>(inlineContent: {
  config: I;
  implementation: ReactInlineContentImplementation<I, S>;
  slashMenu: {
    title: string;
    aliases?: string[];
    group: string;
    icon: ReactNode;
    default: PartialInlineContent<Record<I["type"], I>, S>;
  };
  toolbar: () => ReactNode | null;
}) {
  return {
    inlineContent: createReactInlineContentSpec(
      inlineContent.config,
      inlineContent.implementation,
    ),
    // eslint-disable-next-line @typescript-eslint/no-explicit-any
    slashMenuEntry: (editor: BlockNoteEditor<any>) => ({
      title: inlineContent.slashMenu.title,
      aliases: inlineContent.slashMenu.aliases,
      group: inlineContent.slashMenu.group,
      icon: inlineContent.slashMenu.icon,
      onItemClick: () => {
        editor.insertInlineContent([
          // @ts-expect-error: the AST is dynamically-typed with macros, so the types are incorrect here
          inlineContent.slashMenu.default,
        ]);
      },
    }),
    toolbar,
  };
}

/**
 * Description of a macro
 *
 * @since 0.20
 */
type Macro = {
  /** Name of the macro */
  name: string;

  // TODO: add translations support? or let the part providing the macros take care of it?
  /** Description of the macro */
  description: string;

  /** Description of the macro's parameters */
  parameters: Record<string, MacroParameterType>;

  /**
   * Macro's type
   *
   * `block`: only usable as a block (same level as paragraphs, headings, etc.)
   * `inline`: only usable inside other blocks such as paragraphs
   */
  renderType: "block" | "inline";

  /** Should the macro be hidden from the slash menu? */
  hidden: boolean;

  /** The concrete implementation to use in BlockNote */
  blockNote: MacroForBlockNote;
};

/**
 * Description of a macro's inner content
 *
 * @since 0.20
 */
type MacroForBlockNote =
  // Block macro
  | {
      type: "block";
      block: ReturnType<typeof createCustomBlockSpec>;
    }
  // Inline macro
  | {
      type: "inline";
      inlineContent: ReturnType<typeof createCustomInlineContentSpec>;
    };

/**
 * Description of a macro type
 *
 * @since 0.20
 */
type MacroParameterType = (
  | { type: "boolean" }
  // We use 'float' instead of 'number' here to make it more explicit to developers
  | { type: "float" }
  | { type: "string" }
  | { type: "stringEnum"; possibleValues: string[] }
) & {
  // Make the parameter optional
  optional?: true;
};

/**
 * Internal utility type to get the concrete TypeScript type from a macro parameter's definition
 */
type GetConcreteMacroParameterType<T extends MacroParameterType> =
  | (T extends {
      type: "boolean";
    }
      ? boolean
      : // : T extends { type: "int" }
        //   ? number
        T extends { type: "float" }
        ? number
        : T extends { type: "string" }
          ? string
          : T extends { type: "stringEnum" }
            ? T["possibleValues"][number]
            : never)
  | (T["optional"] extends true ? undefined : never);

/** Internal utility type making all properties that may be assigned `undefined` optional in a record */
type UndefinableToOptional<T> = {
  [K in keyof T as undefined extends T[K] ? K : never]?: Exclude<
    T[K],
    undefined
  >;
} & { [K in keyof T as undefined extends T[K] ? never : K]: T[K] };

/**
 * Internal utility type to get the concrete TypeScript record type from a macro's parameters definition
 *
 * Parameters defined as optional are both optional in the output record and can be assigned `undefined`
 */
type GetConcreteMacroParametersType<
  T extends Record<string, MacroParameterType>,
> = UndefinableToOptional<{
  [Param in keyof T]: GetConcreteMacroParameterType<T[Param]>;
}>;

/**Internal utility type to remove values that may be assigned `undefined` from a record */
type FilterUndefined<T> = {
  [K in keyof T as undefined extends T[K] ? never : K]: T[K];
};

/**
 * Arguments for creating a macro
 *
 * @since 0.20
 */
type MacroCreationArgs<Parameters extends Record<string, MacroParameterType>> =
  {
    /** The macro's name */
    name: string;

    /** The macro's description */
    description: string;

    /** The macro's render type (block or inline content) */
    renderType: "block" | "inline";

    /** Definition of every parameter */
    parameters: Parameters;

    /**
     * Default value of every required parameter
     *
     * Optional parameters will be omitted from the default object
     */
    defaultParameters: FilterUndefined<
      GetConcreteMacroParametersType<Parameters>
    >;

    /** Should the macro be hidden from the slash menu? */
    hidden?: boolean;

    /**
     * React render function
     *
     * @param parameters - The macro's parameters ; optional fields may be absent or equal to `undefined`
     * @param contentRef - The editable section of the block, handled by BlockNote
     *
     * @returns The React node to render the macro as
     */
    render(
      parameters: GetConcreteMacroParametersType<Parameters>,
      contentRef: (node: HTMLElement | null) => void,
    ): React.ReactNode;
  };

/**
 * The prefix used for macro names in BlockNote
 *
 * @since 0.20
 * */
const MACRO_NAME_PREFIX = "Macro.";

/**
 * Create a macro.
 *
 * This will effectively return a `Macro` object
 *
 * @param args - Informations about the macro to create
 *
 * @returns The macro
 *
 * @since 0.20
 */
function createMacro<Parameters extends Record<string, MacroParameterType>>({
  name,
  description,
  parameters,
  defaultParameters,
  hidden,
  render,
  renderType,
}: MacroCreationArgs<Parameters>): Macro {
  // Compute the macro name
  const blockNoteName = `${MACRO_NAME_PREFIX}${name}`;

  // Compute the BlockNote properties schema
  const propSchema: PropSchema = Object.fromEntries(
    Object.entries(parameters).map(([name, param]) => [
      name,
      {
        type:
          param.type === "string" || param.type === "stringEnum"
            ? "string"
            : param.type === "float"
              ? "number"
              : param.type === "boolean"
                ? "boolean"
                : assertUnreachable(param),
        default:
          name in defaultParameters
            ? // NOTE: the type of `defaultParameters` is the union of two different objects
              //       which means it doesn't have an index signature ; hence the need for a typecast here
              (defaultParameters as Record<string, string>)[name]
            : undefined,
        optional: param.optional,
        values: param.type === "stringEnum" ? param.possibleValues : undefined,
      },
    ]),
  );

  // Define the common slash menu properties
  const slashMenu = {
    title: description,
    group: "Macros",
    icon: "M",
    aliases: [],
  };

  // Define the common default value properties
  const defaultValue = {
    ...defaultParameters,
    // TODO: statically type parameters so that the `type` name cannot be used,
    //       as it would be shadowed here otherwise
    type: `${MACRO_NAME_PREFIX}${name}`,
  };

  // Block and inline macros are defined pretty differently, so a bit of logic was computed ahead of time
  // to share it between the two definitions here.
  const concreteMacro: MacroForBlockNote =
    renderType === "block"
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
              render: ({ contentRef, block }) =>
                render(
                  block.props as GetConcreteMacroParametersType<Parameters>,
                  contentRef,
                ),
            },
            slashMenu: {
              ...slashMenu,
              default: defaultValue,
            },
            // TODO: allow macros to define their own toolbar, using a set of provided UI components (buttons, ...)
            toolbar: () => null,
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
              render: ({ contentRef, inlineContent }) =>
                render(
                  inlineContent.props as GetConcreteMacroParametersType<Parameters>,
                  contentRef,
                ),
            },
            slashMenu: {
              ...slashMenu,
              default: [defaultValue],
            },
            // TODO: allow macros to define their own toolbar, using a set of provided UI components (buttons, ...)
            toolbar: () => null,
          }),
        };

  return {
    name,
    description,
    parameters,
    hidden: hidden ?? false,
    renderType,
    blockNote: concreteMacro,
  };
}

export { MACRO_NAME_PREFIX, createCustomBlockSpec, createMacro };
export type { Macro, MacroCreationArgs, MacroParameterType };
