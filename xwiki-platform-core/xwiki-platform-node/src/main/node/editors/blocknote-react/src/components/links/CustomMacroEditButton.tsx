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
import {
  MACRO_NAME_PREFIX,
  invocationToMacroCall,
  macroCallToInvocation,
} from "../../blocknote/utils";
import { useEditor } from "../../hooks";
import { useComponentsContext } from "@blocknote/react";
import { useMemo } from "react";
import { useTranslation } from "react-i18next";
import { RiPencilFill } from "react-icons/ri";
import type {
  ContextForMacros,
  InlineMacroInvocation,
  MacroBlockInvocation,
  MacroCall,
  MacroCallParams,
} from "../../blocknote/utils";

export type CustomMacroEditButtonProps = {
  ctxForMacros: ContextForMacros;
};

/** The invocation to edit for a selected macro block, and the writer that applies the wizard's result back to it. */
type MacroEdit = {
  invocation: MacroBlockInvocation | InlineMacroInvocation;
  writeBack: (updated: MacroBlockInvocation | InlineMacroInvocation) => void;
};

/**
 * Resolve the given selected block into a macro edit: a server-rendered `xwikiMacroBlock` (its call stored in the
 * `call` prop) or a client-rendered `Macro_<id>` block (its parameters stored directly in the props). Returns null for
 * any other block.
 */
function resolveMacroEdit(
  editor: ReturnType<typeof useEditor>,
  // eslint-disable-next-line @typescript-eslint/no-explicit-any
  macroBlock: any,
): MacroEdit | null {
  const type: string = macroBlock.type;
  const props = macroBlock.props;
  if (type === "xwikiMacroBlock") {
    return {
      invocation: macroCallToInvocation(
        JSON.parse(props.call) as MacroCall,
        "block",
      ),
      writeBack: (updated) =>
        editor.updateBlock(macroBlock.id, {
          props: {
            call: JSON.stringify(invocationToMacroCall(updated)),
            output: "[]",
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
          } as any,
        }),
    };
  }
  if (type.startsWith(MACRO_NAME_PREFIX)) {
    return {
      invocation: {
        kind: "block",
        id: type.slice(MACRO_NAME_PREFIX.length),
        params: props as MacroCallParams,
        body: { type: "none" },
      },
      writeBack: (updated) =>
        // eslint-disable-next-line @typescript-eslint/no-explicit-any
        editor.updateBlock(macroBlock.id, { props: updated.params as any }),
    };
  }
  return null;
}

export const CustomMacroEditButton: React.FC<CustomMacroEditButtonProps> = ({
  ctxForMacros,
}) => {
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  const editor = useEditor();

  // A block macro is edited when it is node-selected (clicked) or the text cursor is inside it.
  // getSelection() returns undefined for a node selection, so resolve the block from the text cursor
  // position instead — the same approach the image toolbar uses to detect a selected image block.
  const block = editor.getTextCursorPosition().block;

  const openMacroEditor = useMemo(() => {
    // Hide the edit action when no params editor is available.
    const openParamsEditor = ctxForMacros.openParamsEditor;

    if (!openParamsEditor) {
      return null;
    }

    const edit = resolveMacroEdit(editor, block);
    if (!edit) {
      return null;
    }

    return () => openParamsEditor(edit.invocation, edit.writeBack);
  }, [block, ctxForMacros, editor]);

  return (
    openMacroEditor && (
      <Components.FormattingToolbar.Button
        className={"bn-button"}
        data-test="editMacro"
        label={t("blocknote.linkToolbar.macros.edit.label")}
        mainTooltip={t("blocknote.linkToolbar.macros.edit.tooltip")}
        icon={<RiPencilFill />}
        onClick={openMacroEditor}
      />
    )
  );
};
