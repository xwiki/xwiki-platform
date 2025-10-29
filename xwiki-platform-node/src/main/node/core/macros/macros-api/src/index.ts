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
import type { MacroBlock, MacroInlineContent } from "./ast";

/**
 * Information about a macro.
 *
 * @since 0.23
 * @beta
 */
interface MacroInfos<Parameters extends Record<string, MacroParameterType>> {
  /** ID of the macro: only lowercase letters, uppercase letters, digits and underscores allowed */
  id: string;

  /** Name of the macro (to show in menus) */
  name: string;

  /** Description of the macro */
  // TODO: translations [https://jira.xwiki.org/browse/CRISTAL-707]
  description: string;

  /** Macro's parameters */
  params: Parameters;

  /** Description of the macro's parameters */
  // TODO: translations [https://jira.xwiki.org/browse/CRISTAL-707]
  paramsDescription: { [P in keyof Parameters]: string };

  /**
   * Default parameters for the macro
   *
   * If `false` is provided, the macro will be hidden
   */
  defaultParameters:
    | FilterUndefined<GetConcreteMacroParametersType<Parameters>>
    | false;
}

/**
 * Description of a block macro
 *
 * @since 0.23
 * @beta
 */
interface BlockMacro<Parameters extends Record<string, MacroParameterType>> {
  /** Macro informations */
  infos: MacroInfos<Parameters>;

  /** Indicator that the macro renders as a block */
  renderAs: "block";

  /**
   * Render function
   *
   * @param params - The macro's parameters ; optional fields may be absent or equal to `undefined`
   * @param openParamsEditor - Request the opening of an UI to edit the macro's parameters (e.g. a modal)
   *
   * @returns The AST to render the macro as
   */
  render(params: GetConcreteMacroParametersType<Parameters>): MacroBlock[];
}

/**
 * Description of an inline macro
 *
 * @since 0.23
 * @beta
 */
interface InlineMacro<Parameters extends Record<string, MacroParameterType>> {
  /** Macro informations */
  infos: MacroInfos<Parameters>;

  /** Indicator that the macro renders as an inline content */
  renderAs: "inline";

  /**
   * Render function
   *
   * @param params - The macro's parameters ; optional fields may be absent or equal to `undefined`
   * @param openParamsEditor - Request the opening of an UI to edit the macro's parameters (e.g. a modal)
   *
   * @returns The AST to render the macro as
   */
  render(
    params: GetConcreteMacroParametersType<Parameters>,
  ): MacroInlineContent[];
}

/**
 * Description of a macro
 *
 * @since 0.23
 * @beta
 */
type Macro<Parameters extends Record<string, MacroParameterType>> =
  | BlockMacro<Parameters>
  | InlineMacro<Parameters>;

/**
 * Description of a macro with an unknown parameters shape
 *
 * @since 0.23
 * @beta
 */
type MacroWithUnknownParamsType = Macro<Record<string, MacroParameterType>>;

/**
 * Description of an instanciable macro with an unknown parameters shape
 *
 * @since 0.23
 * @beta
 */

type MacroClassWithUnknownParamsType = new (
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  ...args: any[]
) => MacroWithUnknownParamsType;

/**
 * Description of a macro parameter's type
 *
 * The final TypeScript type will be derived through `GetConcreteMacroParameterType`
 *
 * @since 0.23
 * @beta
 */
type MacroParameterType = (
  | { type: "boolean" }
  // We use 'float' instead of 'number' here to make it more explicit to developers
  | { type: "float" }
  | { type: "string" }
) & {
  // Make the parameter optional
  optional?: true;
};

/**
 * Internal utility type to remove values that may be assigned `undefined` from a record
 *
 * @since 0.23
 * @beta
 */
type FilterUndefined<T> = {
  [K in keyof T as undefined extends T[K] ? never : K]: T[K];
};

/**
 * Utility type to get the concrete TypeScript type from a macro parameter's definition
 *
 * @since 0.23
 * @beta
 */
type GetConcreteMacroParameterType<T extends MacroParameterType> =
  | (T["type"] extends "boolean"
      ? boolean
      : T["type"] extends "float"
        ? number
        : T["type"] extends "string"
          ? string
          : never)
  | (T["optional"] extends true ? undefined : never);

/**
 * Internal utility type making all properties that may be assigned `undefined` optional in a record
 *
 * @since 0.23
 * @beta
 */
type UndefinableToOptional<T> = {
  [K in keyof T as undefined extends T[K] ? K : never]?: Exclude<
    T[K],
    undefined
  >;
} & { [K in keyof T as undefined extends T[K] ? never : K]: T[K] };

/**
 * Utility type to get the concrete TypeScript record type from a macro's parameters definition
 *
 * Parameters defined as optional are both optional in the output record and can be assigned `undefined`
 *
 * @since 0.23
 * @beta
 */
type GetConcreteMacroParametersType<
  T extends Record<string, MacroParameterType>,
> = UndefinableToOptional<{
  [Param in keyof T]: GetConcreteMacroParameterType<T[Param]>;
}>;

/**
 * Generic type for a macro's unshaped parameters
 *
 * @since 0.23
 * @beta
 */
type UnknownMacroParamsType = Record<string, boolean | number | string>;

/**
 * Cast a macro class to an unknown-shape macro class
 *
 * @param macro - The macro class to cast
 *
 * @returns - The same macro class, without its parameters shape
 *
 * @since 0.23
 * @beta
 */
export function eraseParamsTypeForMacroClass<
  Params extends Record<string, MacroParameterType>,
>(
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  macro: new (...args: any[]) => Macro<Params>,
): MacroClassWithUnknownParamsType {
  return macro as MacroClassWithUnknownParamsType;
}

export type {
  BlockMacro,
  FilterUndefined,
  GetConcreteMacroParameterType,
  GetConcreteMacroParametersType,
  InlineMacro,
  Macro,
  MacroClassWithUnknownParamsType,
  MacroInfos,
  MacroParameterType,
  MacroWithUnknownParamsType,
  UndefinableToOptional,
  UnknownMacroParamsType,
};

export type {
  MacroAlignment,
  MacroBlock,
  MacroBlockStyles,
  MacroImage,
  MacroInlineContent,
  MacroLink,
  MacroLinkTarget,
  MacroListItem,
  MacroTableCell,
  MacroTableColumn,
  MacroText,
  MacroTextStyles,
} from "./ast";
