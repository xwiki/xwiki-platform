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
import { EditorInstanceContext } from "../../contexts";
import { macroEditorName } from "../../services/macroEditor";
import { useContext } from "react";
import type { EditorInstance } from "../../editorInstance";
import type {
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroEditor,
  MacroInsertionEditorPrefillData,
} from "../../services/macroEditor";

/**
 * The macro editor of an editor instance, as used by the editor UI: the macro call returned by the macro editor is
 * applied, and the failures are logged.
 */
type MacroEditing = {
  /**
   * @param prefill - what to prefill the macro editor with
   * @param apply - inserts the macro call chosen by the user
   */
  insert(
    prefill: MacroInsertionEditorPrefillData,
    apply: (invocation: MacroBlockInvocation | InlineMacroInvocation) => void,
  ): void;

  /**
   * @param invocation - the macro call to edit
   * @param apply - replaces the edited macro call with the one returned by the macro editor
   */
  edit(
    invocation: MacroBlockInvocation | InlineMacroInvocation,
    apply: (invocation: MacroBlockInvocation | InlineMacroInvocation) => void,
  ): void;
};

/**
 * @param instance - the editor instance
 * @returns the macro editing of the given editor instance, or undefined if the macros are disabled or if no macro
 *   editor is bound in its service container
 */
function getMacroEditing(
  instance: EditorInstance | null,
): MacroEditing | undefined {
  if (
    !instance ||
    instance.options.features?.macros === false ||
    !instance.serviceContainer.isBound(macroEditorName)
  ) {
    return undefined;
  }
  const macroEditor =
    instance.serviceContainer.get<MacroEditor>(macroEditorName);
  return {
    insert: (prefill, apply) =>
      void run(
        () => macroEditor.insert(instance, prefill),
        apply,
        "Failed to insert the macro.",
      ),
    edit: (invocation, apply) =>
      void run(
        () => macroEditor.edit(instance, invocation),
        apply,
        "Failed to edit the macro.",
      ),
  };
}

async function run(
  action: () => Promise<
    MacroBlockInvocation | InlineMacroInvocation | undefined
  >,
  apply: (invocation: MacroBlockInvocation | InlineMacroInvocation) => void,
  failureMessage: string,
): Promise<void> {
  try {
    const invocation = await action();
    if (invocation) {
      apply(invocation);
    }
  } catch (error) {
    console.error(failureMessage, error);
  }
}

/**
 * @returns the macro editing of the editor instance the component is part of, or undefined if not available
 */
function useMacroEditing(): MacroEditing | undefined {
  return getMacroEditing(useContext(EditorInstanceContext));
}

export { getMacroEditing, useMacroEditing };
export type { MacroEditing };
