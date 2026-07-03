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
  useComponentsContext,
  useDictionary,
} from "@blocknote/react";
import type { ImageEditionOverrideFn } from "./images/CustomImageToolbar";
import type { ContextForMacros } from "../blocknote/utils";
import type {
  BlockTypeSelectItem,
  FormattingToolbarProps,
} from "@blocknote/react";
import type { JSX } from "react";

type CustomFormattingToolbarProps = {
  formattingToolbarProps: FormattingToolbarProps;
  additionalBlockTypes: BlockTypeSelectItem[];
  imageEditionOverrideFn?: ImageEditionOverrideFn;
  ctxForMacros: ContextForMacros | false;
};

export const CustomFormattingToolbar: React.FC<
  CustomFormattingToolbarProps
> = ({
  formattingToolbarProps,
  additionalBlockTypes,
  imageEditionOverrideFn,
  ctxForMacros,
}) => {
  const Components = useComponentsContext()!;
  const dict = useDictionary();

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
          ctxForMacros,
        )
      )}
    </Components.FormattingToolbar.Root>
  );
};

const getDefaultFormattingToolbarItems = (
  blockTypeSelectItems: BlockTypeSelectItem[] | undefined,
  ctxForMacros: ContextForMacros | false,
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
    <TextAlignButton textAlignment={"left"} key={"textAlignLeftButton"} />,
    <TextAlignButton textAlignment={"center"} key={"textAlignCenterButton"} />,
    <TextAlignButton textAlignment={"right"} key={"textAlignRightButton"} />,
    <ColorStyleButton key={"colorStyleButton"} />,
    <NestBlockButton key={"nestBlockButton"} />,
    <UnnestBlockButton key={"unnestBlockButton"} />,
    // This button has the exact same appearance as the default creation link button
    // But brings a custom popover to support XWiki references
    <CustomCreateLinkButton key={"createLinkButton"} />,
    <AddCommentButton key={"addCommentButton"} />,
    <AddTiptapCommentButton key={"addTiptapCommentButton"} />,
  ].concat(
    ctxForMacros
      ? [
          <CustomInsertMacroButton
            key={"insertMacroButton"}
            openEditor={ctxForMacros.openInsertionEditor}
          />,
        ]
      : [],
  );

export type { CustomFormattingToolbarProps };
