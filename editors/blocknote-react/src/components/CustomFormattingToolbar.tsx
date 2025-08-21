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

import { CustomImageToolbar } from "./images/CustomImageToolbar";
import { CustomCreateLinkButton } from "./links/CustomCreateLinkButton";
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
  useComponentsContext,
} from "@blocknote/react";
import type { LinkEditionContext } from "../misc/linkSuggest";
import type {
  BlockTypeSelectItem,
  FormattingToolbarProps,
} from "@blocknote/react";
import type { JSX } from "react";

export type CustomFormattingToolbarProps = {
  formattingToolbarProps: FormattingToolbarProps;
  linkEditionCtx: LinkEditionContext;
};

export const CustomFormattingToolbar: React.FC<
  CustomFormattingToolbarProps
> = ({ formattingToolbarProps, linkEditionCtx }) => {
  const Components = useComponentsContext()!;

  const editor = useEditor();

  // TODO: check if there is a need to update the selection in realtime?
  const currentBlock = editor.getTextCursorPosition().block;

  return (
    <Components.FormattingToolbar.Root
      className={"bn-toolbar bn-formatting-toolbar"}
    >
      {/* Display a custom toolbar for specific element types (e.g. images) */}
      {currentBlock.type === "image" ? (
        <CustomImageToolbar
          currentBlock={currentBlock}
          linkEditionCtx={linkEditionCtx}
        />
      ) : (
        // For others, simply show the "normal", default toolbar
        getDefaultFormattingToolbarItems(
          formattingToolbarProps.blockTypeSelectItems,
          linkEditionCtx,
        )
      )}
    </Components.FormattingToolbar.Root>
  );
};

const getDefaultFormattingToolbarItems = (
  blockTypeSelectItems: BlockTypeSelectItem[] | undefined,
  linkEditionCtx: LinkEditionContext,
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
    <CustomCreateLinkButton
      key={"createLinkButton"}
      linkEditionCtx={linkEditionCtx}
    />,
    <AddCommentButton key={"addCommentButton"} />,
    <AddTiptapCommentButton key={"addTiptapCommentButton"} />,
  ];
