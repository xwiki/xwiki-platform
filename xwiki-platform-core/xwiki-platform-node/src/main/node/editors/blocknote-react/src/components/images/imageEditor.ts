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
import { useContext } from "react";
import type { BlockOfType } from "../../blocknote";
import type { EditorInstance } from "../../editorInstance";
import type { ResourceReference } from "@xwiki/platform-rendering-api";

/**
 * The name under which the {@link ImageEditor} is bound in the service container.
 *
 * @since 18.10.0RC1
 * @beta
 */
const imageEditorName = "ImageEditor";

/**
 * The name under which the {@link ImageMetadataHandler} is bound in the service container.
 *
 * @since 18.10.0RC1
 * @beta
 */
const imageMetadataHandlerName = "ImageMetadataHandler";

/**
 * An image, as inserted or edited with the {@link ImageEditor}: the properties of the image block, and the resource
 * reference of the image, when known.
 *
 * @since 18.10.0RC1
 * @beta
 */
type ImageEditionData = Partial<BlockOfType<"image">["props"]> & {
  /**
   * The resource reference of the image, when known; undefined when the image is only known by its URL.
   */
  reference?: ResourceReference;
};

/**
 * The UI (e.g. a modal) letting the user choose an image and set its properties. It replaces the image selector
 * of the editor when it is bound in the service container (see {@link imageEditorName}).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface ImageEditor {
  /**
   * Lets the user choose an image to insert, and set its properties.
   *
   * @param instance - the editor instance where the image is inserted
   * @returns the image to insert, or undefined if the user canceled
   */
  insert(instance: EditorInstance): Promise<ImageEditionData | undefined>;

  /**
   * Lets the user change an image, or its properties.
   *
   * @param instance - the editor instance holding the edited image
   * @param image - the edited image, as provided by the {@link ImageMetadataHandler.beforeEdit}
   * @returns the image properties to update, or undefined if the user canceled
   */
  edit(
    instance: EditorInstance,
    image: ImageEditionData,
  ): Promise<ImageEditionData | undefined>;
}

/**
 * Keeps the data about the images that the image blocks can't hold (e.g. their resource reference), when it is bound
 * in the service container (see {@link imageMetadataHandlerName}).
 *
 * @since 18.10.0RC1
 * @beta
 */
interface ImageMetadataHandler {
  /**
   * Called before the {@link ImageEditor} is opened on an existing image.
   *
   * @param instance - the editor instance holding the image
   * @param block - the image block to edit
   * @returns the image to edit, e.g. the image block properties with the resource reference of the image
   */
  beforeEdit(
    instance: EditorInstance,
    block: BlockOfType<"image">,
  ): ImageEditionData;

  /**
   * Called right after an image is written into the content, either as a new image block or as the new source or
   * properties of an existing image block, whatever the UI used (image suggestions, image selector, upload, or the
   * {@link ImageEditor}).
   *
   * @param instance - the editor instance holding the image
   * @param block - the image block, as written into the content
   * @param reference - the resource reference of the image, when known (e.g. for a suggested or an uploaded image);
   *   undefined when the image is only known by its URL (e.g. the user entered it)
   */
  afterUpdate(
    instance: EditorInstance,
    block: BlockOfType<"image">,
    reference?: ResourceReference,
  ): void;
}

/**
 * The image editor of an editor instance, as used by the editor UI: the image returned by the image editor is
 * written into the content, the image metadata handler is notified, and the failures are logged.
 */
type ImageEditing = {
  /**
   * @param apply - inserts the image chosen by the user
   * @returns a promise resolved once the image editor is closed, whatever the outcome
   */
  insert(apply: (image: ImageEditionData) => void): Promise<void>;

  /**
   * @param block - the image block to edit, which is updated with the image returned by the image editor
   * @returns a promise resolved once the image editor is closed, whatever the outcome
   */
  edit(block: BlockOfType<"image">): Promise<void>;
};

/**
 * @param instance - the editor instance
 * @returns the image metadata handler bound in the service container of the given editor instance, if any
 */
function getImageMetadataHandler(
  instance: EditorInstance | null,
): ImageMetadataHandler | undefined {
  return instance?.serviceContainer.isBound(imageMetadataHandlerName)
    ? instance.serviceContainer.get<ImageMetadataHandler>(
        imageMetadataHandlerName,
      )
    : undefined;
}

/**
 * Lets the image metadata handler of the given editor instance, if any, know that an image was written into the
 * content.
 *
 * @param instance - the editor instance holding the image
 * @param block - the image block, as written into the content
 * @param reference - the resource reference of the image, when known
 */
function afterImageUpdate(
  instance: EditorInstance | null,
  block: BlockOfType<"image">,
  reference?: ResourceReference,
): void {
  if (instance) {
    getImageMetadataHandler(instance)?.afterUpdate(instance, block, reference);
  }
}

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
    edit: (block) =>
      run(
        () =>
          imageEditor.edit(
            instance,
            getImageMetadataHandler(instance)?.beforeEdit(instance, block) ??
              block.props,
          ),
        ({ reference, ...props }) => {
          const updated = instance.editor.updateBlock(block, {
            props,
          }) as BlockOfType<"image">;
          afterImageUpdate(instance, updated, reference);
        },
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

export {
  afterImageUpdate,
  getImageEditing,
  imageEditorName,
  imageMetadataHandlerName,
  useImageEditing,
};
export type {
  ImageEditing,
  ImageEditionData,
  ImageEditor,
  ImageMetadataHandler,
};
