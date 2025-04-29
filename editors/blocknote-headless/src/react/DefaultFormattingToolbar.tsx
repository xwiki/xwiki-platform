/*
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

import {
  BasicTextStyleButton,
  BlockTypeSelect,
  ColorStyleButton,
  CreateLinkButton,
  FileCaptionButton,
  FileReplaceButton,
  NestBlockButton,
  TextAlignButton,
  UnnestBlockButton,
} from "@blocknote/react";

export type DefaultFormattingToolbarProps = {
  disableButtons?: {
    blockTypeSelect?: boolean;
    fileCaption?: boolean;
    fileReplace?: boolean;
    basicTextStyle?: boolean;
    textAlign?: boolean;
    colorStyle?: boolean;
    nestBlock?: boolean;
    unnestBlock?: boolean;
    createLink?: boolean;
  };
};

export const DefaultFormattingToolbar: React.FC<
  DefaultFormattingToolbarProps
> = ({ disableButtons }) => (
  <>
    {disableButtons?.blockTypeSelect !== true && (
      <BlockTypeSelect key={"blockTypeSelect"} />
    )}

    {disableButtons?.fileCaption !== true && (
      <FileCaptionButton key={"fileCaptionButton"} />
    )}

    {disableButtons?.fileReplace !== true && (
      <FileReplaceButton key={"replaceFileButton"} />
    )}

    {disableButtons?.basicTextStyle !== true && (
      <BasicTextStyleButton basicTextStyle={"bold"} key={"boldStyleButton"} />
    )}

    {disableButtons?.basicTextStyle !== true && (
      <BasicTextStyleButton
        basicTextStyle={"italic"}
        key={"italicStyleButton"}
      />
    )}

    {disableButtons?.basicTextStyle !== true && (
      <BasicTextStyleButton
        basicTextStyle={"underline"}
        key={"underlineStyleButton"}
      />
    )}

    {disableButtons?.basicTextStyle !== true && (
      <BasicTextStyleButton
        basicTextStyle={"strike"}
        key={"strikeStyleButton"}
      />
    )}

    {disableButtons?.basicTextStyle !== true && (
      <BasicTextStyleButton key={"codeStyleButton"} basicTextStyle={"code"} />
    )}

    {disableButtons?.textAlign !== true && (
      <TextAlignButton textAlignment={"left"} key={"textAlignLeftButton"} />
    )}
    {disableButtons?.textAlign !== true && (
      <TextAlignButton textAlignment={"center"} key={"textAlignCenterButton"} />
    )}
    {disableButtons?.textAlign !== true && (
      <TextAlignButton textAlignment={"right"} key={"textAlignRightButton"} />
    )}

    {disableButtons?.colorStyle !== true && (
      <ColorStyleButton key={"colorStyleButton"} />
    )}

    {disableButtons?.nestBlock !== true && (
      <NestBlockButton key={"nestBlockButton"} />
    )}
    {disableButtons?.unnestBlock !== true && (
      <UnnestBlockButton key={"unnestBlockButton"} />
    )}

    {disableButtons?.createLink !== true && (
      <CreateLinkButton key={"createLinkButton"} />
    )}
  </>
);
