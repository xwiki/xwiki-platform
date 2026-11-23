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
} from "@blocknote/react";
import type { BlockType } from "../blocknote";
import type {
  BlockTypeSelectItem,
  FormattingToolbarProps,
} from "@blocknote/react";
import type { JSX, ReactElement, ReactNode } from "react";

type CustomFormattingToolbarProps = {
  formattingToolbarProps: FormattingToolbarProps;
  additionalBlockTypes: BlockTypeSelectItem[];
  /** The toolbar items contributed by the editing features. */
  featureItems: ReactElement[];
  /** The toolbars contributed by the editing features, replacing this one for the blocks of a given type. */
  blockToolbars: Record<string, (block: BlockType) => ReactNode>;
};

/**
 * @param block - the selected block
 * @param blockToolbars - the toolbars replacing the formatting toolbar for the blocks of a given type
 * @returns the toolbar replacing the formatting toolbar for the given block, if any
 */
function renderBlockToolbar(
  block: BlockType,
  blockToolbars: Record<string, (block: BlockType) => ReactNode>,
): ReactNode | undefined {
  return blockToolbars[block.type]?.(block);
}

export const CustomFormattingToolbar: React.FC<
  CustomFormattingToolbarProps
> = ({
  formattingToolbarProps,
  additionalBlockTypes,
  featureItems,
  blockToolbars,
}) => {
  const Components = useComponentsContext()!;

  const editor = useEditor();

  // TODO: check if there is a need to update the selection in realtime?
  const currentBlock = editor.getTextCursorPosition().block;

  const combinedBlockTypeSelectItems = (
    formattingToolbarProps.blockTypeSelectItems ?? blockTypeSelectItems(editor)
  ).concat(additionalBlockTypes);

  return (
    <Components.FormattingToolbar.Root
      className={"bn-toolbar bn-formatting-toolbar"}
    >
      {/* Display a custom toolbar for specific element types (e.g. images), and the "normal" toolbar for the others. */}
      {renderBlockToolbar(currentBlock, blockToolbars) ??
        getDefaultFormattingToolbarItems(
          combinedBlockTypeSelectItems,
          featureItems,
        )}
    </Components.FormattingToolbar.Root>
  );
};

const getDefaultFormattingToolbarItems = (
  blockTypeSelectItems: BlockTypeSelectItem[] | undefined,
  featureItems: ReactElement[],
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
    ...featureItems,
    <AddCommentButton key={"addCommentButton"} />,
    <AddTiptapCommentButton key={"addTiptapCommentButton"} />,
  ];

export type { CustomFormattingToolbarProps };
