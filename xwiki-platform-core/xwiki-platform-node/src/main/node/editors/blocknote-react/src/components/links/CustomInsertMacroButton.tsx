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
import { insertMacroInvocation } from "../../blocknote/utils";
import { useEditor } from "../../hooks";
import { useComponentsContext } from "@blocknote/react";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import type { ContextForMacros } from "../../blocknote/utils";

export type CustomInsertMacroButtonProps = {
  openEditor: NonNullable<ContextForMacros["openInsertionEditor"]>;
};

export const CustomInsertMacroButton: React.FC<
  CustomInsertMacroButtonProps
> = ({ openEditor }) => {
  const editor = useEditor();
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  // TODO: check if we need to update in realtime when the selection change?
  const selected = editor.getSelection();

  const selectedBlock =
    selected && selected.blocks.length === 1 ? selected.blocks[0] : null;

  const openPrefilledEditor = useCallback(() => {
    openEditor(
      {
        kind: "block",
        id: null,
        params: null,
        body:
          selectedBlock?.content && Array.isArray(selectedBlock.content)
            ? {
                type: "inlineContents",
                content: selectedBlock.content,
              }
            : { type: "none" },
      },
      // Materialize the selected macro as a server-rendered xwikiMacroBlock / xwikiInlineMacro, replacing the
      // currently selected block (for a block macro).
      (invocation) => insertMacroInvocation(editor, invocation, selectedBlock),
    );
  }, [openEditor, selectedBlock, editor]);

  if (!selectedBlock) {
    return <></>;
  }

  return (
    <Components.FormattingToolbar.Button
      className={"bn-button"}
      data-test="insertMacro"
      label={t("blocknote.toolbar.buttons.insertMacro")}
      mainTooltip={t("blocknote.toolbar.buttons.insertMacro")}
      icon={"M" /* TODO: find a proper icon? */}
      onClick={openPrefilledEditor}
    />
  );
};
