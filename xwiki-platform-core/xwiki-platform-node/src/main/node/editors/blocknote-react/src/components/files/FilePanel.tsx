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
import { ImageFilePanel } from "../images/ImageFilePanel";
import type { EditorType } from "../../blocknote";
import type { LinkEditionContext } from "../../misc/linkSuggest";
import type React from "react";

export type FilePanelProps = {
  editor: EditorType;
  blockId: string;
  linkEditionCtx: LinkEditionContext;
};

export const FilePanel: React.FC<FilePanelProps> = ({
  blockId,
  editor,
  linkEditionCtx,
}) => {
  const block = editor.getBlock(blockId);

  if (!block) {
    throw new Error(
      "Assertion failed: provided blockId was not found in editor (file panel)",
    );
  }

  if (block.type === "image") {
    return (
      <ImageFilePanel linkEditionCtx={linkEditionCtx} currentBlock={block} />
    );
  }

  throw new Error(`Assertion failed: unkown block type: ${block.type}`);
};
