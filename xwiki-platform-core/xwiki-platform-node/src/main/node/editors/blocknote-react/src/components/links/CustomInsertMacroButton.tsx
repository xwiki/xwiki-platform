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
import { MACRO_NAME_PREFIX } from "../../blocknote/utils";
import { useEditor } from "../../hooks";
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import { useComponentsContext } from "@blocknote/react";
import { assertUnreachable } from "@xwiki/platform-fn-utils";
import { useCallback } from "react";
import { useTranslation } from "react-i18next";
import type { BlockType, InlineContentType } from "../../blocknote";
import type {
  ContextForMacros,
  InlineMacroInvocation,
  MacroBlockInvocation,
} from "../../blocknote/utils";

export type CustomInsertMacroButtonProps = {
  openEditor: ContextForMacros["openInsertionEditor"];
};

export const CustomInsertMacroButton: React.FC<
  CustomInsertMacroButtonProps
> = ({ openEditor }) => {
  const editor = useEditor();
  const Components = useComponentsContext()!;
  const { t } = useTranslation();

  // TODO: check if we need to update in realtime when the selection change?
  const selected = editor.getSelection();

  const openPrefilledEditor = useCallback(() => {
    openEditor(
      { id: null, params: null, body: selected?.blocks ?? null },
      insertMacro,
    );
  }, [openEditor, selected]);

  const insertMacro = useCallback(
    // eslint-disable-next-line max-statements
    (call: MacroBlockInvocation | InlineMacroInvocation) => {
      switch (call.kind) {
        case "block": {
          const block: BlockType = {
            // @ts-expect-error: AST is dynamically typed
            type: MACRO_NAME_PREFIX + call.id,
            id: Math.random().toString(),
            // eslint-disable-next-line @typescript-eslint/no-explicit-any
            props: call.params as any,
          };

          if (call.body) {
            block.content = call.body;
          }

          insertOrUpdateBlockForSlashMenu(editor, block);
          break;
        }

        case "inline": {
          const inlineContent: InlineContentType = {
            // @ts-expect-error: AST is dynamically typed
            type: `${MACRO_NAME_PREFIX}${call.id}`,
            props: call.params,
          };

          if (call.body) {
            // NOTE: AST is dynamically typed
            (
              inlineContent as unknown as { content: InlineContentType }
            ).content = call.body;
          }

          editor.insertInlineContent([], { updateSelection: true });
          break;
        }

        default:
          assertUnreachable(call);
      }
    },
    [editor],
  );

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
