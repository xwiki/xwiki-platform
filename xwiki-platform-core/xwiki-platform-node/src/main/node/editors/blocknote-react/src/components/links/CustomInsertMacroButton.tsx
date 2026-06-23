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
import { MACRO_NAME_PREFIX, buildMacroRawContent } from "../../blocknote/utils";
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
      insertMacro,
    );
  }, [openEditor, selectedBlock]);

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

          switch (call.body.type) {
            case "inlineContents":
              block.content = call.body.content;
              break;

            case "raw":
              block.content = [buildMacroRawContent(call.body.content)];
              break;

            case "none":
              break;

            default:
              assertUnreachable(call.body);
          }

          if (selectedBlock) {
            editor.replaceBlocks([selectedBlock], [block]);
          } else {
            insertOrUpdateBlockForSlashMenu(editor, block);
          }

          break;
        }

        case "inline": {
          const inlineContent: InlineContentType = {
            // @ts-expect-error: AST is dynamically typed
            type: `${MACRO_NAME_PREFIX}${call.id}`,
            props: call.params,
          };

          switch (call.body.type) {
            case "inlineContent":
              // NOTE: AST is dynamically typed
              (
                inlineContent as unknown as { content: InlineContentType }
              ).content = call.body.content;
              break;

            case "raw":
              // NOTE: AST is dynamically typed
              (
                inlineContent as unknown as { content: InlineContentType }
              ).content = buildMacroRawContent(call.body.content);
              break;

            case "none":
              break;

            default:
              assertUnreachable(call.body);
          }

          editor.insertInlineContent([inlineContent], {
            updateSelection: true,
          });

          break;
        }

        default:
          assertUnreachable(call);
      }
    },
    [editor],
  );

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
