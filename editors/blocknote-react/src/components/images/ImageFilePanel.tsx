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
import { ImageSelector } from "./ImageSelector";
import { useEditor } from "../../hooks";
import { Paper } from "@mantine/core";
import { useCallback } from "react";
import type { BlockOfType } from "../../blocknote";
import type { LinkEditionContext } from "../../misc/linkSuggest";

export type ImageFilePanelProps = {
  currentBlock: BlockOfType<"image">;
  linkEditionCtx: LinkEditionContext;
};

export const ImageFilePanel: React.FC<ImageFilePanelProps> = ({
  currentBlock: image,
  linkEditionCtx,
}) => {
  const editor = useEditor();

  const updateImage = useCallback(
    (url: string) => {
      editor.updateBlock({ id: image.id }, { props: { url } });
      editor.focus();
    },
    [editor, image],
  );

  return (
    // By default file panels don't have any styling and just "float", unstyled, above the editor
    // So we put some container to make it stand out from the editor's content
    <Paper shadow="md" p="sm">
      <ImageSelector linkEditionCtx={linkEditionCtx} onSelected={updateImage} />
    </Paper>
  );
};
