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
import { ImageSelector } from "./ImageSelector";
import { afterImageUpdate } from "./imageEditor";
import { EditorInstanceContext } from "../../contexts";
import { useEditor } from "../../hooks";
import { Paper } from "@mantine/core";
import { useCallback, useContext } from "react";
import type { BlockOfType } from "../../blocknote";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

export type ImageFilePanelProps = {
  currentBlock: BlockOfType<"image">;
};

export const ImageFilePanel: React.FC<ImageFilePanelProps> = ({
  currentBlock: image,
}) => {
  const editor = useEditor();
  const instance = useContext(EditorInstanceContext);

  const updateImage = useCallback(
    (url: string, reference?: ResourceReference) => {
      const updatedImage = editor.updateBlock(
        { id: image.id },
        { props: { url } },
      ) as BlockOfType<"image">;
      afterImageUpdate(instance, updatedImage, reference);
      editor.focus();
    },
    [editor, instance, image],
  );

  return (
    // By default file panels don't have any styling and just "float", unstyled, above the editor
    // So we put some container to make it stand out from the editor's content
    <Paper shadow="md" p="sm">
      <ImageSelector
        currentSelection={image.props.url}
        onSelected={updateImage}
      />
    </Paper>
  );
};
