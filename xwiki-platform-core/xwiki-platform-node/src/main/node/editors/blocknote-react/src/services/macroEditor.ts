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
import type { InlineContentType } from "../blocknote";
import type { EditorInstance } from "../editorInstance";
import type {
  InlineMacroInvocation as UniAstInlineMacroInvocation,
  MacroBlockInvocation as UniAstMacroBlockInvocation,
} from "@xwiki/platform-uniast-api";

/**
 * A macro call's parameter map: parameter name to primitive value.
 *
 * @since 18.6.0RC1
 * @beta
 */
type MacroCallParams = Record<string, boolean | number | string>;

/**
 * Information to fill the insertion modal UI with
 *
 * @since 18.5.0RC1
 * @beta
 */
type MacroInsertionEditorPrefillData = {
  /** Type of the macro to insert */
  kind: "block" | "inline";

  /** ID of the macro to insert */
  id: string | null;

  /** Parameters of the macro */
  params: MacroCallParams | null;

  /** Body of the macro */
  body: MacroBlockInvocation["body"] | InlineMacroInvocation["body"] | null;
};

/**
 * Information about a macro block invocation.
 *
 * Identical to `MacroBlockInvocation` from `@xwiki/platform-uniast-api`, except for `body` which is a BlockNote AST
 * piece here.
 *
 * @since 18.5.0RC1
 * @beta
 */
type MacroBlockInvocation = Omit<UniAstMacroBlockInvocation, "body"> & {
  // NOTE: 'InlineContentType[]' should become 'BlockType[]' once BlockNote supports nesting
  // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
  body:
    | { type: "inlineContents"; content: InlineContentType[] }
    | { type: "raw"; content: string }
    | { type: "none" };
};

/**
 * Information about an inline macro invocation.
 *
 * Identical to `InlineMacroInvocation` from `@xwiki/platform-uniast-api`, except for `body` which is a BlockNote AST
 * piece here.
 *
 * @since 18.5.0RC1
 * @beta
 */
type InlineMacroInvocation = Omit<UniAstInlineMacroInvocation, "body"> & {
  // NOTE: 'InlineContentType' should become 'InlineContentType[]' once BlockNote supports nesting
  // Tracking issue: https://github.com/TypeCellOS/BlockNote/issues/1540
  body:
    | { type: "inlineContent"; content: InlineContentType }
    | { type: "raw"; content: string }
    | { type: "none" };
};

/**
 * The name under which the {@link MacroEditor} is bound in the service container.
 *
 * @since 18.10.0RC1
 * @beta
 */
const macroEditorName = "MacroEditor";

/**
 * The UI (e.g. a modal) letting the user choose the macro to insert and set the parameters of a macro call. The
 * actions to insert and edit macros are available only when it is bound in the service container (see
 * {@link macroEditorName}).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface MacroEditor {
  /**
   * Lets the user choose a macro to insert, and set its parameters.
   *
   * @param instance - the editor instance where the macro is inserted
   * @param prefill - what to prefill the UI with (e.g. the selected content, as the macro content)
   * @returns the macro call to insert, or undefined if the user canceled
   */
  insert(
    instance: EditorInstance,
    prefill: MacroInsertionEditorPrefillData,
  ): Promise<MacroBlockInvocation | InlineMacroInvocation | undefined>;

  /**
   * Lets the user edit the parameters of a macro call. The user may also change the macro, so the returned macro call
   * can call a different macro.
   *
   * @param instance - the editor instance holding the edited macro call
   * @param invocation - the macro call to edit
   * @returns the macro call replacing the edited one, or undefined if the user canceled
   */
  edit(
    instance: EditorInstance,
    invocation: MacroBlockInvocation | InlineMacroInvocation,
  ): Promise<MacroBlockInvocation | InlineMacroInvocation | undefined>;
}

export { macroEditorName };
export type {
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroCallParams,
  MacroEditor,
  MacroInsertionEditorPrefillData,
};
