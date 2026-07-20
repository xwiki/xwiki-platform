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

import { CustomImageToolbar } from "./images/CustomImageToolbar";
import { CustomCreateLinkButton } from "./links/CustomCreateLinkButton";
import { CustomInsertMacroButton } from "./links/CustomInsertMacroButton";
import { CustomMacroEditButton } from "./links/CustomMacroEditButton";
import { useEditor } from "../hooks";
import {
  AddCommentButton,
  AddTiptapCommentButton,
  BasicTextStyleButton,
  BlockTypeSelect,
  ColorStyleButton,
  FileCaptionButton,
  FileDeleteButton,
  FileDownloadButton,
  FilePreviewButton,
  FileRenameButton,
  FileReplaceButton,
  NestBlockButton,
  TableCellMergeButton,
  TextAlignButton,
  UnnestBlockButton,
  blockTypeSelectItems,
  useBlockNoteEditor,
  useComponentsContext,
  useDictionary,
  useEditorState,
} from "@blocknote/react";
import { useTranslation } from "react-i18next";
import { RiSubscript, RiSuperscript } from "react-icons/ri";
import type { ImageEditionOverrideFn } from "./images/CustomImageToolbar";
import type { ContextForMacros } from "../blocknote/utils";
import type { LinkEditionHandler } from "./links/linkEdition";
import type {
  BlockTypeSelectItem,
  FormattingToolbarProps,
} from "@blocknote/react";
import type { JSX } from "react";

const BooleanStyleButton: React.FC<{
  style: string;
  icon: React.ReactNode;
  label: string;
}> = ({ style, icon, label }) => {
  const Components = useComponentsContext()!;
  const editor = useBlockNoteEditor();
  const state = useEditorState({
    editor,
    selector: ({ editor }) => {
      if (!editor.isEditable) {
        return undefined;
      }
      const hasContent = (
        editor.getSelection()?.blocks || [editor.getTextCursorPosition().block]
      ).some((b) => b.content !== undefined);
      if (!hasContent) {
        return undefined;
      }
      return { active: style in editor.getActiveStyles() };
    },
  });

  if (state === undefined) {
    return null;
  }
  return (
    <Components.FormattingToolbar.Button
      className="bn-button"
      data-test={style}
      onClick={() => {
        editor.focus();
        editor.toggleStyles({ [style]: true } as never);
      }}
      isSelected={state.active}
      label={label}
      mainTooltip={label}
      icon={icon}
    />
  );
};

type CustomFormattingToolbarProps = {
  formattingToolbarProps: FormattingToolbarProps;
  additionalBlockTypes: BlockTypeSelectItem[];
  macros: { ctx: ContextForMacros } | false;
  linkEditionHandler: LinkEditionHandler;
  imageEditionOverrideFn?: ImageEditionOverrideFn;
};

export const CustomFormattingToolbar: React.FC<
  CustomFormattingToolbarProps
> = ({
  formattingToolbarProps,
  additionalBlockTypes,
  imageEditionOverrideFn,
  macros,
  linkEditionHandler,
}) => {
  const Components = useComponentsContext()!;
  const dict = useDictionary();
  const { t } = useTranslation();

  const editor = useEditor();

  // TODO: check if there is a need to update the selection in realtime?
  const currentBlock = editor.getTextCursorPosition().block;

  const combinedBlockTypeSelectItems = (
    formattingToolbarProps.blockTypeSelectItems ?? blockTypeSelectItems(dict)
  ).concat(additionalBlockTypes);

  return (
    <Components.FormattingToolbar.Root
      className={"bn-toolbar bn-formatting-toolbar"}
    >
      {/* Display a custom toolbar for specific element types (e.g. images) */}
      {currentBlock.type === "image" ? (
        <CustomImageToolbar
          currentBlock={currentBlock}
          imageEditionOverrideFn={imageEditionOverrideFn}
        />
      ) : (
        // For others, simply show the "normal", default toolbar
        getDefaultFormattingToolbarItems(
          combinedBlockTypeSelectItems,
          macros,
          linkEditionHandler,
          t,
        )
      )}
    </Components.FormattingToolbar.Root>
  );
};

const getDefaultFormattingToolbarItems = (
  blockTypeSelectItems: BlockTypeSelectItem[] | undefined,
  macros: { ctx: ContextForMacros } | false,
  linkEditorHandler: LinkEditionHandler,
  t: (key: string) => string,
): JSX.Element[] =>
  // NOTE: This should return **exactly** the same items as BlockNote's default toolbar
  // So, when BlockNote updates theirs, we should update ours
  [
    <BlockTypeSelect key={"blockTypeSelect"} items={blockTypeSelectItems} />,
    <TableCellMergeButton key={"tableCellMergeButton"} />,
    <FileCaptionButton key={"fileCaptionButton"} />,
    <FileReplaceButton key={"replaceFileButton"} />,
    <FileRenameButton key={"fileRenameButton"} />,
    <FileDeleteButton key={"fileDeleteButton"} />,
    <FileDownloadButton key={"fileDownloadButton"} />,
    <FilePreviewButton key={"filePreviewButton"} />,
    <BasicTextStyleButton basicTextStyle={"bold"} key={"boldStyleButton"} />,
    <BasicTextStyleButton
      basicTextStyle={"italic"}
      key={"italicStyleButton"}
    />,
    <BasicTextStyleButton
      basicTextStyle={"underline"}
      key={"underlineStyleButton"}
    />,
    <BasicTextStyleButton
      basicTextStyle={"strike"}
      key={"strikeStyleButton"}
    />,
    <BooleanStyleButton
      style="subscript"
      icon={<RiSubscript />}
      label={t("blocknote.toolbar.subscript")}
      key={"subscriptStyleButton"}
    />,
    <BooleanStyleButton
      style="superscript"
      icon={<RiSuperscript />}
      label={t("blocknote.toolbar.superscript")}
      key={"superscriptStyleButton"}
    />,
    <TextAlignButton textAlignment={"left"} key={"textAlignLeftButton"} />,
    <TextAlignButton textAlignment={"center"} key={"textAlignCenterButton"} />,
    <TextAlignButton textAlignment={"right"} key={"textAlignRightButton"} />,
    <ColorStyleButton key={"colorStyleButton"} />,
    <NestBlockButton key={"nestBlockButton"} />,
    <UnnestBlockButton key={"unnestBlockButton"} />,
    // This button has the exact same appearance as the default creation link button
    // But brings a custom popover to support XWiki references
    <CustomCreateLinkButton
      key={"createLinkButton"}
      linkEditionHandler={linkEditorHandler}
    />,
    <AddCommentButton key={"addCommentButton"} />,
    <AddTiptapCommentButton key={"addTiptapCommentButton"} />,
  ].concat(
    macros
      ? [
          <CustomMacroEditButton
            key={"macroEditButton"}
            ctxForMacros={macros.ctx}
          />,
          // Hide the insert action when no insertion editor is available.
          ...(macros.ctx.openInsertionEditor
            ? [
                <CustomInsertMacroButton
                  key={"insertMacroButton"}
                  openEditor={macros.ctx.openInsertionEditor}
                />,
              ]
            : []),
        ]
      : [],
  );

export type { CustomFormattingToolbarProps };
