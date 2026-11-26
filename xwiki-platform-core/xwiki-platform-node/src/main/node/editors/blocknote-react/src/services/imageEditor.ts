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
import { insertOrUpdateBlockForSlashMenu } from "@blocknote/core";
import type { BlockOfType } from "../blocknote";
import type { EditorInstance } from "../editorInstance";
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
   * {@link ImageEditor}). It is called in the same transaction as the image change, so that the changes it makes to the
   * content (e.g. to the metadata of the image block, see `setBlockMetadata`) are undone together with the image
   * change.
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
 * @param instance - the editor instance
 * @returns the image metadata handler bound in the service container of the given editor instance, if any
 */
function getImageMetadataHandler(
  instance: EditorInstance,
): ImageMetadataHandler | undefined {
  return instance.serviceContainer.isBound(imageMetadataHandlerName)
    ? instance.serviceContainer.get<ImageMetadataHandler>(
        imageMetadataHandlerName,
      )
    : undefined;
}

/**
 * Inserts an image block at the selection of the given editor instance, and lets its image metadata handler know.
 *
 * @param instance - the editor instance where the image is inserted
 * @param props - the properties of the image block to insert
 * @param reference - the resource reference of the image, when known
 */
function insertImage(
  instance: EditorInstance,
  props: Partial<BlockOfType<"image">["props"]>,
  reference?: ResourceReference,
): void {
  instance.editor.transact(() => {
    const block = insertOrUpdateBlockForSlashMenu(instance.editor, {
      type: "image",
      props,
    }) as BlockOfType<"image">;
    getImageMetadataHandler(instance)?.afterUpdate(instance, block, reference);
  });
}

/**
 * Updates an image block of the given editor instance, and lets its image metadata handler know.
 *
 * @param instance - the editor instance holding the image
 * @param block - the image block to update
 * @param props - the image block properties to update
 * @param reference - the resource reference of the new image, when known
 */
function updateImage(
  instance: EditorInstance,
  block: BlockOfType<"image">,
  props: Partial<BlockOfType<"image">["props"]>,
  reference?: ResourceReference,
): void {
  instance.editor.transact(() => {
    const updated = instance.editor.updateBlock(block, {
      props,
    }) as BlockOfType<"image">;
    getImageMetadataHandler(instance)?.afterUpdate(
      instance,
      updated,
      reference,
    );
  });
}

/**
 * @param instance - the editor instance holding the image
 * @param block - the image block to edit
 * @returns the image to edit with the image editor, as provided by the image metadata handler, when bound
 */
function getEditedImage(
  instance: EditorInstance,
  block: BlockOfType<"image">,
): ImageEditionData {
  return (
    getImageMetadataHandler(instance)?.beforeEdit(instance, block) ??
    block.props
  );
}

export {
  getEditedImage,
  imageEditorName,
  imageMetadataHandlerName,
  insertImage,
  updateImage,
};
export type { ImageEditionData, ImageEditor, ImageMetadataHandler };
