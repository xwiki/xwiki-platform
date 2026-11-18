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
import { EditorInstanceContext } from "../../contexts";
import { imageEditorName } from "../../services/imageEditor";
import { useContext } from "react";
import type { EditorInstance } from "../../editorInstance";
import type { ImageEditionData, ImageEditor } from "../../services/imageEditor";

/**
 * The image editor of an editor instance, as used by the editor UI: the image returned by the image editor is applied,
 * and the failures are logged. The caller decides where the image is written (e.g. as an image block).
 */
type ImageEditing = {
  /**
   * @param apply - inserts the image chosen by the user
   * @returns a promise resolved once the image editor is closed, whatever the outcome
   */
  insert(apply: (image: ImageEditionData) => void): Promise<void>;

  /**
   * @param image - the image to edit
   * @param apply - updates the edited image with the one returned by the image editor
   * @returns a promise resolved once the image editor is closed, whatever the outcome
   */
  edit(
    image: ImageEditionData,
    apply: (image: ImageEditionData) => void,
  ): Promise<void>;
};

/**
 * @param instance - the editor instance
 * @returns the image editing of the given editor instance, or undefined if no image editor is bound in its service
 *   container
 */
function getImageEditing(
  instance: EditorInstance | null,
): ImageEditing | undefined {
  if (!instance?.serviceContainer.isBound(imageEditorName)) {
    return undefined;
  }
  const imageEditor =
    instance.serviceContainer.get<ImageEditor>(imageEditorName);
  return {
    insert: (apply) =>
      run(
        () => imageEditor.insert(instance),
        apply,
        "Failed to insert the image.",
      ),
    edit: (image, apply) =>
      run(
        () => imageEditor.edit(instance, image),
        apply,
        "Failed to edit the image.",
      ),
  };
}

async function run(
  action: () => Promise<ImageEditionData | undefined>,
  apply: (image: ImageEditionData) => void,
  failureMessage: string,
): Promise<void> {
  try {
    const image = await action();
    if (image) {
      apply(image);
    }
  } catch (error) {
    console.error(failureMessage, error);
  }
}

/**
 * @returns the image editing of the editor instance the component is part of, or undefined if not available
 */
function useImageEditing(): ImageEditing | undefined {
  return getImageEditing(useContext(EditorInstanceContext));
}

export { getImageEditing, useImageEditing };
export type { ImageEditing };
